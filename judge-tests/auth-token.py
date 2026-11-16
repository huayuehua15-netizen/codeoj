#!/usr/bin/env python3
"""Print a test login token for shell scripts; never log response bodies."""
import argparse
import os
import sys
from support import register_and_login


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--username", required=True)
    parser.add_argument("--existing", action="store_true")
    args = parser.parse_args()
    password = os.environ.get("TEST_PASSWORD")
    if not password:
        parser.error("set TEST_PASSWORD")
    try:
        data = register_and_login(args.base.rstrip("/"), args.username, password,
                                  register=not args.existing)
        print(data["token"])
        return 0
    except RuntimeError as error:
        print(str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
