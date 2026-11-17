/* S-06 读取宿主机信息（论文 6-2）：
 * 尝试读取敏感路径。非 root（-u 1000:1000）下 /etc/shadow 应 EACCES，
 * /var/run/docker.sock 未挂载应不存在，/proc/1/root/host 为无意义路径应不存在。
 * 全部被阻断则 exit 0（隔离生效），任一可读则 exit 1（隔离失效）。
 */
#include <stdio.h>
#include <unistd.h>

int main(void) {
    int leaked = 0;
    FILE *f;

    if ((f = fopen("/etc/shadow", "r")) != NULL) {   /* 非 root 应不可读 */
        leaked = 1;
        fclose(f);
    }
    if ((f = fopen("/var/run/docker.sock", "r")) != NULL) { /* 应不存在 */
        leaked = 1;
        fclose(f);
    }
    if (access("/proc/1/root/host", F_OK) == 0) {    /* 应不存在 */
        leaked = 1;
    }
    if (access("/proc/1/root/etc/passwd", F_OK) == 0 && leaked) {
        leaked = 1; /* 容器 init 视图内 passwd 存在属正常，不影响判定 */
    }
    printf("host-info %s\n", leaked ? "LEAKED" : "blocked");
    return leaked ? 1 : 0;
}
