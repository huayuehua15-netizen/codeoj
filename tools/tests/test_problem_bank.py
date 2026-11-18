"""题库答案交叉验证：朴素算法/闭式构造的答案对照独立参考程序。"""
import pathlib
import sys
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from problem_bank import PROBLEMS, REFERENCES, validate_catalog


class ProblemBankTest(unittest.TestCase):
    def test_catalog_covers_three_difficulties_with_unique_titles_and_enough_cases(self):
        report=validate_catalog()
        self.assertEqual(50,report['problems'])
        self.assertEqual({1:20,2:22,3:8},report['difficulty'])
        self.assertGreaterEqual(report['cases'],1000)

    def test_every_expected_answer_matches_the_reference_program(self):
        for problem in PROBLEMS:
            namespace={'__name__':'reference_test'}
            exec(REFERENCES[problem['title']],namespace)
            for index,(text,expected) in enumerate(problem['cases']):
                with self.subTest(title=problem['title'],case=index+1):
                    actual=namespace['solve'](text)
                    self.assertEqual(expected.split(),str(actual).split())

    def test_binary_search_uses_zero_based_indices_and_strictly_sorted_data(self):
        problem=next(p for p in PROBLEMS if p['title']=='二分查找')
        self.assertEqual('2',problem['samples'][0]['output'])
        for text,expected in problem['cases']:
            n,t,*a=map(int,text.split())
            self.assertEqual(n,len(a))
            self.assertTrue(all(a[i]<a[i+1] for i in range(n-1)))
            self.assertEqual(int(expected),a.index(t) if t in a else -1)

    def test_array_sum_cases_respect_statement_constraints(self):
        problem=next(p for p in PROBLEMS if p['title']=='数组求和')
        for text,expected in problem['cases']:
            n,*a=map(int,text.split())
            self.assertEqual(n,len(a))
            self.assertLessEqual(n,100000)
            self.assertTrue(all(abs(x)<=10000 for x in a))
            self.assertEqual(int(expected),sum(a))

    def test_edge_cases_include_overflow_overlap_and_disconnected_graphs(self):
        cases={p['title']:p['cases'] for p in PROBLEMS}
        self.assertTrue(any(int(v)>2**31 for _,v in cases['逆序对计数']))
        self.assertIn(('aaaaa\naaa','3'),cases['字符串模式匹配'])
        self.assertIn(('2 0','-1'),cases['无向带权最短路'])
        self.assertIn(('2 0','-1'),cases['最小生成树'])
        self.assertIn(('3\n-1 -2 -3','-1'),cases['最大子段和'])


if __name__=='__main__': unittest.main()
