"""原创教学题库及确定性测试数据。参考程序仅供维护者验收，不通过题目 API 发布。"""
import copy
import math
import random
import textwrap
from collections import Counter

from basic_problem_bank import PROBLEMS as BASIC

PROBLEMS = copy.deepcopy(BASIC)
REFERENCES = {}
RNG = random.Random(20261117)


def reference(body):
    return ('import sys, math, bisect, heapq\nfrom collections import deque, Counter\n'
            + 'def solve(text):\n' + textwrap.indent(textwrap.dedent(body).strip(), '    ')
            + '\nif __name__ == "__main__":\n    print(solve(sys.stdin.read()))\n')


def add(title, difficulty, tags, description, inputs, outputs, limits, cases, body):
    examples = [(str(a), str(b)) for a, b in cases]
    PROBLEMS.append(dict(title=title, difficulty=difficulty, tags=tags.split('/'),
        time_limit_ms=5000 if difficulty == 3 else 3000, memory_limit_mb=512,
        description=f'## 题目描述\n\n{description}\n\n## 输入格式\n\n{inputs}\n\n## 输出格式\n\n{outputs}\n\n## 数据范围\n\n{limits}',
        samples=[dict(input=a, output=b) for a, b in examples[:2]], cases=examples))
    REFERENCES[title] = reference(body)


def array_input(values):
    return str(len(values)) + '\n' + ' '.join(map(str, values))


def append_case(title, text, output):
    p = next(p for p in PROBLEMS if p['title'] == title)
    pair = (str(text), str(output))
    if all(a != pair[0] for a, _ in p['cases']):
        p['cases'].append(pair)


# 保留基础题题面，修正越界数据；增加上界、零值、负数和确定性随机用例。
basic_tags = ['入门/数学', '数学/循环', '数学/数论', '动态规划/递推', '字符串/双指针',
              '数学/数论', '数组/基础运算', '字符串/模拟', '数学/条件判断', '数学/模拟',
              '数组/动态规划', '数组/二分查找']
basic_bodies = [
    'a,b=map(int,text.split()); return str(a+b)',
    'return str(math.factorial(int(text)))',
    'a,b=map(int,text.split()); return str(math.gcd(a,b))',
    '''n=int(text); a,b=0,1
for _ in range(n): a,b=b,a+b
return str(a)''',
    's=text.strip(); return "YES" if s==s[::-1] else "NO"',
    'n=int(text); return "YES" if n>1 and all(n%d for d in range(2,math.isqrt(n)+1)) else "NO"',
    'a=list(map(int,text.split())); return str(sum(a[1:]))',
    'return text.strip()[::-1]',
    'y=int(text); return "YES" if y%400==0 or y%4==0 and y%100!=0 else "NO"',
    'return str(int(text.strip()[::-1]))',
    '''a=list(map(int,text.split()))[1:]; best=cur=a[0]
for x in a[1:]: cur=max(x,cur+x); best=max(best,cur)
return str(best)''',
    '''a=list(map(int,text.split())); n,t=a[:2]; values=a[2:]; i=bisect.bisect_left(values,t)
return str(i if i<n and values[i]==t else -1)''',
]
for p, tags, body in zip(PROBLEMS, basic_tags, basic_bodies):
    p['tags'] = tags.split('/')
    REFERENCES[p['title']] = reference(body)

for _ in range(18):
    a, b = RNG.randint(0, 10**9), RNG.randint(0, 10**9)
    append_case('两数之和', f'{a} {b}', a+b)
    n = RNG.randint(0, 20)
    append_case('阶乘', n, math.prod(range(1, n+1)))
    a, b = RNG.randint(1, 10**9), RNG.randint(1, 10**9)
    append_case('最大公约数', f'{a} {b}', math.gcd(a,b))
    n = RNG.randint(1,90); seq=[0,1]
    for i in range(2,n+1): seq.append(seq[-1]+seq[-2])
    append_case('斐波那契数列', n, seq[n])
    s = ''.join(RNG.choices('abcde', k=RNG.randint(1,40)))
    if _%2: s=s+s[::-1]
    append_case('回文串判断', s, 'YES' if list(s)==list(reversed(s)) else 'NO')
    n = RNG.randint(1,2000)
    append_case('素数判断', n, 'YES' if n>1 and len([d for d in range(1,n+1) if n%d==0])==2 else 'NO')
    a = [RNG.randint(-10000,10000) for _ in range(RNG.randint(1,50))]
    append_case('数组求和', array_input(a), sum(a))
    append_case('字符串反转', s, ''.join(reversed(s)))
    y = RNG.randint(1,9999)
    append_case('闰年判断', y, 'YES' if y%4==0 and (y%100!=0 or y%400==0) else 'NO')
    n = RNG.randint(0,10**9)
    append_case('数字反转', n, int(str(n)[::-1]))
    a = [RNG.randint(-20,20) for _ in range(RNG.randint(1,20))]
    append_case('最大子段和', array_input(a), max(sum(a[l:r]) for l in range(len(a)) for r in range(l+1,len(a)+1)))
    a = sorted(RNG.sample(range(-100,101),RNG.randint(1,30))); target=RNG.choice(a) if _%2 else 101
    append_case('二分查找', f'{len(a)} {target}\n'+ ' '.join(map(str,a)), a.index(target) if target in a else -1)

