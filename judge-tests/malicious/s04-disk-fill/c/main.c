/* S-04 磁盘写满（论文 6-2）：
 * 持续向工作目录写大文件。ulimit fsize=16MB 触发 SIGXFSZ 杀死进程（exit=153）；
 * 若限制被绕过，fwrite 返回短计数后退出（exit=1），同样视为拦截失败。
 */
#include <stdio.h>
#include <string.h>

int main(void) {
    FILE *f = fopen("big.bin", "w");
    if (!f) {
        return 2;
    }
    static char buf[65536];
    memset(buf, 'B', sizeof buf);
    while (fwrite(buf, 1, sizeof buf, f) == sizeof buf) {
    }
    fclose(f);
    return 1; /* 未被 fsize 限制杀掉 → 隔离失败 */
}
