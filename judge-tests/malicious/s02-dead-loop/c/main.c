/* S-02 死循环（论文 6-2）：
 * 空转死循环。容器内 timeout 在时限到后 SIGTERM、1s 后 SIGKILL —— 预期 TLE（exit=124）。
 */
int main(void) {
    for (;;) {
    }
    return 0;
}
