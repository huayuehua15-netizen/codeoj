/* S-05 网络访问（论文 6-2）：
 * 尝试连接公网（8.8.8.8:80）与内网网关（172.17.0.1）。
 * --network none 下连接必须全部失败：全部被阻断则 exit 0（隔离生效），
 * 任一连接意外成功则 exit 1（隔离失效）。
 */
#include <arpa/inet.h>
#include <netinet/in.h>
#include <stdio.h>
#include <sys/socket.h>
#include <unistd.h>

static int try_connect(const char *ip) {
    int s = socket(AF_INET, SOCK_STREAM, 0);
    if (s < 0) {
        return 1; /* 建 socket 失败也算阻断 */
    }
    struct sockaddr_in addr;
    addr.sin_family = AF_INET;
    addr.sin_port = htons(80);
    addr.sin_addr.s_addr = inet_addr(ip);
    int ok = connect(s, (struct sockaddr *) &addr, sizeof addr);
    close(s);
    return ok; /* 0 = 连通（隔离失效） */
}

int main(void) {
    int leaked = 0;
    if (try_connect("8.8.8.8") == 0) {
        leaked = 1;   /* 公网连通 */
    }
    if (try_connect("172.17.0.1") == 0) {
        leaked = 1;   /* 内网网关连通 */
    }
    printf("network %s\n", leaked ? "REACHABLE" : "blocked");
    return leaked ? 1 : 0;
}
