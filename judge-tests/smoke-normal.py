#!/usr/bin/env python3
"""Submit five harmless OK-output programs to a prepared all-language test problem."""
import argparse
import json
import os
from pathlib import Path
import sys
from datetime import datetime, timezone
from support import Deadline, http, response_data

PROGRAMS = {
    "c": '#include <stdio.h>\nint main(){puts("OK");return 0;}\n',
    "cpp": '#include <iostream>\nint main(){std::cout << "OK\\n";}\n',
    "java": 'public class Main { public static void main(String[] args) { System.out.println("OK"); } }\n',
    "python3": 'print("OK")\n',
    "js": 'console.log("OK");\n',
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="http://localhost:18080/api")
    parser.add_argument("--problem-id", type=int, required=True, help="prepared problem whose every case expects OK")
    parser.add_argument("--max-duration", type=float, default=180)
    parser.add_argument("--output", default="smoke-normal-result.json")
    args = parser.parse_args()
    token = os.environ.get("TEST_TOKEN")
    if not token:
        parser.error("set TEST_TOKEN to a dedicated student test token")
    deadline, results = Deadline(args.max_duration), []
    for index, (language, code) in enumerate(PROGRAMS.items()):
        # The API keeps a two-second per-user/per-problem idempotency lock.
        if index:
            deadline.pause(2.1)
        if deadline.expired():
            results.append({"language": language, "error": "deadline before submit"})
            continue
        status, response = http("POST", args.base.rstrip("/") + "/submissions", token,
                                {"problemId": args.problem_id, "language": language, "code": code},
                                timeout=deadline.timeout())
        data = response_data(status, response)
        sid = data.get("submissionId") if isinstance(data, dict) else None
        result = {"language": language, "submissionId": sid, "ok": False}
        if not sid:
            result.update({"httpStatus": status, "response": response})
        while sid and not deadline.expired():
            status, response = http("GET", args.base.rstrip("/") + f"/submissions/{sid}", token,
                                    timeout=deadline.timeout())
            data = response_data(status, response)
            if isinstance(data, dict) and (data.get("status") or 0) >= 2:
                cases = data.get("cases") or []
                result.update({"status": data["status"], "timeUsedMs": data.get("timeUsedMs"),
                               "memoryUsedKb": data.get("memoryUsedKb"), "cases": cases,
                               "errorInfo": data.get("errorInfo")})
                result["ok"] = (data["status"] == 2 and data.get("timeUsedMs") is not None
                                and (data.get("memoryUsedKb") or 0) > 0 and bool(cases)
                                and all(case.get("status") == 2 for case in cases))
                break
            deadline.pause(0.5)
        results.append(result)
    report = {"generatedAt": datetime.now(timezone.utc).isoformat(), "results": results,
              "ok": len(results) == 5 and all(result.get("ok") for result in results)}
    Path(args.output).write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if report["ok"] else 1


if __name__ == "__main__":
    sys.exit(main())
