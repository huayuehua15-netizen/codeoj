// S-10 编译期攻击（论文 6-2）：
// 模板递归使类型大小指数爆炸（A<30> ≈ 2^30 字节），
// 编译容器内存/时间受限（512MB / 10s），编译进程被杀 —— 预期 CE。
template <int N>
struct A {
    A<N - 1> a;
    A<N - 1> b;
};

template <>
struct A<0> {
    char c;
};

int main() {
    A<30> x;
    (void) x;
    return 0;
}