for title, text, answer in [('阶乘','20',2432902008176640000),('斐波那契数列','90',2880067194370816120),
    ('数组求和',array_input([10000]*100000),10**9),('最大子段和',array_input([10000]*100000),10**9),
    ('二分查找','100000 99999\n'+' '.join(map(str,range(100000))),99999)]:
    append_case(title,text,answer)

ab=copy.deepcopy(PROBLEMS[0]); ab['title']='A+B Problem'
ab['description']=ab['description'].replace('输出一个整数，即 a + b。','一行一个整数，即 a+b 的值。')
PROBLEMS.insert(0,ab); REFERENCES[ab['title']]=REFERENCES['两数之和']

add('数组最值',1,'数组/遍历','给定非空整数数组，求最小值与最大值。',
    '第一行 n；第二行 n 个整数。','一行输出最小值和最大值，以空格分隔。','1 ≤ n ≤ 100000，|a_i| ≤ 10^9。',
    [('5\n3 -2 7 7 0','-2 7'),('1\n-9','-9 -9')],
    'a=list(map(int,text.split()))[1:]; return f"{min(a)} {max(a)}"')
add('升序排序',1,'数组/排序','将数组按非递减顺序排列，保留重复元素。',
    '第一行 n；第二行 n 个整数。','一行输出排序后的 n 个数。','1 ≤ n ≤ 100000，|a_i| ≤ 10^9。',
    [('5\n3 1 2 1 -1','-1 1 1 2 3'),('1\n0','0')],
    'a=sorted(map(int,text.split()[1:])); return " ".join(map(str,a))')
add('有序数组去重',1,'数组/双指针','给定非递减整数数组，只保留每个值的一次出现。',
    '第一行 n；第二行 n 个非递减整数。','第一行输出不同数的个数，第二行按升序输出这些数。','1 ≤ n ≤ 100000，|a_i| ≤ 10^9。',
    [('6\n1 1 2 2 2 5','3\n1 2 5'),('3\n-1 -1 -1','1\n-1')],
    'a=sorted(set(map(int,text.split()[1:]))); return str(len(a))+"\\n"+" ".join(map(str,a))')
add('字符频次',1,'字符串/计数','统计字符串中 a 到 z 各字母的出现次数。',
    '一行非空小写英文字母字符串。','一行按 a 到 z 顺序输出 26 个计数，以空格分隔。','1 ≤ |s| ≤ 100000。',
    [('abac','2 1 1 '+'0 '*22+'0'),('z','0 '*25+'1')],
    'c=Counter(text.strip()); return " ".join(str(c[chr(97+i)]) for i in range(26))')
add('括号匹配',1,'字符串/栈','字符串仅包含 ()[]{}。判断括号是否正确嵌套并全部匹配。',
    '一行非空括号字符串。','正确输出 YES，否则输出 NO。','1 ≤ |s| ≤ 100000。',
    [('([]{})','YES'),('([)]','NO')],
    '''stack=[]; pairs={')':'(',']':'[','}':'{'}
for c in text.strip():
    if c in '([{': stack.append(c)
    elif not stack or stack.pop()!=pairs[c]: return 'NO'
return 'NO' if stack else 'YES' ''')
add('进制转换',1,'数学/模拟','将非负十进制整数转换为 b 进制，不输出前导零。10 至 15 使用大写 A 至 F。',
    '一行两个整数 n 和 b。','输出转换后的字符串；n 为 0 时输出 0。','0 ≤ n ≤ 10^18，2 ≤ b ≤ 16。',
    [('255 16','FF'),('0 2','0')],
    '''n,b=map(int,text.split()); out=[]
while n: out.append('0123456789ABCDEF'[n%b]); n//=b
return ''.join(out[::-1]) or '0' ''')
add('快速幂取模',1,'数学/快速幂','计算 a 的 b 次方除以 m 的余数。规定 0 的 0 次方为 1。',
    '一行三个整数 a、b、m。','输出 a^b mod m。','0 ≤ a ≤ 10^9，0 ≤ b ≤ 10^18，1 ≤ m ≤ 10^9。',
    [('2 10 1000','24'),('0 0 1','0')],
    'a,b,m=map(int,text.split()); return str(pow(a,b,m))')
