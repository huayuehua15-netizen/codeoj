"""通过管理员接口导入教学题库，默认仅新增；无第三方依赖。"""
import argparse
import json
import os
import pathlib
import sys
import urllib.error
import urllib.request
from collections import Counter

from problem_bank import PROBLEMS, validate_catalog


class ApiError(RuntimeError):
    pass


class Api:
    def __init__(self, base):
        self.base=base.rstrip('/')
        if not self.base.endswith('/api'): self.base+='/api'
        self.token=None

    def __call__(self, method, path, data=None):
        headers={'Content-Type':'application/json; charset=utf-8'}
        if self.token: headers['Authorization']='Bearer '+self.token
        body=None if data is None else json.dumps(data,ensure_ascii=False).encode('utf-8')
        request=urllib.request.Request(self.base+path,data=body,headers=headers,method=method)
        try:
            with urllib.request.urlopen(request,timeout=60) as response: payload=json.load(response)
        except urllib.error.HTTPError as error:
            raise ApiError(f'{method} {path}: HTTP {error.code}') from None
        except (urllib.error.URLError,TimeoutError,ValueError):
            raise ApiError(f'{method} {path}: 网络错误或非 JSON 响应') from None
        if payload.get('code')!=0:
            raise ApiError(f'{method} {path}: code={payload.get("code")}, {payload.get("message","")}')
        return payload.get('data')

    def login(self, username, password):
        data=self('POST','/auth/login',{'username':username,'password':password})
        if not data or data.get('role')!=1: raise ApiError('需要管理员账号')
        self.token=data['token']


def all_problems(api):
    records=[]; seen=set(); page=1
    while True:
        result=api('GET',f'/problems?page={page}&size=100')
        batch=result.get('records',[])
        fresh=[p for p in batch if p['id'] not in seen]
        if not fresh and len(records)<result['total']: raise ApiError('分页未前进，停止导入')
        records.extend(fresh); seen.update(p['id'] for p in fresh)
        if len(records)>=result['total']: return records
        page+=1


def normalize(text):
    return text.replace('\r\n','\n').rstrip()


def problem_form(problem):
    return {key:problem.get(key) for key in ('title','description','tags','samples',
        'difficulty','timeLimitMs','memoryLimitMb','languages','enabled')}


def new_form(problem):
    return dict(title=problem['title'],description=problem['description'],tags=problem['tags'],
        samples=problem['samples'],difficulty=problem['difficulty'],
        timeLimitMs=problem['time_limit_ms'],memoryLimitMb=problem['memory_limit_mb'],
        languages=['c','cpp','java','python3','js'],enabled=False)


def import_cases(api,pid,cases):
    # 留足 Nginx 16 MiB 限制余量；数量同样遵守单次最多 200 组的接口约束。
    batch=[]; size=0
    for case in cases:
        length=len(json.dumps(case,ensure_ascii=False).encode('utf-8'))
        if length>12*1024*1024: raise ApiError('单组用例超过安全导入大小')
        if batch and (size+length>4*1024*1024 or len(batch)>=100):
            api('POST',f'/problems/{pid}/testcases',{'cases':batch}); batch=[]; size=0
        batch.append(case); size+=length
    if batch: api('POST',f'/problems/{pid}/testcases',{'cases':batch})


