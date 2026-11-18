#!/usr/bin/env python3
"""将维护者参考程序逐题提交真实判题服务；沿用普通学生账号限流。"""
import argparse
import hashlib
import json
import os
import pathlib
import sys
import time

from import_problem_bank import Api, ApiError, all_problems
from problem_bank import PROBLEMS, REFERENCES, validate_catalog


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report',type=pathlib.Path,default=pathlib.Path('.tmp/problem-bank-judge.json'))
    parser.add_argument('--title',action='append',help='只验证指定标题，可重复')
    parser.add_argument('--interval',type=float,default=11,help='提交间隔秒数，不得低于 11 秒')
    args=parser.parse_args()
    if args.interval<11: raise ApiError('请保留每分钟最多 6 次的学生限流')
    if not os.environ.get('VERIFY_PASS'): raise ApiError('需要 VERIFY_USER 和 VERIFY_PASS 学生账号')
    validate_catalog()
    api=Api(os.environ.get('BASE_URL','http://localhost:8080/api'))
    auth=api('POST','/auth/login',{'username':os.environ.get('VERIFY_USER','bank_verifier'),
                                'password':os.environ['VERIFY_PASS']})
    api.token=auth['token']
    live={p['title']:p for p in all_problems(api)}
    selected=[p for p in PROBLEMS if not args.title or p['title'] in args.title]
    if args.title and set(args.title)-{p['title'] for p in selected}: raise ApiError('存在未收录的标题')
    if any(p['title'] not in live for p in selected): raise ApiError('有题目尚未导入或未上架')
    report={'base':api.base,'user':auth.get('username'),'engineRequirement':'real',
            'startedAt':time.strftime('%Y-%m-%d %H:%M:%S'),'results':[]}
    args.report.parent.mkdir(parents=True,exist_ok=True)
    pending={}; next_submit=0; cursor=0; deadline=time.monotonic()+max(300,len(selected)*40)
    def save(): args.report.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    while cursor<len(selected) or pending:
        now=time.monotonic()
        if now>deadline: raise ApiError('验收超时，保留报告以便检查队列')
        if cursor<len(selected) and now>=next_submit:
            p=selected[cursor]; pid=live[p['title']]['id']
            submitted=api('POST','/submissions',{'problemId':pid,'language':'python3','code':REFERENCES[p['title']]})
            item=dict(title=p['title'],problemId=pid,submissionId=submitted['submissionId'],status=submitted['status'],
                referenceSha256=hashlib.sha256(REFERENCES[p['title']].encode()).hexdigest())
            report['results'].append(item); pending[item['submissionId']]=item
            print(f'SUBMIT {cursor+1}/{len(selected)}: {p["title"]} #{item["submissionId"]}',flush=True)
            save(); cursor+=1; next_submit=now+args.interval
        for sid,item in list(pending.items()):
            detail=api('GET',f'/submissions/{sid}')
            if detail['status']>=2:
                item.update({key:detail.get(key) for key in ('status','statusDesc','timeUsedMs','memoryUsedKb','errorInfo','cases')})
                item['passedCases']=sum(c['status']==2 for c in item['cases'] or [])
                item['caseCount']=len(item['cases'] or [])
                item['verified']=item['status']==2 and item['caseCount']>=len(next(p['cases'] for p in selected if p['title']==item['title'])) and item['passedCases']==item['caseCount'] and not item['errorInfo']
                del pending[sid]; save()
                print(f'RESULT: {item["title"]} {item["statusDesc"]} {item["passedCases"]}/{item["caseCount"]}',flush=True)
        time.sleep(1)
    report['finishedAt']=time.strftime('%Y-%m-%d %H:%M:%S')
    report['passed']=sum(row.get('verified',False) for row in report['results'])
    save()
    print(f'FINISHED: {report["passed"]}/{len(selected)} verified.',flush=True)
    return 0 if report['passed']==len(selected) else 1


if __name__=='__main__':
    try: sys.exit(main())
    except ApiError as error: print('验收停止：'+str(error),file=sys.stderr); sys.exit(2)
