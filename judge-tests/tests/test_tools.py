import base64
import hashlib
import hmac
import importlib.util
import json
from pathlib import Path
import sys
import threading
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from support import Deadline, percentile, register_and_login, summary


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


load_test = load("load_test", ROOT / "perf/perf-load-test.py")
fixtures = load("fixtures", ROOT / "perf/perf-fixtures.py")


class StatisticsTest(unittest.TestCase):
    def test_asymmetric_distribution_keeps_mean_distinct_from_median(self):
        actual = summary([0, 0, 100])
        self.assertAlmostEqual(100 / 3, actual["mean"])
        self.assertEqual(0, actual["p50"])
        self.assertEqual(98, actual["p99"])

    def test_linear_interpolation_boundaries_and_empty_values(self):
        self.assertEqual(99, percentile([0, 100], 99))
        self.assertEqual(0, percentile([0, 100], 0))
        self.assertEqual(100, percentile([0, 100], 100))
        self.assertIsNone(summary([])["mean"])
        with self.assertRaises(ValueError):
            percentile([1], 101)


class DeadlineTest(unittest.TestCase):
    def test_stuck_submission_stops_and_retains_id(self):
        now = [0.0]
        deadline = Deadline(1, clock=lambda: now[0], sleeper=lambda delay: now.__setitem__(0, now[0] + delay))
        calls = []
        def transport(method, url, token, data=None, timeout=10):
            calls.append((method, timeout))
            self.assertLessEqual(timeout, deadline.remaining())
            if method == "POST":
                return 200, {"code": 0, "data": {"submissionId": 17}}
            now[0] += min(0.2, deadline.remaining())
            return 200, {"code": 0, "data": {"status": 0}}
        start = threading.Event()
        start.set()
        result = load_test.worker("http://test/api", [1], {"userId": "7", "token": "private"},
                                  1, 15, deadline, start, transport=transport, poll_seconds=0.3)
        self.assertTrue(deadline.expired())
        self.assertEqual([17], result["unfinishedIds"])
        self.assertLess(len(calls), 10)
        report = load_test.build_report([result], [], 0, now[0], 1, 1, 15, True)
        self.assertEqual([17], report["unfinishedIds"])
        self.assertEqual(1, report["submitAccepted"])
        self.assertEqual(0, report["judgeDone"])
        self.assertIsNone(report["turnaroundAvgMs"])
        self.assertNotIn("private", json.dumps(report))

    def test_think_time_does_not_outlive_deadline(self):
        now = [0.0]
        deadline = Deadline(0.1, clock=lambda: now[0], sleeper=lambda delay: now.__setitem__(0, now[0] + delay))
        deadline.pause(15)
        self.assertEqual(0.1, now[0])
        with self.assertRaises(TimeoutError):
            deadline.timeout()

    def test_missing_server_timestamp_is_not_reported_as_zero_latency(self):
        self.assertIsNone(load_test.turnaround({"createdAt": "2026-11-16T00:00:00"}))
        self.assertIsNone(load_test.turnaround({"createdAt": "2026-11-16T00:00:00", "judgedAt": None}))
        self.assertEqual(1000, load_test.turnaround({"createdAt": "2026-11-16T00:00:00Z",
                                                   "judgedAt": "2026-11-16T00:00:01Z"}))


class AuthCompatibilityTest(unittest.TestCase):
    def test_registration_void_and_legacy_token_both_require_login(self):
        for registration_data in (None, {"token": "legacy"}):
            calls = []
            def transport(method, url, data):
                calls.append(url)
                return 200, {"code": 0, "data": registration_data if url.endswith("register") else {"token": "login-token", "userId": 9}}
            actual = register_and_login("http://test/api", "test-user", "secret", transport=transport)
            self.assertEqual("login-token", actual["token"])
            self.assertEqual(["http://test/api/auth/register", "http://test/api/auth/login"], calls)

    def test_existing_account_skips_registration(self):
        calls = []
        def transport(method, url, data):
            calls.append(url)
            return 200, {"code": 0, "data": {"token": "valid"}}
        register_and_login("http://test/api", "existing", "secret", register=False, transport=transport)
        self.assertEqual(["http://test/api/auth/login"], calls)

    def test_rate_limit_does_not_trigger_repeated_login_or_registration(self):
        calls = []
        def transport(method, url, data):
            calls.append(url)
            return 200, {"code": 42901, "data": None}
        with self.assertRaises(RuntimeError):
            register_and_login("http://test/api", "new", "secret", transport=transport)
        self.assertEqual(1, len(calls))


class FixtureTest(unittest.TestCase):
    def test_jjwt_compatible_algorithms_claims_and_signature(self):
        for length, algorithm, digest in ((32, "HS256", hashlib.sha256), (48, "HS384", hashlib.sha384), (64, "HS512", hashlib.sha512)):
            secret = "x" * length
            token = fixtures.issue_token(secret, 123, 1, now=100, hours=1)
            header, payload, signature = token.split(".")
            decode = lambda text: base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))
            self.assertEqual(algorithm, json.loads(decode(header))["alg"])
            claims = json.loads(decode(payload))
            self.assertEqual(("123", 1, 100, 3700), (claims["sub"], claims["role"], claims["iat"], claims["exp"]))
            self.assertTrue(claims["jti"])
            expected = hmac.new(secret.encode(), (header + "." + payload).encode(), digest).digest()
            self.assertEqual(expected, decode(signature))

    def test_schema_cannot_target_production_database(self):
        schema = "CREATE DATABASE IF NOT EXISTS codeoj\n charset x;\nUSE codeoj;"
        for unsafe in ("codeoj", "production", "codeoj_perf_x;DROP DATABASE codeoj"):
            with self.assertRaises(ValueError):
                fixtures.schema_for_test(schema, unsafe)
        actual = fixtures.schema_for_test(schema, "codeoj_perf_unit")
        self.assertIn("USE `codeoj_perf_unit`;", actual)
        self.assertNotIn("USE codeoj;", actual)


if __name__ == "__main__":
    unittest.main()