def import_catalog(api, problems=PROBLEMS, enrich=False, repair=False, state=None, save=lambda:None):
    state=state if state is not None else {}
    owned=state.setdefault('created',{})
    existing={}
    for row in all_problems(api):
        if row['title'] in existing: raise ApiError('已有重复标题，需先人工区分: '+row['title'])
        existing[row['title']]=row
    report=[]
    for problem in problems:
        title=problem['title']; row=existing.get(title); created=False
        if row is None:
            pid=api('POST','/problems',new_form(problem))
            if isinstance(pid,dict): pid=pid['id']
            owned[title]=pid; save(); created=True
        else: pid=row['id']
        resume=owned.get(title)==pid
        if not created and not resume and not enrich and not repair:
            report.append(dict(title=title,id=pid,action='skipped',casesAdded=0)); continue
        detail=api('GET',f'/problems/{pid}')
        canonical=normalize(detail.get('description') or '')==normalize(problem['description'])
        if not canonical:
            # 同名但题面不同可能属于共同作者，保留全部内容。
            report.append(dict(title=title,id=pid,action='different-statement-skipped',casesAdded=0)); continue
        current=api('GET',f'/problems/{pid}/testcases')
        form=problem_form(detail); changed=False; bad=[]
        if repair:
            for case in current:
                pair=(normalize(case['input']),normalize(case['expectedOutput']))
                if (title=='二分查找' and pair==('5 3\n1 2 3 4 5','3')) or (
                        title=='数组求和' and pair==('1\n1000000000','1000000000')):
                    bad.append(case['id'])
            if title=='二分查找':
                for sample in form['samples']:
                    if normalize(sample['input'])=='5 3\n1 2 3 4 5' and normalize(sample['output'])=='3':
                        sample['output']='2'; changed=True
        usable=[c for c in current if c['id'] not in bad]
        known={normalize(c['input']):normalize(c['expectedOutput']) for c in usable}
        desired=[]
        if created or resume or enrich:
            desired=problem['cases']
        elif repair:
            desired=[(a,b) for a,b in problem['cases'] if (title=='二分查找' and a=='5 3\n1 2 3 4 5') or (
                title=='数组求和' and a=='1\n10000')]
        additions=[]
        samples={(normalize(s['input']),normalize(s['output'])) for s in problem['samples']}
        for text,expected in desired:
            key=normalize(text); answer=normalize(expected)
            if key in known:
                if known[key]!=answer: raise ApiError('已有同输入异答案的用例，保留现场: '+title)
                continue
            additions.append(dict(input=text,expectedOutput=expected,score=0,isSample=(key,answer) in samples))
            known[key]=answer
        import_cases(api,pid,additions)
        # 先加入正确用例，再删除明确识别的历史错误用例；不清空其他用例。
        for case_id in bad: api('DELETE',f'/problems/{pid}/testcases/{case_id}')
        if (created or resume or enrich) and not form.get('tags'):
            form['tags']=problem['tags']; changed=True
        if resume and not form['enabled']:
            form['enabled']=True; changed=True
        if changed: api('PUT',f'/problems/{pid}',form)
        action='created' if created else ('resumed' if resume else 'updated')
        item=dict(title=title,id=pid,action=action,casesAdded=len(additions),badCasesRemoved=len(bad))
        report.append(item)
        print(f'{action}: {title} (id={pid}, +{len(additions)} cases, -{len(bad)} invalid)',flush=True)
    return report


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--dry-run',action='store_true',help='仅检查本地题库，不访问网络')
    parser.add_argument('--enrich-existing',action='store_true',help='仅给题面完全一致的原有题补空标签与缺失用例')
    parser.add_argument('--repair-known',action='store_true',help='精确修复二分下标与数组求和越界用例，不触碰历史提交')
    parser.add_argument('--state',type=pathlib.Path,default=pathlib.Path('.tmp/problem-bank-import.json'))
    parser.add_argument('--report',type=pathlib.Path)
    args=parser.parse_args()
    print(json.dumps(validate_catalog(),ensure_ascii=False),flush=True)
    if args.dry_run: return 0
    password=os.environ.get('ADMIN_PASS')
    if not password: raise ApiError('请通过 ADMIN_PASS 提供管理员密码（不会写入日志或状态文件）')
    api=Api(os.environ.get('BASE_URL','http://localhost:8080/api'))
    api.login(os.environ.get('ADMIN_USER','admin'),password)
    state=json.loads(args.state.read_text(encoding='utf-8')) if args.state.exists() else {}
    if state.get('base',api.base)!=api.base: raise ApiError('状态文件属于不同环境，请指定另一 --state 路径')
    state['base']=api.base
    def save():
        args.state.parent.mkdir(parents=True,exist_ok=True)
        args.state.write_text(json.dumps(state,ensure_ascii=False,indent=2),encoding='utf-8')
    report=import_catalog(api,enrich=args.enrich_existing,repair=args.repair_known,state=state,save=save)
    save()
    if args.report:
        args.report.parent.mkdir(parents=True,exist_ok=True)
        args.report.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print('完成：'+json.dumps(Counter(row['action'] for row in report),ensure_ascii=False))
    return 0


if __name__=='__main__':
    try: sys.exit(main())
    except ApiError as error: print('导入停止：'+str(error),file=sys.stderr); sys.exit(2)
