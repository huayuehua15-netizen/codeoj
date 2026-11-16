#!/usr/bin/env python3
"""Bounded isolated load test using preissued tokens; no registration or login traffic."""
import argparse
import csv
import json
import os
from pathlib import Path
import random
import subprocess
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from support import Deadline, http, response_data, summary

C_CODE = '#include <stdio.h>\nint main(){puts("OK");return 0;}\n'


def turnaround(data):
    try:
        if not isinstance(data.get("createdAt"), str) or not isinstance(data.get("judgedAt"), str):
            return None
        created = datetime.fromisoformat(data["createdAt"].replace("Z", "+00:00"))
        judged = datetime.fromisoformat(data["judgedAt"].replace("Z", "+00:00"))
        return max(0, (judged - created).total_seconds() * 1000)
    except (KeyError, TypeError, ValueError):
        return None


def worker(base, problem_ids, credential, submits, think, deadline, start,
           transport=http, poll_seconds=3):
    start.wait(timeout=deadline.remaining())
    samples, pending = [], {}
    for index in range(submits):
        if deadline.expired():
            break
        before = time.monotonic()
        try:
            request_timeout = deadline.timeout()
        except TimeoutError:
            break
        status, response = transport("POST", base + "/submissions", credential["token"],
                                     {"problemId": random.choice(problem_ids), "language": "c", "code": C_CODE},
                                     timeout=request_timeout)
        data = response_data(status, response)
        sid = data.get("submissionId") if isinstance(data, dict) else None
        sample = {"userId": int(credential["userId"]), "index": index,
                  "submittedAt": datetime.now(timezone.utc).isoformat(),
                  "submitLatencyMs": (time.monotonic() - before) * 1000,
                  "httpStatus": status, "businessCode": response.get("code") if isinstance(response, dict) else None,
                  "submissionId": sid, "status": None}
        samples.append(sample)
        if sid:
            pending[sid] = sample
        if index + 1 < submits:
            deadline.pause(think)
    # Exact ID lookup avoids hiding outstanding tasks behind a paginated list.
    while pending and not deadline.expired():
        for sid in list(pending):
            if deadline.expired():
                break
            try:
                request_timeout = deadline.timeout()
            except TimeoutError:
                break
            status, response = transport("GET", base + f"/submissions/{sid}", credential["token"],
                                         timeout=request_timeout)
            data = response_data(status, response)
            if isinstance(data, dict) and isinstance(data.get("status"), int) and data["status"] >= 2:
                sample = pending.pop(sid)
                sample.update({"status": data["status"], "turnaroundMs": turnaround(data),
                               "timeUsedMs": data.get("timeUsedMs"), "memoryUsedKb": data.get("memoryUsedKb"),
                               "observedDoneMonotonic": time.monotonic()})
        if pending:
            deadline.pause(poll_seconds)
    return {"samples": samples, "unfinishedIds": list(pending),
            "notAttempted": submits - len(samples)}


