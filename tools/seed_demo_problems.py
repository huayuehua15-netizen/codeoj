#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""兼容原入口：导入完整教学题库，参数及环境变量见 docs/PROBLEM-BANK.md。"""
import sys

from import_problem_bank import ApiError, main
from problem_bank import PROBLEMS

if __name__=='__main__':
    try: sys.exit(main())
    except ApiError as error: print('导入停止：'+str(error),file=sys.stderr); sys.exit(2)
