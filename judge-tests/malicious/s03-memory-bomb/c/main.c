/* S-03 内存炸弹（论文 6-2）：
 * 渐进式分配：每轮申请 64MB 并立即写入，直到 cgroup 内存限制触发 OOM kill。
 * 一次性 malloc 大内存可能因 overcommit 策略返回 NULL，分块写入保证
 * 真实触及物理内存，稳定触发 MLE（exit=137）。
 */
#include <stdlib.h>
#include <string.h>

#define CHUNK (64UL * 1024 * 1024) /* 64MB */

int main(void) {
    for (;;) {
        char *p = (char *) malloc(CHUNK);
        if (!p) {
            return 0;
        }
        memset(p, 0x41, CHUNK);
    }
}
