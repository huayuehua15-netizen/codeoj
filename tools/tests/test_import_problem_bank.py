import copy
import pathlib
import sys
import unittest

sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[1]))
from import_problem_bank import ApiError, all_problems, import_catalog, new_form
from problem_bank import PROBLEMS


class FakeApi:
    def __init__(self):
        self.problems={}; self.cases={}; self.calls=[]; self.fail_import=False

    def seed(self,p):
        pid=len(self.problems)+1
        self.problems[pid]={'id':pid,**copy.deepcopy(p)}; self.cases[pid]=[]
        return pid

    def __call__(self,method,path,data=None):
        self.calls.append((method,path,copy.deepcopy(data)))
        parts=path.split('?')[0].strip('/').split('/')
        if method=='GET' and len(parts)==1:
            page=int(path.split('page=')[1].split('&')[0]); rows=list(self.problems.values())
            # 模拟服务器将请求页大小钳制为 2，包含下架题。
            return {'total':len(rows),'records':copy.deepcopy(rows[(page-1)*2:page*2])}
        if method=='POST' and len(parts)==1: return self.seed(data)
        pid=int(parts[1])
        if len(parts)==2:
            if method=='GET': return copy.deepcopy(self.problems[pid])
            if method=='PUT': self.problems[pid].update(copy.deepcopy(data)); return None
        if len(parts)==3:
            if method=='GET': return copy.deepcopy(self.cases[pid])
            if method=='POST':
                if self.fail_import: raise ApiError('测试用例导入失败')
                for case in data['cases']:
                    self.cases[pid].append({'id':len(self.cases[pid])+1,**copy.deepcopy(case)})
                return len(data['cases'])
        if method=='DELETE':
            cid=int(parts[3]); self.cases[pid]=[c for c in self.cases[pid] if c['id']!=cid]; return None
        raise AssertionError((method,path))


class ImportBankTest(unittest.TestCase):
    def setUp(self): self.api=FakeApi(); self.problem=copy.deepcopy(PROBLEMS[0])

    def test_hidden_and_later_pages_are_included_in_duplicate_detection(self):
        for i in range(5): self.api.seed({'title':f'test{i}','enabled':False})
        self.assertEqual(5,len(all_problems(self.api)))

    def test_new_problem_is_hidden_until_cases_are_imported(self):
        report=import_catalog(self.api,[self.problem])
        self.assertEqual('created',report[0]['action'])
        pid=report[0]['id']; self.assertTrue(self.api.problems[pid]['enabled'])
        self.assertEqual(len(self.problem['cases']),len(self.api.cases[pid]))
        self.assertFalse(next(data for method,path,data in self.api.calls if method=='POST' and path=='/problems')['enabled'])
        methods=[method+' '+path for method,path,data in self.api.calls]
        self.assertLess(methods.index(f'POST /problems/{pid}/testcases'),methods.index(f'PUT /problems/{pid}'))

    def test_import_failure_keeps_draft_and_rerun_resumes_without_duplicates(self):
        state={}; self.api.fail_import=True
        with self.assertRaises(ApiError): import_catalog(self.api,[self.problem],state=state)
        self.assertFalse(self.api.problems[1]['enabled'])
        self.api.fail_import=False
        import_catalog(self.api,[self.problem],state=state)
        count=len(self.api.cases[1]); import_catalog(self.api,[self.problem],state=state)
        self.assertTrue(self.api.problems[1]['enabled'])
        self.assertEqual(1,len(self.api.problems)); self.assertEqual(count,len(self.api.cases[1]))

    def test_existing_problem_is_not_changed_by_default(self):
        form=new_form(self.problem); form['enabled']=False
        self.api.seed(form); before=copy.deepcopy(self.api.problems)
        report=import_catalog(self.api,[self.problem])
        self.assertEqual('skipped',report[0]['action']); self.assertEqual(before,self.api.problems)
        self.assertFalse(any(method!='GET' for method,path,data in self.api.calls))

    def test_different_statement_is_preserved_even_when_enriching(self):
        form=new_form(self.problem); form['description']='共同作者的不同题面'
        self.api.seed(form); before=copy.deepcopy(self.api.problems)
        report=import_catalog(self.api,[self.problem],enrich=True,repair=True)
        self.assertEqual('different-statement-skipped',report[0]['action'])
        self.assertEqual(before,self.api.problems); self.assertEqual([],self.api.cases[1])

    def test_known_bad_binary_case_and_sample_are_repaired_without_removing_custom_case(self):
        p=copy.deepcopy(next(p for p in PROBLEMS if p['title']=='二分查找'))
        form=new_form(p); form['samples'][0]['output']='3'; form['enabled']=True
        self.api.seed(form)
        self.api.cases[1]=[{'id':1,'input':'5 3\n1 2 3 4 5','expectedOutput':'3'},
                           {'id':2,'input':'2 9\n7 9','expectedOutput':'1'}]
        import_catalog(self.api,[p],repair=True)
        self.assertEqual('2',self.api.problems[1]['samples'][0]['output'])
        self.assertTrue(any(c['input']=='2 9\n7 9' for c in self.api.cases[1]))
        self.assertTrue(any(c['expectedOutput']=='2' for c in self.api.cases[1]))
        self.assertFalse(any(c['id']==1 for c in self.api.cases[1]))

    def test_conflicting_unknown_expected_answer_is_not_overwritten(self):
        self.api.seed(new_form(self.problem))
        a,b=self.problem['cases'][0]; self.api.cases[1]=[{'id':1,'input':a,'expectedOutput':'unknown'}]
        with self.assertRaises(ApiError): import_catalog(self.api,[self.problem],enrich=True)
        self.assertEqual('unknown',self.api.cases[1][0]['expectedOutput'])
        self.assertFalse(self.api.problems[1]['enabled'])


if __name__=='__main__': unittest.main()
