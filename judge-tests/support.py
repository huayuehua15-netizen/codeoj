"""Standard-library helpers for isolated judge validation tools."""
import json
import math
import statistics
import time
import urllib.error
import urllib.request


class Deadline:
    def __init__(self, seconds, clock=time.monotonic, sleeper=time.sleep):
        self.clock, self.sleeper = clock, sleeper
        self.end = clock() + max(0, seconds)

    def remaining(self):
        return max(0, self.end - self.clock())

    def expired(self):
        return self.remaining() <= 0

    def pause(self, seconds):
        delay = min(max(0, seconds), self.remaining())
        if delay:
            self.sleeper(delay)

    def timeout(self, maximum=10):
        remaining = self.remaining()
        if remaining <= 0:
            raise TimeoutError("test deadline reached")
        return min(maximum, remaining)


def http(method, url, token=None, data=None, timeout=10):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    body = json.dumps(data, ensure_ascii=False).encode("utf-8") if data is not None else None
    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8", "replace")
        try:
            return error.code, json.loads(raw)
        except ValueError:
            return error.code, {"error": "non-JSON HTTP error"}
    except Exception as error:
        return -1, {"error": type(error).__name__}


def response_data(status, response):
    if status == 200 and isinstance(response, dict) and response.get("code") == 0:
        return response.get("data")
    return None


def register_and_login(base, username, password, register=True, transport=http):
    credentials = {"username": username, "password": password}
    if register:
        status, response = transport("POST", base + "/auth/register", data=credentials)
        if status != 200 or not isinstance(response, dict) or response.get("code") != 0:
            raise RuntimeError("registration failed; use an existing test account or wait for the IP quota")
    # Registration returns R<Void>. Always log in, even on older versions returning a token.
    status, response = transport("POST", base + "/auth/login", data=credentials)
    data = response_data(status, response)
    if not isinstance(data, dict) or not data.get("token"):
        raise RuntimeError("login failed; check credentials and the normal IP rate limit")
    return data


def percentile(values, percent):
    if not 0 <= percent <= 100:
        raise ValueError("percentile must be between 0 and 100")
    if not values:
        return None
    ordered = sorted(values)
    rank = (len(ordered) - 1) * percent / 100
    low, high = math.floor(rank), math.ceil(rank)
    return ordered[low] + (ordered[high] - ordered[low]) * (rank - low)


def summary(values):
    return {"count": len(values), "mean": statistics.mean(values) if values else None,
            "p50": percentile(values, 50), "p99": percentile(values, 99),
            "max": max(values) if values else None}