def build_report(results, queue_samples, started, ended, users, submits, think, timed_out):
    samples = [sample for result in results for sample in result["samples"]]
    finished = [s for s in samples if s["status"] is not None]
    latency = summary([s["submitLatencyMs"] for s in samples])
    turns = summary([s["turnaroundMs"] for s in finished if s.get("turnaroundMs") is not None])
    errors = [s for s in samples if s["submissionId"] is None]
    unfinished = [sid for r in results for sid in r["unfinishedIds"]]
    counts = {}
    for sample in finished:
        key = str(sample["status"])
        counts[key] = counts.get(key, 0) + 1
    elapsed = max(0, ended - started)
    return {"schemaVersion": 2, "users": users, "submitsPerUser": submits, "thinkSeconds": think,
            "durationSeconds": elapsed, "deadlineReached": timed_out,
            "submissions": len(samples), "submitAccepted": len(samples) - len(errors),
            "notAttempted": sum(r["notAttempted"] for r in results),
            "submitAvgMs": latency["mean"], "submitP50Ms": latency["p50"],
            "submitP99Ms": latency["p99"], "submitMaxMs": latency["max"],
            "errorRatePct": len(errors) / len(samples) * 100 if samples else None,
            "judgeDone": len(finished), "unfinishedIds": unfinished,
            "turnaroundSampleCount": turns["count"], "turnaroundAvgMs": turns["mean"],
            "turnaroundP50Ms": turns["p50"], "turnaroundP99Ms": turns["p99"],
            "throughputPerSec": len(finished) / elapsed if elapsed else None,
            "throughputDefinition": "observed terminal submissions / full test wall time (includes think and polling)",
            "acRatePct": counts.get("2", 0) / len(finished) * 100 if finished else None,
            "runWallTimeMs": summary([s["timeUsedMs"] for s in finished if s.get("timeUsedMs") is not None]),
            "queueBacklogMax": max((s["backlog"] for s in queue_samples), default=None),
            "statusCounts": counts, "samples": samples, "queueSamples": queue_samples,
            "notes": ["HTTP acceptance error rate is separate from judge non-AC rate.",
                      "Percentiles use linear interpolation at rank (n-1)*p."]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default=os.environ.get("BASE_URL", "http://localhost/api"))
    parser.add_argument("--users", type=int, default=100)
    parser.add_argument("--submits", type=int, default=6)
    parser.add_argument("--think", type=float, default=15)
    parser.add_argument("--tokens-csv", default="perf-tokens.csv")
    parser.add_argument("--problems-file", default="perf-problems.json")
    parser.add_argument("--output", default="perf-report.json")
    parser.add_argument("--max-duration", type=float, default=3600)
    parser.add_argument("--isolated-test-environment", action="store_true", required=True)
    args = parser.parse_args()
    if min(args.users, args.submits, args.max_duration) <= 0 or args.think < 0:
        parser.error("users/submits/max-duration must be positive and think nonnegative")
    with open(args.tokens_csv, encoding="utf-8", newline="") as stream:
        credentials = list(csv.DictReader(stream))[:args.users]
    if len(credentials) != args.users or any(not c.get("token") or not c.get("userId", "").isdigit() for c in credentials):
        parser.error("provide enough preissued userId/token credentials; no login fallback")
    if len({c["userId"] for c in credentials}) != len(credentials):
        parser.error("each virtual user needs a distinct account")
    with open(args.problems_file, encoding="utf-8") as stream:
        problem_ids = json.load(stream)["problemIds"]
    if not problem_ids:
        parser.error("problemIds must not be empty")
    base = args.base.rstrip("/")
    deadline, start = Deadline(args.max_duration), threading.Event()
    queue_samples, results, monitor_stop = [], [], threading.Event()
    admin_token = os.environ.get("PERF_ADMIN_TOKEN")

    def monitor():
        while not deadline.expired() and not monitor_stop.is_set():
            try:
                request_timeout = deadline.timeout()
            except TimeoutError:
                break
            status, response = http("GET", base + "/admin/queue", admin_token, timeout=request_timeout)
            data = response_data(status, response)
            if isinstance(data, dict) and data.get("backlog") is not None:
                queue_samples.append({"monotonic": time.monotonic(), "backlog": int(data["backlog"])})
            monitor_stop.wait(min(5, deadline.remaining()))

    monitor_thread = threading.Thread(target=monitor, daemon=True) if admin_token else None
    started = time.monotonic()
    if monitor_thread:
        monitor_thread.start()
    # Every worker uses the same deadline, including socket timeout and think/poll waits.
    with ThreadPoolExecutor(max_workers=len(credentials)) as executor:
        futures = [executor.submit(worker, base, problem_ids, c, args.submits, args.think, deadline, start)
                   for c in credentials]
        start.set()
        for future in futures:
            results.append(future.result())
    ended = time.monotonic()
    monitor_stop.set()
    report = build_report(results, list(queue_samples), started, ended, args.users,
                          args.submits, args.think, deadline.expired())
    report["generatedAt"] = datetime.now(timezone.utc).isoformat()
    report["baseUrl"] = base
    try:
        report["commit"] = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
        report["workingTreeDirty"] = bool(subprocess.check_output(["git", "status", "--porcelain"], text=True).strip())
    except (OSError, subprocess.CalledProcessError):
        report["commit"] = None
    Path(args.output).write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({k: v for k, v in report.items() if k not in ("samples", "queueSamples")},
                     ensure_ascii=False, indent=2))
    print("Raw samples saved to " + args.output)
    return 1 if (report["unfinishedIds"] or report["notAttempted"]
                 or report["submitAccepted"] != report["submissions"] or report["acRatePct"] != 100) else 0


if __name__ == "__main__":
    sys.exit(main())
