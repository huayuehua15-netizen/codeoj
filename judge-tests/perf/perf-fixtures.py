#!/usr/bin/env python3
"""Generate SQL and short-lived tokens for a separate codeoj_perf_* database. Executes no SQL."""
import argparse
import base64
import csv
import hashlib
import hmac
import json
import os
from pathlib import Path
import re
import time
import uuid


def issue_token(secret, user_id, role, now=None, hours=2):
    key = secret.encode("utf-8")
    if len(key) < 32:
        raise ValueError("test JWT secret must contain at least 32 UTF-8 bytes")
    # Matches JJWT Keys.hmacShaKeyFor + signWith(key) algorithm selection.
    algorithm, digest = ("HS512", hashlib.sha512) if len(key) >= 64 else (
        ("HS384", hashlib.sha384) if len(key) >= 48 else ("HS256", hashlib.sha256))
    now = int(time.time()) if now is None else now
    def encode(value):
        return base64.urlsafe_b64encode(json.dumps(value, separators=(",", ":")).encode()).rstrip(b"=")
    payload = {"sub": str(user_id), "role": role, "jti": str(uuid.uuid4()),
               "iat": now, "exp": now + int(hours * 3600)}
    signed = encode({"alg": algorithm, "typ": "JWT"}) + b"." + encode(payload)
    return (signed + b"." + base64.urlsafe_b64encode(hmac.new(key, signed, digest).digest()).rstrip(b"=")).decode()


def schema_for_test(schema, database):
    if not re.fullmatch(r"codeoj_perf_[a-zA-Z0-9_]+", database):
        raise ValueError("database must have the codeoj_perf_ prefix and a safe suffix")
    if schema.count("CREATE DATABASE IF NOT EXISTS codeoj\n") != 1 or schema.count("USE codeoj;") != 1:
        raise ValueError("schema database declarations changed; review fixture generation")
    return schema.replace("CREATE DATABASE IF NOT EXISTS codeoj\n", f"CREATE DATABASE IF NOT EXISTS `{database}`\n").replace(
        "USE codeoj;", f"USE `{database}`;")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--database", required=True)
    parser.add_argument("--users", type=int, default=600)
    parser.add_argument("--hours", type=float, default=2)
    parser.add_argument("--output-dir", default=".local-fixtures")
    parser.add_argument("--isolated-test-environment", action="store_true", required=True)
    args = parser.parse_args()
    if not 1 <= args.users <= 2000 or not 0 < args.hours <= 12:
        parser.error("users must be 1..2000 and token lifetime 0..12 hours")
    secret = os.environ.get("PERF_JWT_SECRET", "")
    if not secret.startswith("perf-test-") or len(secret.encode()) < 32:
        parser.error("set a separate PERF_JWT_SECRET starting perf-test- (at least 32 bytes); never reuse production keys")
    schema = schema_for_test((Path(__file__).resolve().parents[2] / "sql/schema.sql").read_text(encoding="utf-8"), args.database)
    output = Path(args.output_dir)
    output.mkdir(parents=True, exist_ok=True)
    rows, statements = [], [f"USE `{args.database}`;", "START TRANSACTION;"]
    for index in range(args.users + 1):
        user_id, role = 1000000 + index, 1 if index == 0 else 0
        username = "perf_fixture_admin" if index == 0 else f"perf_fixture_{index:04d}"
        statements.append("INSERT INTO `user` (`id`,`username`,`password`,`role`,`status`) "
                          f"VALUES ({user_id},'{username}','!TOKEN_ONLY_TEST_ACCOUNT',{role},0);")
        rows.append({"userId": user_id, "username": username,
                     "token": issue_token(secret, user_id, role, hours=args.hours)})
    statements.append("COMMIT;")
    (output / "perf-schema.sql").write_text(schema, encoding="utf-8")
    (output / "perf-fixtures.sql").write_text("\n".join(statements) + "\n", encoding="utf-8")
    with (output / "perf-tokens.csv").open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["userId", "username", "token"])
        writer.writeheader()
        writer.writerows(rows[1:])
    (output / "perf-admin.json").write_text(json.dumps(rows[0]), encoding="utf-8")
    for path in output.iterdir():
        if path.is_file():
            path.chmod(0o600)
    print(f"Generated SQL (not executed) and {args.users} short-lived tokens in {output}.")
    print(f"Import both SQL files into an ISOLATED test MySQL; start test server/judge with MYSQL_DB={args.database}.")
    print("Set test server JWT_SECRET to PERF_JWT_SECRET. Keep these files private; do not archive tokens.")


if __name__ == "__main__":
    main()
