#!/usr/bin/env python3
"""Create deterministic OK-output problems with a preissued isolated test admin token."""
import argparse
import json
import os
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from support import http, response_data


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default=os.environ.get("BASE_URL", "http://localhost/api"))
    parser.add_argument("--problems", type=int, default=100)
    parser.add_argument("--cases", type=int, default=2)
    parser.add_argument("--output", default="perf-problems.json")
    parser.add_argument("--isolated-test-environment", action="store_true", required=True)
    args = parser.parse_args()
    if args.problems < 1 or args.cases < 1:
        parser.error("problems/cases must be positive")
    token = os.environ.get("PERF_ADMIN_TOKEN")
    if not token:
        parser.error("set PERF_ADMIN_TOKEN from the isolated fixture; this tool does not register/login accounts")
    ids = []
    for index in range(1, args.problems + 1):
        body = {"title": f"压测题目 {index}", "description": "独立测试库专用：输出 OK。",
                "samples": [{"input": "0", "output": "OK"}], "difficulty": 1,
                "timeLimitMs": 1000, "memoryLimitMb": 256,
                "languages": ["c", "cpp", "java", "python3", "js"], "enabled": True}
        status, response = http("POST", args.base.rstrip("/") + "/problems", token, body)
        data = response_data(status, response)
        problem_id = data.get("id") if isinstance(data, dict) else data
        if not isinstance(problem_id, int):
            raise RuntimeError(f"problem creation failed at {index}: HTTP {status}")
        cases = [{"input": str(number), "expectedOutput": "OK", "score": 10, "isSample": number == 1}
                 for number in range(1, args.cases + 1)]
        status, response = http("POST", args.base.rstrip("/") + f"/problems/{problem_id}/testcases", token,
                                {"cases": cases})
        if status != 200 or not isinstance(response, dict) or response.get("code") != 0:
            raise RuntimeError(f"test-case import failed for problem {problem_id}: HTTP {status}")
        ids.append(problem_id)
    Path(args.output).write_text(json.dumps({"problemIds": ids}), encoding="utf-8")
    print(f"Created {len(ids)} problems x {args.cases} cases; IDs saved to {args.output}")


if __name__ == "__main__":
    main()