add('区间和查询',1,'数组/前缀和','多次查询静态数组中闭区间 [l,r] 的元素之和，下标从 1 开始。',
    '第一行 n、q；第二行 n 个整数；接着 q 行，每行 l、r。','每行输出一个查询答案，可能超过 32 位整数范围。','1 ≤ n,q ≤ 100000，1 ≤ l ≤ r ≤ n，|a_i| ≤ 10^9。',
    [('4 2\n1 2 3 4\n1 4\n2 3','10\n5'),('1 1\n-5\n1 1','-5')],
    '''it=iter(map(int,text.split())); n,q=next(it),next(it); p=[0]
for _ in range(n): p.append(p[-1]+next(it))
return '\\n'.join(str(p[r]-p[l-1]) for l,r in zip(it,it))''')
add('合并有序数组',1,'数组/双指针','将两个非递减数组合并为一个非递减数组，保留重复元素。',
    '第一行 n、m；第二行 n 个数；第三行 m 个数。','一行输出合并后的 n+m 个整数。','1 ≤ n,m ≤ 100000，|元素| ≤ 10^9。',
    [('3 3\n1 3 5\n2 3 4','1 2 3 3 4 5'),('1 1\n-1\n0','-1 0')],
    '''a=list(map(int,text.split())); n,m=a[:2]; x=a[2:2+n]; y=a[2+n:]; i=j=0; out=[]
while i<n and j<m:
    if x[i]<=y[j]: out.append(x[i]); i+=1
    else: out.append(y[j]); j+=1
out.extend(x[i:]); out.extend(y[j:]); return ' '.join(map(str,out))''')
add('二进制位计数',1,'数学/位运算','计算非负整数的二进制表示中 1 的个数。',
    '一行一个整数 n。','输出 1 的个数。','0 ≤ n ≤ 2^63-1。',
    [('13','3'),('0','0')], 'return str(int(text).bit_count())')

add('区间增量',2,'数组/差分','依次将闭区间 [l,r] 的每个元素增加 d，输出全部操作后的数组。下标从 1 开始。',
    '第一行 n、m；第二行 n 个整数；接着 m 行 l、r、d。','一行输出最终数组，使用 64 位整数。','1 ≤ n,m ≤ 100000，1 ≤ l ≤ r ≤ n，|a_i|,|d| ≤ 10^9。',
    [('4 2\n1 2 3 4\n1 3 2\n2 4 -1','3 3 4 3'),('1 1\n0\n1 1 -5','-5')],
    '''it=iter(map(int,text.split())); n,m=next(it),next(it); a=[next(it) for _ in range(n)]; d=[0]*(n+1)
for _ in range(m):
    l,r,x=next(it),next(it),next(it); d[l-1]+=x; d[r]-=x
v=0
for i in range(n): v+=d[i]; a[i]+=v
return ' '.join(map(str,a))''')
add('最长无重复子串',2,'字符串/滑动窗口','求不包含重复字符的最长连续子串长度。',
    '一行小写英文字母字符串。','输出最长长度。','1 ≤ |s| ≤ 100000。',
    [('abcabcbb','3'),('bbbbb','1')],
    '''last={}; left=best=0
for i,c in enumerate(text.strip()):
    left=max(left,last.get(c,-1)+1); last[c]=i; best=max(best,i-left+1)
return str(best)''')
add('和为目标的连续子数组',2,'数组/前缀和/哈希表','求元素和恰好为 k 的非空连续子数组个数。数组可以包含负数。',
    '第一行 n、k；第二行 n 个整数。','输出子数组个数，使用 64 位整数。','1 ≤ n ≤ 100000，|a_i|,|k| ≤ 10^9。',
    [('3 2\n1 1 1','2'),('3 0\n0 0 0','6')],
    '''a=list(map(int,text.split())); n,k=a[:2]; seen={0:1}; s=answer=0
for x in a[2:]: s+=x; answer+=seen.get(s-k,0); seen[s]=seen.get(s,0)+1
return str(answer)''')
add('滑动窗口最大值',2,'数组/单调队列','长度为 k 的窗口从左向右逐步滑过数组，求每个窗口中的最大值。',
    '第一行 n、k；第二行 n 个整数。','一行依次输出 n-k+1 个最大值。','1 ≤ k ≤ n ≤ 100000，|a_i| ≤ 10^9。',
    [('8 3\n1 3 -1 -3 5 3 6 7','3 3 5 5 6 7'),('1 1\n-2','-2')],
    '''a=list(map(int,text.split())); n,k=a[:2]; a=a[2:]; q=deque(); out=[]
for i,x in enumerate(a):
    while q and q[0]<=i-k: q.popleft()
    while q and a[q[-1]]<=x: q.pop()
    q.append(i)
    if i>=k-1: out.append(a[q[0]])
return ' '.join(map(str,out))''')
add('第K小的数',2,'数组/排序/堆','求数组按非递减顺序排列后的第 k 个数，重复元素分别计数。',
    '第一行 n、k；第二行 n 个整数。','输出第 k 小的数。','1 ≤ k ≤ n ≤ 200000，|a_i| ≤ 10^9。',
    [('5 3\n5 1 3 3 2','3'),('1 1\n-7','-7')],
    'a=list(map(int,text.split())); return str(sorted(a[2:])[a[1]-1])')
