#!/usr/bin/env python3
"""Exercise management integrity using newly created fixtures in an isolated test stack."""
import argparse
from datetime import datetime, timedelta, timezone
import json
import os
from pathlib import Path
import sys
import uuid
from support import http


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="http://localhost:18080/api")
    parser.add_argument("--isolated-test-environment", action="store_true", required=True)
    parser.add_argument("--output", default="management-smoke.json")
    args = parser.parse_args()
    token = os.environ.get("TEST_ADMIN_TOKEN")
    if not token:
        parser.error("set TEST_ADMIN_TOKEN for a dedicated test administrator")
    prefix = "smoke-" + uuid.uuid4().hex[:10]
    results, cleanup_errors, problems, courses, contests = [], [], [], [], []

    def request(name, method, path, data=None, expected=0):
        status, body = http(method, args.base.rstrip("/") + path, token, data)
        ok = status == 200 and body.get("code") == expected
        results.append({"check": name, "ok": ok, "httpStatus": status, "businessCode": body.get("code")})
        if not ok:
            raise AssertionError(f"{name}: unexpected HTTP/business result")
        return body.get("data")

    try:
        for index in range(2):
            form = {"title": f"{prefix}-{index}", "description": "Isolated management fixture",
                    "languages": ["python3"], "difficulty": 1, "timeLimitMs": 1000,
                    "memoryLimitMb": 256, "enabled": True, "samples": []}
            problems.append(request("create_problem", "POST", "/problems", form))
        course_form = {"name": prefix, "problemIds": problems}
        course_id = request("create_course_with_ordered_problems", "POST", "/admin/courses", course_form)
        courses.append(course_id)
        request("duplicate_course_selection_rejected", "PUT", f"/admin/courses/{course_id}",
                dict(course_form, problemIds=[problems[0], problems[0]]), expected=40000)
        request("nonexistent_course_problem_rejected", "PUT", f"/admin/courses/{course_id}",
                dict(course_form, problemIds=[9223372036854775807]), expected=40000)
        links = request("course_still_readable", "GET", f"/admin/courses/{course_id}/problems")
        if [row["problemId"] for row in links] != problems:
            raise AssertionError("failed edit must preserve original ordered course problems")
        request("course_reference_prevents_problem_deletion", "DELETE", f"/problems/{problems[0]}", expected=40000)
        now = datetime.now(timezone(timedelta(hours=8)))
        contest = {"title": prefix, "startTime": now.strftime("%Y-%m-%d %H:%M:%S"),
                   "endTime": (now + timedelta(hours=1)).strftime("%Y-%m-%d %H:%M:%S"),
                   "problemIds": [problems[1]]}
        contests.append(request("create_contest", "POST", "/admin/contests", contest))
        request("duplicate_contest_selection_rejected", "POST", "/admin/contests",
                dict(contest, problemIds=[problems[1],problems[1]]), expected=40000)
        request("contest_missing_problem_rejected", "POST", "/admin/contests",
                dict(contest, problemIds=[9223372036854775807]), expected=40000)
        request("contest_reference_prevents_problem_deletion", "DELETE", f"/problems/{problems[1]}", expected=40000)
        path = f"/problems/{problems[0]}/testcases"
        request("missing_case_list_rejected", "POST", path, {}, expected=40000)
        request("null_case_element_rejected", "POST", path, {"cases":[None]}, expected=40000)
        case = {"input":"", "expectedOutput":"", "score":0, "isSample":False}
        request("empty_input_output_are_valid", "POST", path, {"cases":[case]})
        request("orphan_case_import_rejected", "POST", "/problems/9223372036854775807/testcases",
                {"cases":[case]}, expected=40400)
        request("missing_course_returns_not_found", "GET", "/admin/courses/9223372036854775807/problems", expected=40400)
    except Exception as error:
        results.append({"check":"execution", "ok":False, "error":str(error)})
    finally:
        # Delete only resources created by this run; removing links first allows problem deletion.
        for path in ([f"/admin/courses/{i}" for i in courses]
                     + [f"/admin/contests/{i}" for i in contests]
                     + [f"/problems/{i}" for i in problems]):
            status, body = http("DELETE", args.base.rstrip("/") + path, token)
            if status != 200 or body.get("code") != 0:
                cleanup_errors.append(path)
    report = {"ok":bool(results) and all(r["ok"] for r in results) and not cleanup_errors,
              "results":results, "cleanupErrors":cleanup_errors}
    Path(args.output).write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding="utf-8")
    print(json.dumps(report,ensure_ascii=False,indent=2))
    return 0 if report["ok"] else 1


if __name__ == "__main__":
    sys.exit(main())
