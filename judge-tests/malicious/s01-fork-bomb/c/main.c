/* S-01 fork 炸弹（论文 6-2）：
 * 无限 fork 子进程。pids-limit 64 达到后 fork 失败（EAGAIN），
 * 循环空转直至容器内 timeout 超时 SIGKILL —— 预期 TLE（exit=124）。
 */
#include <unistd.h>

int main(void) {
    for (;;) {
        fork();
    }
    return 0;
}