add('活动安排',2,'贪心/排序','每项活动占用半开时间段 [s,e)。同一时间只能参加一个活动，求最多能参加多少项。前一活动结束时可立即开始下一项。',
    '第一行 n；接着 n 行，每行开始时间 s 和结束时间 e。','输出最多活动数。','1 ≤ n ≤ 100000，0 ≤ s < e ≤ 10^9。',
    [('4\n1 3\n2 4\n3 5\n5 6','3'),('2\n0 10\n0 10','1')],
    '''it=iter(map(int,text.split())); n=next(it); intervals=sorted(zip(it,it),key=lambda x:x[1]); end=-1; answer=0
for s,e in intervals:
    if s>=end: answer+=1; end=e
return str(answer)''')
add('合并区间',2,'排序/区间','将所有有交集的闭区间合并。端点相同也视为有交集，区间之间仅相邻但无交集时不合并。',
    '第一行 n；接着 n 行，每行 l、r。','第一行输出合并后区间数，随后按左端点递增输出每个区间的 l 和 r。','1 ≤ n ≤ 100000，-10^9 ≤ l ≤ r ≤ 10^9。',
    [('3\n1 3\n3 5\n8 9','2\n1 5\n8 9'),('1\n2 2','1\n2 2')],
    '''it=iter(map(int,text.split())); next(it); out=[]
for l,r in sorted(zip(it,it)):
    if out and l<=out[-1][1]: out[-1][1]=max(out[-1][1],r)
    else: out.append([l,r])
return str(len(out))+'\\n'+'\\n'.join(f'{l} {r}' for l,r in out)''')
add('最少硬币',2,'动态规划/完全背包','每种面值的硬币都有无限枚。求凑出金额 amount 的最少枚数；无法凑出则输出 -1。',
    '第一行 k、amount；第二行 k 个不同的正整数面值。','输出最少枚数；amount 为 0 时答案为 0。','1 ≤ k ≤ 20，0 ≤ amount ≤ 10000，1 ≤ 面值 ≤ 10000。',
    [('3 11\n1 2 5','3'),('1 3\n2','-1')],
    '''a=list(map(int,text.split())); k,amount=a[:2]; dp=[0]+[amount+1]*amount
for v in range(1,amount+1):
    for c in a[2:]:
        if c<=v: dp[v]=min(dp[v],dp[v-c]+1)
return str(dp[amount] if dp[amount]<=amount else -1)''')
add('0-1背包',2,'动态规划/背包','每件物品至多选一次，在总重量不超过 W 的条件下，求最大总价值。可以不选任何物品。',
    '第一行 n、W；接着 n 行，每行重量 w 和价值 v。','输出最大总价值。','1 ≤ n ≤ 100，1 ≤ W,w ≤ 10000，0 ≤ v ≤ 10^6。',
    [('3 5\n2 3\n3 4\n4 5','7'),('1 1\n2 9','0')],
    '''it=iter(map(int,text.split())); n,W=next(it),next(it); dp=[0]*(W+1)
for w,v in zip(it,it):
    for cap in range(W,w-1,-1): dp[cap]=max(dp[cap],dp[cap-w]+v)
return str(dp[W])''')
add('最长上升子序列',2,'动态规划/二分查找','求严格递增子序列的最大长度。子序列可以不连续。',
    '第一行 n；第二行 n 个整数。','输出最大长度，相等的元素不能延长严格递增序列。','1 ≤ n ≤ 200000，|a_i| ≤ 10^9。',
    [('8\n10 9 2 5 3 7 101 18','4'),('3\n2 2 2','1')],
    '''tails=[]
for x in map(int,text.split()[1:]):
    i=bisect.bisect_left(tails,x)
    if i==len(tails): tails.append(x)
    else: tails[i]=x
return str(len(tails))''')
add('最长公共子序列',2,'字符串/动态规划','求两个字符串的最长公共子序列长度。子序列保留相对顺序，但不要求连续。',
    '两行，每行一个非空小写英文字母字符串。','输出最长公共子序列长度。','1 ≤ 两个字符串长度 ≤ 1000。',
    [('abcde\nace','3'),('abc\ndef','0')],
    '''a,b=text.split(); dp=[0]*(len(b)+1)
for x in a:
    prev=0
    for j,y in enumerate(b,1):
        old=dp[j]; dp[j]=prev+1 if x==y else max(dp[j],dp[j-1]); prev=old
return str(dp[-1])''')
add('网格最短路',2,'图论/广度优先搜索','从网格左上角到右下角，每步可向上下左右移动一格。0 表示通路，1 表示障碍，求最少步数。起点或终点被阻挡也不可达。',
    '第一行 n、m；接着 n 行，每行 m 个连续的 0 或 1 字符。','输出最少步数，不可达输出 -1。','1 ≤ n,m ≤ 300。',
    [('3 3\n000\n110\n000','4'),('1 1\n1','-1')],
    '''lines=text.split(); n,m=map(int,lines[:2]); grid=lines[2:]
if grid[0][0]=='1' or grid[-1][-1]=='1': return '-1'
q=deque([(0,0,0)]); seen={(0,0)}
while q:
    x,y,d=q.popleft()
    if (x,y)==(n-1,m-1): return str(d)
    for a,b in ((x+1,y),(x-1,y),(x,y+1),(x,y-1)):
        if 0<=a<n and 0<=b<m and grid[a][b]=='0' and (a,b) not in seen: seen.add((a,b)); q.append((a,b,d+1))
return '-1' ''')
add('连通分量统计',2,'图论/并查集','求无向图的连通分量个数，孤立顶点也算一个分量。允许重复边和自环。',
    '第一行 n、m；接着 m 行无向边 u、v，顶点编号 1 到 n。','输出连通分量个数。','1 ≤ n ≤ 100000，0 ≤ m ≤ 200000。',
    [('5 2\n1 2\n3 4','3'),('1 0','1')],
    '''it=iter(map(int,text.split())); n,m=next(it),next(it); parent=list(range(n+1)); answer=n
def find(x):
    while parent[x]!=x: parent[x]=parent[parent[x]]; x=parent[x]
    return x
for u,v in zip(it,it):
    u,v=find(u),find(v)
    if u!=v: parent[u]=v; answer-=1
return str(answer)''')
add('课程依赖是否可完成',2,'图论/拓扑排序','有向边 u→v 表示完成课程 v 前必须先完成 u。判断能否完成全部课程。允许重复依赖和自环。',
    '第一行 n、m；接着 m 行依赖 u、v。课程编号为 1 到 n。','能完成全部课程输出 YES，否则输出 NO。','1 ≤ n ≤ 100000，0 ≤ m ≤ 200000。',
    [('3 2\n1 2\n2 3','YES'),('2 2\n1 2\n2 1','NO')],
    '''it=iter(map(int,text.split())); n,m=next(it),next(it); g=[[] for _ in range(n)]; deg=[0]*n
for u,v in zip(it,it): g[u-1].append(v-1); deg[v-1]+=1
q=deque(i for i in range(n) if not deg[i]); done=0
while q:
    u=q.popleft(); done+=1
    for v in g[u]:
        deg[v]-=1
        if not deg[v]: q.append(v)
return 'YES' if done==n else 'NO' ''')
add('无向带权最短路',2,'图论/最短路/堆','求无向带权图中从顶点 1 到顶点 n 的最短距离。边权非负，允许重复边和自环。',
    '第一行 n、m；接着 m 行 u、v、w。','输出最短距离，不可达输出 -1；n=1 时答案为 0。','1 ≤ n ≤ 100000，0 ≤ m ≤ 200000，0 ≤ w ≤ 10^9。答案使用 64 位整数。',
    [('3 3\n1 2 4\n2 3 5\n1 3 20','9'),('2 0','-1')],
    '''it=iter(map(int,text.split())); n,m=next(it),next(it); g=[[] for _ in range(n)]
for _ in range(m):
    u,v,w=next(it)-1,next(it)-1,next(it); g[u].append((v,w)); g[v].append((u,w))
dist=[math.inf]*n; dist[0]=0; q=[(0,0)]
while q:
    d,u=heapq.heappop(q)
    if d!=dist[u]: continue
    for v,w in g[u]:
        if d+w<dist[v]: dist[v]=d+w; heapq.heappush(q,(d+w,v))
return str(dist[-1]) if dist[-1]!=math.inf else '-1' ''')
add('最小生成树',2,'图论/最小生成树/并查集','在无向带权图中选择边使所有顶点连通且总边权最小。允许负权、重复边和自环。',
    '第一行 n、m；接着 m 行 u、v、w。','输出最小总权值，无法连通则输出 -1；单个顶点答案为 0。','1 ≤ n ≤ 100000，0 ≤ m ≤ 200000，|w| ≤ 10^9。总权值使用 64 位整数。',
    [('3 3\n1 2 4\n2 3 5\n1 3 20','9'),('2 0','-1')],
    '''it=iter(map(int,text.split())); n,m=next(it),next(it); edges=[(next(it),next(it),next(it)) for _ in range(m)]; p=list(range(n+1)); total=used=0
def find(x):
    while p[x]!=x: p[x]=p[p[x]]; x=p[x]
    return x
for u,v,w in sorted(edges,key=lambda e:e[2]):
    u,v=find(u),find(v)
    if u!=v: p[u]=v; total+=w; used+=1
return str(total) if used==n-1 else '-1' ''')
add('并查集操作',2,'数据结构/并查集','维护元素 1 到 n 的集合，初始每个元素单独成组。U a b 合并两个元素所在集合，Q a b 查询是否同组。',
    '第一行 n、q；接着 q 行操作 U a b 或 Q a b，至少有一次查询。','每次查询各输出一行 YES 或 NO。','1 ≤ n ≤ 100000，1 ≤ q ≤ 200000，1 ≤ a,b ≤ n。',
    [('3 4\nQ 1 2\nU 1 2\nQ 1 2\nQ 1 3','NO\nYES\nNO'),('1 1\nQ 1 1','YES')],
    '''lines=text.splitlines(); n,q=map(int,lines[0].split()); p=list(range(n+1)); out=[]
def find(x):
    while p[x]!=x: p[x]=p[p[x]]; x=p[x]
    return x
for line in lines[1:]:
    op,a,b=line.split(); a,b=find(int(a)),find(int(b))
    if op=='U': p[a]=b
    else: out.append('YES' if a==b else 'NO')
return '\\n'.join(out)''')
add('树的直径',2,'图论/树/广度优先搜索','给定无权树，求任意两个顶点间最长简单路径的边数。',
    '第一行 n；接着 n-1 行无向边 u、v。顶点编号为 1 到 n。','输出直径的边数。','1 ≤ n ≤ 100000；输入保证是一棵树。',
    [('4\n1 2\n2 3\n2 4','2'),('1','0')],
    '''it=iter(map(int,text.split())); n=next(it); g=[[] for _ in range(n)]
for u,v in zip(it,it): g[u-1].append(v-1); g[v-1].append(u-1)
def far(start):
    d=[-1]*n; d[start]=0; q=deque([start]); u=start
    while q:
        u=q.popleft()
        for v in g[u]:
            if d[v]<0: d[v]=d[u]+1; q.append(v)
    return u,d[u]
return str(far(far(0)[0])[1])''')
add('质因数分解',2,'数学/数论','将整数分解为质因数幂的乘积，因子按从小到大排列。',
    '一行一个整数 n。','一行输出 p^e 格式的项，以空格分隔；指数为 1 也必须写出。','2 ≤ n ≤ 10^12。',
    [('360','2^3 3^2 5^1'),('13','13^1')],
    '''n=int(text); p=2; out=[]
while p*p<=n:
    e=0
    while n%p==0: n//=p; e+=1
    if e: out.append(f'{p}^{e}')
    p+=1 if p==2 else 2
if n>1: out.append(f'{n}^1')
return ' '.join(out)''')

