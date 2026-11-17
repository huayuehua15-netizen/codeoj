/* S-08 输出轰炸（论文 6-2）：
 * 无限输出。ulimit fsize=16MB 触发 SIGXFSZ 杀死进程（exit=153），
 * 或 runner 截断 16MB 并置 trunc=1 —— 二者均视为输出被限制。
 */
#include <stdio.h>
#include <string.h>

int main(void) {
    static char buf[65536];
    memset(buf, 'A', sizeof buf);
    buf[sizeof buf - 1] = '\n';
    while (fwrite(buf, 1, sizeof buf, stdout) == sizeof buf) {
    }
    return 1;
}
