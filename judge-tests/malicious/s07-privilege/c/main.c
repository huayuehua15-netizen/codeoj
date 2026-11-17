/* S-07 提权尝试（论文 6-2）：
 * 尝试 setuid(0)/setgid(0)。cap-drop ALL + no-new-privileges + 非 root 下必须 EPERM。
 * 提权被拒则 exit 0（隔离生效），任一调用意外成功则 exit 1（隔离失效）。
 */
#include <stdio.h>
#include <unistd.h>

int main(void) {
    int escaped = 0;
    if (setuid(0) == 0) {
        escaped = 1;
    }
    if (setgid(0) == 0) {
        escaped = 1;
    }
    printf("privilege %s\n", escaped ? "ESCALATED" : "blocked");
    return escaped ? 1 : 0;
}
