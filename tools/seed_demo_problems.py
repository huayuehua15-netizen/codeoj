#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
演示题目种子脚本：通过管理端 API 创建一套编程练习题（题面 + 样例 + 测试用例）。
仅使用 Python 标准库。

用法：
  BASE_URL=http://localhost/api ADMIN_USER=admin ADMIN_PASS=xxx python3 tools/seed_demo_problems.py
"""
import json
import os
import sys
import urllib.error
import urllib.request

BASE = os.environ.get("BASE_URL", "http://localhost:8080/api").rstrip("/")
ADMIN_USER = os.environ.get("ADMIN_USER", "admin")
ADMIN_PASS = os.environ.get("ADMIN_PASS", "admin123")

# (标题, 难度, 时间ms, 内存MB, 题面, 样例, 测试用例)
PROBLEMS = [
    {
        "title": "两数之和", "difficulty": 1, "time_limit_ms": 1000, "memory_limit_mb": 256,
        "description": "## 题目描述\n\n输入两个整数 a 和 b，输出它们的和。\n\n## 输入格式\n\n一行两个整数，以空格分隔。\n\n## 输出格式\n\n输出一个整数，即 a + b。\n\n## 数据范围\n\n0 ≤ a, b ≤ 10^9",
        "samples": [{"input": "1 2", "output": "3"}, {"input": "10 20", "output": "30"}],
        "cases": [("1 2", "3"), ("10 20", "30"), ("0 0", "0"), ("1000000000 1000000000", "2000000000")],
    },
    {
        "title": "阶乘", "difficulty": 1, "time_limit_ms": 1000, "memory_limit_mb": 256,
        "description": "## 题目描述\n\n输入非负整数 n，输出 n 的阶乘 n!。\n\n## 输入格式\n\n一行一个整数 n。\n\n## 输出格式\n\n输出 n! 的值。\n\n## 数据范围\n\n0 ≤ n ≤ 20（结果可用 64 位整数表示）",
        "samples": [{"input": "5", "output": "120"}, {"input": "0", "output": "1"}],
        "cases": [("5", "120"), ("0", "1"), ("10", "3628800"), ("20", "2432902008176640000")],
    },
    {
        "title": "最大公约数", "difficulty": 1, "time_limit_ms": 1000, "memory_limit_mb": 128,
        "description": "## 题目描述\n\n输入两个正整数 a 和 b，输出它们的最大公约数（GCD）。\n\n## 输入格式\n\n一行两个正整数，以空格分隔。\n\n## 输出格式\n\n输出一个整数，即 a 与 b 的最大公约数。\n\n## 数据范围\n\n1 ≤ a, b ≤ 10^9",
        "samples": [{"input": "12 18", "output": "6"}, {"input": "100 75", "output": "25"}],
        "cases": [("12 18", "6"), ("100 75", "25"), ("7 13", "1"), ("1000000000 1000000000", "1000000000")],
    },
    {
        "title": "斐波那契数列", "difficulty": 2, "time_limit_ms": 1000, "memory_limit_mb": 128,
        "description": "## 题目描述\n\n斐波那契数列定义为 F(1)=1，F(2)=1，F(n)=F(n-1)+F(n-2)（n≥3）。给定 n，输出 F(n)。\n\n## 输入格式\n\n一行一个整数 n。\n\n## 输出格式\n\n输出 F(n)。\n\n## 数据范围\n\n1 ≤ n ≤ 90（结果可用 64 位整数表示）",
        "samples": [{"input": "1", "output": "1"}, {"input": "10", "output": "55"}],
        "cases": [("1", "1"), ("2", "1"), ("10", "55"), ("50", "12586269025")],
    },
    {
        "title": "回文串判断", "difficulty": 1, "time_limit_ms": 1000, "memory_limit_mb": 128,
        "description": "## 题目描述\n\n输入一个由小写字母组成的字符串 s，判断它是否为回文串（正着读和倒着读相同）。\n\n## 输入格式\n\n一行一个字符串 s。\n\n## 输出格式\n\n如果是回文串输出 `YES`，否则输出 `NO`。\n\n## 数据范围\n\n1 ≤ |s| ≤ 1000",
        "samples": [{"input": "abba", "output": "YES"}, {"input": "abc", "output": "NO"}],
        "cases": [("abba", "YES"), ("abc", "NO"), ("a", "YES"), ("racecar", "YES")],
    },
    {
        "title": "素数判断", "difficulty": 1, "time_limit_ms": 1000, "memory_limit_mb": 128,
        "description": "## 题目描述\n\n输入一个整数 n，判断它是否为素数（只能被 1 和自身整除的大于 1 的整数）。\n\n## 输入格式\n\n一行一个整数 n。\n\n## 输出格式\n\n如果是素数输出 `YES`，否则输出 `NO`。\n\n## 数据范围\n\n1 ≤ n ≤ 10^7",
        "samples": [{"input": "7", "output": "YES"}, {"input": "9", "output": "NO"}],
        "cases": [("7", "YES"), ("2", "YES"), ("9", "NO"), ("1", "NO"), ("9973", "YES")],
    },
    {
        "title": "数组求和", "difficulty": 1, "time_limit_ms": 1000, "memory_limit_mb": 128,
        "description": "## 题目描述\n\n给定一个整数数组，求所有元素之和。\n\n## 输入格式\n\n第一行一个整数 n，表示数组长度；第二行 n 个整数，以空格分隔。\n\n## 输出格式\n\n输出一个整数，即数组元素之和。\n\n## 数据范围\n\n1 ≤ n ≤ 10^5，|元素| ≤ 10^4",
        "samples": [{"input": "3\n1 2 3", "output": "6"}, {"input": "5\n-1 0 1 2 3", "output": "5"}],
        "cases": [("3\n1 2 3", "6"), ("5\n-1 0 1 2 3", "5"), ("1\n1000000000", "1000000000"), ("4\n-5 -5 -5 -5", "-20")],
    },
    {
        "title": "字符串反转", "difficulty": 1, "time_limit_ms": 1000, "memory_limit_mb": 128,
        "description": "## 题目描述\n\n输入一个字符串 s，输出将它反转后的字符串。\n\n## 输入格式\n\n一行一个字符串 s（不含空格）。\n\n## 输出格式\n\n输出反转后的字符串。\n\n## 数据范围\n\n1 ≤ |s| ≤ 1000",
        "samples": [{"input": "hello", "output": "olleh"}, {"input": "abcde", "output": "edcba"}],
        "cases": [("hello", "olleh"), ("abcde", "edcba"), ("a", "a"), ("12345", "54321")],
    },
    {
        "title": "闰年判断", "difficulty": 1, "time_limit_ms": 1000, "memory_limit_mb": 128,
        "description": "## 题目描述\n\n给定年份 y，判断是否为闰年：能被 400 整除，或能被 4 整除但不能被 100 整除的年份为闰年。\n\n## 输入格式\n\n一行一个整数 y。\n\n## 输出格式\n\n如果是闰年输出 `YES`，否则输出 `NO`。\n\n## 数据范围\n\n1 ≤ y ≤ 9999",
        "samples": [{"input": "2000", "output": "YES"}, {"input": "1900", "output": "NO"}],
        "cases": [("2000", "YES"), ("1900", "NO"), ("2024", "YES"), ("2023", "NO")],
    },
    {
        "title": "数字反转", "difficulty": 1, "time_limit_ms": 1000, "memory_limit_mb": 128,
        "description": "## 题目描述\n\n输入一个非负整数 n，输出它的十进制表示反转后的数值（反转后前导零省略）。\n\n## 输入格式\n\n一行一个非负整数 n。\n\n## 输出格式\n\n输出反转后的整数。\n\n## 数据范围\n\n0 ≤ n ≤ 10^9",
        "samples": [{"input": "123", "output": "321"}, {"input": "1200", "output": "21"}],
        "cases": [("123", "321"), ("1200", "21"), ("7", "7"), ("100000", "1"), ("0", "0")],
    },
    {
        "title": "最大子段和", "difficulty": 2, "time_limit_ms": 2000, "memory_limit_mb": 256,
        "description": "## 题目描述\n\n给定长度为 n 的整数数组，求连续子段的最大和（子段至少包含一个元素）。\n\n## 输入格式\n\n第一行一个整数 n；第二行 n 个整数，以空格分隔。\n\n## 输出格式\n\n输出一个整数，即最大连续子段和。\n\n## 数据范围\n\n1 ≤ n ≤ 10^5，|元素| ≤ 10^4",
        "samples": [{"input": "5\n1 2 -3 4 5", "output": "9"}, {"input": "3\n-1 -2 -3", "output": "-1"}],
        "cases": [("5\n1 2 -3 4 5", "9"), ("3\n-1 -2 -3", "-1"), ("4\n-2 1 -3 4", "4"), ("1\n5", "5")],
    },
    {
        "title": "二分查找", "difficulty": 2, "time_limit_ms": 2000, "memory_limit_mb": 256,
        "description": "## 题目描述\n\n给定一个升序整数数组和一个目标值 target，查找 target 在数组中的下标（从 0 开始）。若不存在，输出 -1。\n\n## 输入格式\n\n第一行两个整数 n 和 target；第二行 n 个严格升序的整数。\n\n## 输出格式\n\n输出一个整数，即下标或 -1。\n\n## 数据范围\n\n1 ≤ n ≤ 10^5",
        "samples": [{"input": "5 3\n1 2 3 4 5", "output": "3"}, {"input": "5 6\n1 2 3 4 5", "output": "-1"}],
        "cases": [("5 3\n1 2 3 4 5", "3"), ("5 6\n1 2 3 4 5", "-1"), ("1 10\n10", "0")],
    },
]


def http(method, path, token=None, data=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    body = json.dumps(data, ensure_ascii=False).encode("utf-8") if data is not None else None
    req = urllib.request.Request(BASE + path, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8", "replace"))


def main():
    admin = http("POST", "/auth/login", data={"username": ADMIN_USER, "password": ADMIN_PASS})
    token = (admin.get("data") or {}).get("token")
    if not token:
        print("管理员登录失败，请用 ADMIN_USER/ADMIN_PASS 指定账号：", admin)
        sys.exit(2)

    existing = {p["title"] for p in http("GET", "/problems?page=1&size=200", token)["data"]["records"]}
    created = 0
    for p in PROBLEMS:
        if p["title"] in existing:
            print(f"跳过（已存在）: {p['title']}")
            continue
        body = {
            "title": p["title"], "description": p["description"], "samples": p["samples"],
            "difficulty": p["difficulty"], "timeLimitMs": p["time_limit_ms"],
            "memoryLimitMb": p["memory_limit_mb"],
            "languages": ["c", "cpp", "java", "python3", "js"], "enabled": True,
        }
        resp = http("POST", "/problems", token, body)
        data = (resp or {}).get("data")
        pid = data.get("id") if isinstance(data, dict) else data
        if not pid:
            print(f"建题失败: {p['title']} -> {resp}")
            continue
        cases = [{"input": c[0], "expectedOutput": c[1], "score": 10, "isSample": False}
                 for c in p["cases"]]
        resp = http("POST", f"/problems/{pid}/testcases", token, {"cases": cases})
        if resp.get("code") != 0:
            print(f"用例导入失败: {p['title']} -> {resp}")
            continue
        created += 1
        print(f"已创建: {p['title']}（id={pid}，{len(cases)} 组用例）")
    print(f"完成，新增 {created} 道题")


if __name__ == "__main__":
    main()