add('逆序对计数',3,'数组/归并排序/分治','统计满足 i<j 且 a_i>a_j 的下标对个数，相等元素不构成逆序对。',
    '第一行 n；第二行 n 个整数。','输出逆序对数，使用 64 位整数。','1 ≤ n ≤ 200000，|a_i| ≤ 10^9。',
    [('5\n5 4 3 2 1','10'),('3\n2 2 2','0')],
    '''a=list(map(int,text.split()))[1:]; vals={v:i+1 for i,v in enumerate(sorted(set(a)))}; bit=[0]*(len(vals)+1); answer=0
for x in reversed(a):
    i=vals[x]-1
    while i: answer+=bit[i]; i-=i&-i
    i=vals[x]
    while i<len(bit): bit[i]+=1; i+=i&-i
return str(answer)''')
add('最短编辑距离',3,'字符串/动态规划','每次可插入、删除或替换一个字符，求将第一个字符串变为第二个字符串所需的最少操作数。',
    '两行，每行一个非空小写英文字母字符串。','输出最少操作数。','1 ≤ 两个字符串长度 ≤ 1000。',
    [('kitten\nsitting','3'),('a\na','0')],
    '''a,b=text.split(); dp=list(range(len(b)+1))
for i,x in enumerate(a,1):
    row=[i]
    for j,y in enumerate(b,1): row.append(min(dp[j]+1,row[-1]+1,dp[j-1]+(x!=y)))
    dp=row
return str(dp[-1])''')
add('区间动态求和',3,'数据结构/树状数组','支持单点增量和闭区间求和。操作 1 i d 将 a_i 增加 d；操作 2 l r 查询 [l,r] 的元素和。',
    '第一行 n、q；第二行 n 个整数；接着 q 行操作，至少有一次查询。','每次查询各输出一行结果，使用 64 位整数。','1 ≤ n,q ≤ 100000，|a_i|,|d| ≤ 10^9，下标从 1 开始且有效。',
    [('3 3\n1 2 3\n2 1 3\n1 2 5\n2 2 3','6\n10'),('1 1\n-1\n2 1 1','-1')],
    '''it=iter(map(int,text.split())); n,q=next(it),next(it); bit=[0]*(n+1)
def add(i,v):
    while i<=n: bit[i]+=v; i+=i&-i
def prefix(i):
    s=0
    while i: s+=bit[i]; i-=i&-i
    return s
for i in range(1,n+1): add(i,next(it))
out=[]
for _ in range(q):
    op,x,y=next(it),next(it),next(it)
    if op==1: add(x,y)
    else: out.append(prefix(y)-prefix(x-1))
return '\\n'.join(map(str,out))''')
add('区间加与区间最小值',3,'数据结构/线段树','支持闭区间加法与区间最小值查询。操作 1 l r d 将 [l,r] 中每个数增加 d；操作 2 l r 查询区间最小值。',
    '第一行 n、q；第二行 n 个整数；接着 q 行操作，至少有一次查询。','每次查询各输出一行最小值，使用 64 位整数。','1 ≤ n,q ≤ 100000，|a_i|,|d| ≤ 10^9，1 ≤ l ≤ r ≤ n。',
    [('3 3\n1 2 3\n2 1 3\n1 1 2 -5\n2 1 3','1\n-4'),('1 1\n9\n2 1 1','9')],
    '''lines=text.splitlines(); n,q=map(int,lines[0].split()); a=list(map(int,lines[1].split())); tree=[0]*(4*n); lazy=[0]*(4*n)
def build(o,l,r):
    if l==r: tree[o]=a[l]; return
    m=(l+r)//2; build(o*2,l,m); build(o*2+1,m+1,r); tree[o]=min(tree[o*2],tree[o*2+1])
def push(o):
    for c in (o*2,o*2+1): tree[c]+=lazy[o]; lazy[c]+=lazy[o]
    lazy[o]=0
def update(o,l,r,x,y,v):
    if x<=l and r<=y: tree[o]+=v; lazy[o]+=v; return
    push(o); m=(l+r)//2
    if x<=m: update(o*2,l,m,x,y,v)
    if y>m: update(o*2+1,m+1,r,x,y,v)
    tree[o]=min(tree[o*2],tree[o*2+1])
def query(o,l,r,x,y):
    if x<=l and r<=y: return tree[o]
    push(o); m=(l+r)//2; answer=math.inf
    if x<=m: answer=min(answer,query(o*2,l,m,x,y))
    if y>m: answer=min(answer,query(o*2+1,m+1,r,x,y))
    return answer
build(1,0,n-1); out=[]
for line in lines[2:]:
    op,*p=map(int,line.split()); l,r=p[:2]
    if op==1: update(1,0,n-1,l-1,r-1,p[2])
    else: out.append(query(1,0,n-1,l-1,r-1))
return '\\n'.join(map(str,out))''')
add('字符串模式匹配',3,'字符串/KMP','统计模式串在文本中出现的次数，允许不同匹配相互重叠。',
    '第一行为文本 s，第二行为模式串 p，均仅含小写英文字母。','输出匹配次数。模式串比文本长时答案为 0。','1 ≤ |s| ≤ 1000000，1 ≤ |p| ≤ 100000。',
    [('aaaaa\naaa','3'),('abc\nabcd','0')],
    '''s,p=text.split(); pi=[0]*len(p); j=0
for i in range(1,len(p)):
    while j and p[i]!=p[j]: j=pi[j-1]
    if p[i]==p[j]: j+=1
    pi[i]=j
j=answer=0
for c in s:
    while j and c!=p[j]: j=pi[j-1]
    if c==p[j]: j+=1
    if j==len(p): answer+=1; j=pi[j-1]
return str(answer)''')
add('最长回文子串',3,'字符串/Manacher','求最长连续回文子串的长度，区分子串与不连续的子序列。',
    '一行非空小写英文字母字符串。','输出最长回文子串长度。','1 ≤ |s| ≤ 200000。',
    [('babad','3'),('cbbd','2')],
    '''s='#'+'#'.join(text.strip())+'#'; rad=[0]*len(s); center=right=answer=0
for i in range(len(s)):
    if i<right: rad[i]=min(right-i,rad[2*center-i])
    while i-rad[i]-1>=0 and i+rad[i]+1<len(s) and s[i-rad[i]-1]==s[i+rad[i]+1]: rad[i]+=1
    if i+rad[i]>right: center,right=i,i+rad[i]
    answer=max(answer,rad[i])
return str(answer)''')
add('有向图强连通分量',3,'图论/强连通分量','若一个顶点集合内任意两点都可沿有向边互相到达，它构成一个强连通分量。求分量总数。允许自环、重复边和孤立顶点。',
    '第一行 n、m；接着 m 行有向边 u、v，编号为 1 到 n。','输出强连通分量个数。','1 ≤ n ≤ 100000，0 ≤ m ≤ 200000。',
    [('4 4\n1 2\n2 1\n2 3\n3 4','3'),('1 0','1')],
    '''it=iter(map(int,text.split())); n,m=next(it),next(it); g=[[] for _ in range(n)]; rg=[[] for _ in range(n)]
for u,v in zip(it,it): g[u-1].append(v-1); rg[v-1].append(u-1)
seen=[False]*n; order=[]
for root in range(n):
    if seen[root]: continue
    stack=[(root,False)]
    while stack:
        u,exit=stack.pop()
        if exit: order.append(u); continue
        if seen[u]: continue
        seen[u]=True; stack.append((u,True))
        for v in g[u]:
            if not seen[v]: stack.append((v,False))
seen=[False]*n; answer=0
for root in reversed(order):
    if seen[root]: continue
    answer+=1; stack=[root]; seen[root]=True
    while stack:
        u=stack.pop()
        for v in rg[u]:
            if not seen[v]: seen[v]=True; stack.append(v)
return str(answer)''')
add('树上最近公共祖先',3,'图论/树/倍增','树以 1 为根。对每个询问 u、v，输出它们的最近公共祖先，顶点自身也属于自己的祖先。',
    '第一行 n、q；接着 n-1 行无向边；再接着 q 行，每行 u、v。','每次询问各输出一行祖先编号。','1 ≤ n,q ≤ 100000，输入保证是一棵树，顶点编号为 1 到 n。',
    [('5 3\n1 2\n1 3\n2 4\n2 5\n4 5\n3 4\n2 4','2\n1\n2'),('1 1\n1 1','1')],
    '''it=iter(map(int,text.split())); n,q=next(it),next(it); g=[[] for _ in range(n+1)]
for _ in range(n-1):
    u,v=next(it),next(it); g[u].append(v); g[v].append(u)
depth=[-1]*(n+1); depth[1]=0; par=[0]*(n+1); queue=deque([1])
while queue:
    u=queue.popleft()
    for v in g[u]:
        if depth[v]<0: depth[v]=depth[u]+1; par[v]=u; queue.append(v)
up=[par]
for _ in range(n.bit_length()): up.append([up[-1][up[-1][v]] for v in range(n+1)])
out=[]
for _ in range(q):
    u,v=next(it),next(it)
    if depth[u]<depth[v]: u,v=v,u
    diff=depth[u]-depth[v]
    for k in range(diff.bit_length()):
        if diff>>k&1: u=up[k][u]
    if u!=v:
        for k in range(len(up)-1,-1,-1):
            if up[k][u]!=up[k][v]: u,v=up[k][u],up[k][v]
        u=par[u]
    out.append(u)
return '\\n'.join(map(str,out))''')

# 更多随机用例与独立朴素算法的预期值见 problem_bank_cases.py。
from problem_bank_cases import extend_cases
extend_cases(PROBLEMS, append_case, RNG)


def validate_catalog():
    titles = [p['title'] for p in PROBLEMS]
    if len(titles) != len(set(titles)):
        raise ValueError('题目标题重复')
    for p in PROBLEMS:
        if p['difficulty'] not in (1,2,3) or not p.get('tags'):
            raise ValueError('缺少难度或标签: '+p['title'])
        if len(p['cases']) < 10:
            raise ValueError('测试用例不足: '+p['title'])
        if len({a for a,b in p['cases']}) != len(p['cases']):
            raise ValueError('测试输入重复: '+p['title'])
        if any((s['input'],s['output']) not in p['cases'] for s in p['samples']):
            raise ValueError('样例未纳入判题: '+p['title'])
    return {'problems':len(PROBLEMS),'cases':sum(len(p['cases']) for p in PROBLEMS),
            'difficulty':dict(Counter(p['difficulty'] for p in PROBLEMS)),
            'tags':sorted({t for p in PROBLEMS for t in p['tags']})}
