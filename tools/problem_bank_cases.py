"""小规模随机数据用朴素算法计算答案，大规模边界用例用可独立核对的闭式答案。"""
import itertools
import math
from collections import Counter, deque
from functools import lru_cache


def extend_cases(problems, add, rng):
    def arr(a): return str(len(a))+'\n'+' '.join(map(str,a))
    def words(a): return ' '.join(map(str,a))
    def graph(n, edges): return f'{n} {len(edges)}'+''.join('\n'+words(e) for e in edges)
    def closure(n,edges,directed=False):
        reach=[[i==j for j in range(n)] for i in range(n)]
        for u,v,*_ in edges:
            reach[u-1][v-1]=True
            if not directed: reach[v-1][u-1]=True
        for k in range(n):
            for i in range(n):
                for j in range(n): reach[i][j] |= reach[i][k] and reach[k][j]
        return reach
    def groups(reach):
        seen=set(); answer=0
        for i in range(len(reach)):
            if i not in seen:
                answer+=1; seen.update(j for j in range(len(reach)) if reach[i][j] and reach[j][i])
        return answer
    def edit(a,b):
        @lru_cache(None)
        def f(i,j):
            if i==len(a): return len(b)-j
            if j==len(b): return len(a)-i
            return min(1+f(i+1,j),1+f(i,j+1),(a[i]!=b[j])+f(i+1,j+1))
        return f(0,0)

    for trial in range(24):
        a=[rng.randint(-30,30) for _ in range(rng.randint(1,30))]; ordered=sorted(a)
        add('数组最值',arr(a),f'{min(a)} {max(a)}')
        add('升序排序',arr(a),words(ordered))
        unique=sorted(set(a)); add('有序数组去重',arr(ordered),str(len(unique))+'\n'+words(unique))
        s=''.join(rng.choices('abcdefxyz',k=rng.randint(1,50)))
        add('字符频次',s,words(s.count(chr(i)) for i in range(97,123)))
        brackets=''.join(rng.choices('()[]{}',k=rng.randint(1,40)))
        if trial%3==0: brackets='('*trial+'[]' + ')'*trial
        reduced=brackets
        while True:
            nxt=reduced.replace('()','').replace('[]','').replace('{}','')
            if nxt==reduced: break
            reduced=nxt
        add('括号匹配',brackets,'NO' if reduced else 'YES')
        n=rng.randint(0,10**18); b=rng.randint(2,16); digits=[]; x=n
        # 按位权从最高位提取，与参考程序的倒序除基法独立。
        power=1
        while power*b<=x: power*=b
        while power: digit,x=divmod(x,power); digits.append('0123456789ABCDEF'[digit]); power//=b
        add('进制转换',f'{n} {b}',''.join(digits))
        n,b,m=rng.randint(0,10000),rng.randint(0,40),rng.randint(1,10000)
        add('快速幂取模',f'{n} {b} {m}',n**b%m)
        queries=[]
        for _ in range(10):
            l=rng.randint(1,len(a)); queries.append((l,rng.randint(l,len(a))))
        add('区间和查询',f'{len(a)} {len(queries)}\n'+words(a)+''.join(f'\n{l} {r}' for l,r in queries),
            '\n'.join(str(sum(a[l-1:r])) for l,r in queries))
        other=sorted(rng.randint(-30,30) for _ in range(rng.randint(1,30)))
        add('合并有序数组',f'{len(a)} {len(other)}\n'+words(ordered)+'\n'+words(other),words(sorted(a+other)))
        n=rng.randint(0,2**63-1); add('二进制位计数',n,sum(int(d) for d in bin(n)[2:]))
        ops=[(l,r,rng.randint(-100,100)) for l,r in queries]; after=a[:]
        for l,r,d in ops:
            for i in range(l-1,r): after[i]+=d
        add('区间增量',f'{len(a)} {len(ops)}\n'+words(a)+''.join(f'\n{l} {r} {d}' for l,r,d in ops),words(after))
        add('最长无重复子串',s,max(r-l for l in range(len(s)) for r in range(l+1,len(s)+1) if len(set(s[l:r]))==r-l))
        k=rng.randint(-40,40)
        add('和为目标的连续子数组',f'{len(a)} {k}\n'+words(a),sum(sum(a[l:r])==k for l in range(len(a)) for r in range(l+1,len(a)+1)))
        k=rng.randint(1,len(a)); add('滑动窗口最大值',f'{len(a)} {k}\n'+words(a),words(max(a[i:i+k]) for i in range(len(a)-k+1)))
        add('第K小的数',f'{len(a)} {k}\n'+words(a),ordered[k-1])
        intervals=[]
        for _ in range(rng.randint(1,8)):
            l=rng.randint(0,15); intervals.append((l,rng.randint(l+1,20)))
        best=0
        for mask in range(1<<len(intervals)):
            sub=sorted(v for i,v in enumerate(intervals) if mask>>i&1)
            if all(sub[i][1]<=sub[i+1][0] for i in range(len(sub)-1)): best=max(best,len(sub))
        add('活动安排',str(len(intervals))+''.join(f'\n{l} {r}' for l,r in intervals),best)
        # 区间重叠关系的连通分量，与扫描线合并算法独立。
        edges=[(i+1,j+1) for i,x in enumerate(intervals) for j,y in enumerate(intervals) if max(x[0],y[0])<=min(x[1],y[1])]
        reach=closure(len(intervals),edges); seen=set(); merged=[]
        for i in range(len(intervals)):
            if i in seen: continue
            component=[j for j in range(len(intervals)) if reach[i][j]]; seen.update(component)
            merged.append((min(intervals[j][0] for j in component),max(intervals[j][1] for j in component)))
        merged.sort()
        add('合并区间',str(len(intervals))+''.join(f'\n{l} {r}' for l,r in intervals),str(len(merged))+''.join(f'\n{l} {r}' for l,r in merged))
        coins=sorted(rng.sample(range(1,12),rng.randint(1,5))); amount=rng.randint(0,50); queue=deque([(0,0)]); seen={0}; result=-1
        while queue:
            value,steps=queue.popleft()
            if value==amount: result=steps; break
            for c in coins:
                if value+c<=amount and value+c not in seen: seen.add(value+c); queue.append((value+c,steps+1))
        add('最少硬币',f'{len(coins)} {amount}\n'+words(coins),result)
        items=[(rng.randint(1,15),rng.randint(0,30)) for _ in range(rng.randint(1,8))]; cap=rng.randint(1,30)
        best=max(sum(v for i,(w,v) in enumerate(items) if mask>>i&1) for mask in range(1<<len(items))
                 if sum(w for i,(w,v) in enumerate(items) if mask>>i&1)<=cap)
        add('0-1背包',f'{len(items)} {cap}'+''.join(f'\n{w} {v}' for w,v in items),best)
        dp=[1]*len(a)
        for i in range(len(a)):
            for j in range(i):
                if a[j]<a[i]: dp[i]=max(dp[i],dp[j]+1)
        add('最长上升子序列',arr(a),max(dp))
        x=''.join(rng.choices('abc',k=rng.randint(1,9))); y=''.join(rng.choices('abc',k=rng.randint(1,9)))
        longest=0
        for mask in range(1<<len(x)):
            sub=''.join(c for i,c in enumerate(x) if mask>>i&1); it=iter(y)
            if all(any(v==c for v in it) for c in sub): longest=max(longest,len(sub))
        add('最长公共子序列',x+'\n'+y,longest)
        add('最短编辑距离',x+'\n'+y,edit(x,y))
        n,m=rng.randint(1,6),rng.randint(1,6); grid=[[int(rng.random()<0.25) for _ in range(m)] for _ in range(n)]
        dist=[[math.inf]*m for _ in range(n)]
        if not grid[0][0]: dist[0][0]=0
        # Bellman-Ford 式松弛，与 BFS 参考程序独立。
        for _ in range(n*m):
            for i in range(n):
                for j in range(m):
                    if grid[i][j]: continue
                    for u,v in ((i-1,j),(i+1,j),(i,j-1),(i,j+1)):
                        if 0<=u<n and 0<=v<m and not grid[u][v]: dist[i][j]=min(dist[i][j],dist[u][v]+1)
        add('网格最短路',f'{n} {m}\n'+'\n'.join(''.join(map(str,row)) for row in grid),-1 if math.isinf(dist[-1][-1]) else dist[-1][-1])
        n=rng.randint(1,6); edges=[(rng.randint(1,n),rng.randint(1,n)) for _ in range(rng.randint(0,9))]
        reach=closure(n,edges); add('连通分量统计',graph(n,edges),groups(reach))
        directed=closure(n,edges,True)
        add('课程依赖是否可完成',graph(n,edges),'NO' if any(u==v or directed[v-1][u-1] for u,v in edges) else 'YES')
        add('有向图强连通分量',graph(n,edges),groups(directed))
        weighted=[(u,v,rng.randint(-5,30)) for u,v in edges]; dist=[[0 if i==j else math.inf for j in range(n)] for i in range(n)]
        for u,v,w in weighted: dist[u-1][v-1]=min(dist[u-1][v-1],abs(w)); dist[v-1][u-1]=dist[u-1][v-1]
        for k in range(n):
            for i in range(n):
                for j in range(n): dist[i][j]=min(dist[i][j],dist[i][k]+dist[k][j])
        add('无向带权最短路',graph(n,[(u,v,abs(w)) for u,v,w in weighted]),-1 if math.isinf(dist[0][-1]) else dist[0][-1])
        best=math.inf
        for chosen in itertools.combinations(weighted,n-1):
            if groups(closure(n,chosen))==1: best=min(best,sum(w for u,v,w in chosen))
        add('最小生成树',graph(n,weighted),-1 if math.isinf(best) else best)
        labels=list(range(n+1)); operations=[]; answers=[]
        for i in range(20):
            u,v=rng.randint(1,n),rng.randint(1,n)
            if i%3:
                old,new=labels[u],labels[v]; labels=[new if l==old else l for l in labels]; operations.append(f'U {u} {v}')
            else: operations.append(f'Q {u} {v}'); answers.append('YES' if labels[u]==labels[v] else 'NO')
        add('并查集操作',f'{n} {len(operations)}\n'+'\n'.join(operations),'\n'.join(answers))
        n=rng.randint(1,35); parent=[0,0]+[rng.randint(1,i-1) for i in range(2,n+1)]; edges=[(i,parent[i]) for i in range(2,n+1)]
        g=[[] for _ in range(n+1)]
        for u,v in edges: g[u].append(v); g[v].append(u)
        diameter=0
        for root in range(1,n+1):
            stack=[(root,0,0)]
            while stack:
                u,p,d=stack.pop(); diameter=max(diameter,d)
                stack.extend((v,u,d+1) for v in g[u] if v!=p)
        add('树的直径',str(n)+''.join(f'\n{u} {v}' for u,v in edges),diameter)
        queries=[(rng.randint(1,n),rng.randint(1,n)) for _ in range(20)]; answers=[]
        for u,v in queries:
            ancestors=set()
            while u: ancestors.add(u); u=parent[u]
            while v not in ancestors: v=parent[v]
            answers.append(v)
        add('树上最近公共祖先',f'{n} {len(queries)}'+''.join(f'\n{u} {v}' for u,v in edges+queries),'\n'.join(map(str,answers)))
        n=rng.randint(2,50000); factors=[]; value=n
        for p in range(2,n+1):
            if p*p>value: break
            if value%p==0:
                e=0
                while value%p==0: value//=p; e+=1
                factors.append(f'{p}^{e}')
        if value>1: factors.append(f'{value}^1')
        add('质因数分解',n,' '.join(factors))
        add('逆序对计数',arr(a),sum(a[i]>a[j] for i in range(len(a)) for j in range(i+1,len(a))))
        updates=[]; answers=[]; after=a[:]
        for i in range(20):
            if i%2:
                at=rng.randint(1,len(a)); delta=rng.randint(-100,100); updates.append(f'1 {at} {delta}'); after[at-1]+=delta
            else:
                l=rng.randint(1,len(a)); r=rng.randint(l,len(a)); updates.append(f'2 {l} {r}'); answers.append(sum(after[l-1:r]))
        add('区间动态求和',f'{len(a)} {len(updates)}\n'+words(a)+'\n'+'\n'.join(updates),'\n'.join(map(str,answers)))
        updates=[]; answers=[]; after=a[:]
        for i in range(20):
            l=rng.randint(1,len(a)); r=rng.randint(l,len(a))
            if i%2:
                delta=rng.randint(-100,100); updates.append(f'1 {l} {r} {delta}')
                for j in range(l-1,r): after[j]+=delta
            else: updates.append(f'2 {l} {r}'); answers.append(min(after[l-1:r]))
        add('区间加与区间最小值',f'{len(a)} {len(updates)}\n'+words(a)+'\n'+'\n'.join(updates),'\n'.join(map(str,answers)))
        s=''.join(rng.choices('abc',k=rng.randint(1,50))); p=''.join(rng.choices('abc',k=rng.randint(1,8)))
        add('字符串模式匹配',s+'\n'+p,sum(s[i:i+len(p)]==p for i in range(len(s))))
        add('最长回文子串',s,max(r-l for l in range(len(s)) for r in range(l+1,len(s)+1) if s[l:r]==s[l:r][::-1]))

    # 极值与规模边界：答案由构造直接得到，避免用被测参考算法生成预期值。
    cases=[
        ('数组最值',arr([-10**9]+[0]*99998+[10**9]),'-1000000000 1000000000'),
        ('升序排序',arr(list(range(100000,0,-1))),words(range(1,100001))),
        ('有序数组去重',arr([7]*100000),'1\n7'),
        ('字符频次','z'*100000,'0 '*25+'100000'),
        ('括号匹配','('*50000+')'*50000,'YES'),
        ('括号匹配',')'*100000,'NO'),
        ('进制转换','1000000000000000000 16',format(10**18,'X')),
        ('快速幂取模','2 1000000000000000000 3','1'),
        ('快速幂取模','0 0 7','1'),
        ('区间和查询','100000 2\n'+words([10**9]*100000)+'\n1 100000\n100000 100000','100000000000000\n1000000000'),
        ('合并有序数组','100000 1\n'+words([5]*100000)+'\n5',words([5]*100001)),
        ('二进制位计数',2**63-1,63),
        ('区间增量','100000 2\n'+words([0]*100000)+'\n1 100000 1000000000\n1 100000 1000000000',words([2*10**9]*100000)),
        ('最长无重复子串','abcdefghijklmnopqrstuvwxyz'*3846,26),
        ('和为目标的连续子数组','100000 0\n'+words([0]*100000),100000*100001//2),
        ('滑动窗口最大值','100000 99999\n'+words(range(100000)),'99998 99999'),
        ('第K小的数','200000 200000\n'+words(range(200000)),199999),
        ('活动安排','100000'+''.join(f'\n{i} {i+1}' for i in range(100000)),100000),
        ('合并区间','100000'+''.join(f'\n{i} {i+100000}' for i in range(100000)),'1\n0 199999'),
        ('最少硬币','1 10000\n1',10000),
        ('0-1背包','100 10000'+'\n100 1000000'*100,100000000),
        ('最长上升子序列',arr(list(range(200000))),200000),
        ('最长上升子序列',arr(list(range(200000,0,-1))),1),
        ('最长公共子序列','a'*1000+'\n'+'a'*1000,1000),
        ('网格最短路','300 300\n'+('\n'.join(['0'*300]*300)),598),
        ('连通分量统计','100000 0',100000),
        ('课程依赖是否可完成','100000 99999'+''.join(f'\n{i} {i+1}' for i in range(1,100000)),'YES'),
        ('无向带权最短路','100000 99999'+''.join(f'\n{i} {i+1} 1000000000' for i in range(1,100000)),99999*10**9),
        ('最小生成树','100000 99999'+''.join(f'\n{i} {i+1} 1000000000' for i in range(1,100000)),99999*10**9),
        ('并查集操作','100000 2\nU 1 100000\nQ 1 100000','YES'),
        ('树的直径','100000'+''.join(f'\n{i} {i+1}' for i in range(1,100000)),99999),
        ('质因数分解',10**12,'2^12 5^12'),
        ('逆序对计数',arr(list(range(200000,0,-1))),200000*199999//2),
        ('最短编辑距离','a'*1000+'\n'+'b'*1000,1000),
        ('区间动态求和','100000 3\n'+words([10**9]*100000)+'\n2 1 100000\n1 100000 -1000000000\n2 1 100000','100000000000000\n99999000000000'),
        ('区间加与区间最小值','100000 3\n'+words([0]*100000)+'\n1 1 100000 -1000000000\n1 50000 100000 -1000000000\n2 1 100000','-2000000000'),
        ('字符串模式匹配','a'*1000000+'\n'+'a'*100000,900001),
        ('最长回文子串','a'*200000,200000),
        ('有向图强连通分量','100000 100000'+''.join(f'\n{i} {i+1}' for i in range(1,100000))+'\n100000 1',1),
        ('有向图强连通分量','100000 99999'+''.join(f'\n{i} {i+1}' for i in range(1,100000)),100000),
        ('树上最近公共祖先','100000 3'+''.join(f'\n{i} {i+1}' for i in range(1,100000))+'\n1 100000\n50000 100000\n100000 100000','1\n50000\n100000'),
    ]
    for title,text,answer in cases: add(title,text,answer)

    # 同时放大 n 与操作数量，避免只有“大数组 + 两次查询”时放过 O(nq) 程序。
    add('区间和查询','100000 100000\n'+words([10**9]*100000)+'\n1 100000'*100000,
        '\n'.join(['100000000000000']*100000))
    add('区间增量','100000 100000\n'+words([0]*100000)+'\n1 100000 1000000000'*100000,
        words([10**14]*100000))
    add('滑动窗口最大值','100000 50000\n'+words([7]*100000),words([7]*50001))
    add('并查集操作','100000 100000'+''.join(f'\nU {i} {i+1}' for i in range(1,50001))+'\nQ 1 50001'*50000,
        '\n'.join(['YES']*50000))
    add('区间动态求和','100000 100000\n'+words([0]*100000)+'\n1 100000 1\n2 1 100000'*50000,
        '\n'.join(map(str,range(1,50001))))
    add('区间加与区间最小值','100000 100000\n'+words([0]*100000)+'\n1 1 100000 1\n2 1 100000'*50000,
        '\n'.join(map(str,range(1,50001))))
    add('树上最近公共祖先','100000 100000'+''.join(f'\n{i} {i+1}' for i in range(1,100000))+'\n1 100000'*100000,
        '\n'.join(['1']*100000))
    prime=999983
    assert all(prime%d for d in range(2,math.isqrt(prime)+1))
    add('质因数分解',prime*prime,f'{prime}^2')
