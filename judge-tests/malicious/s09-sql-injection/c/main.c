/* S-09 SQL 注入载荷（论文 6-2）：
 * 载荷仅存于注释中，随正常代码一起提交。
 * 全链路测试断言：平台按普通文本处理，正常判题（AC/WA/CE），
 * 接口不崩溃、不泄露数据；题目搜索接口对注入串同样安全。
 *   ' OR '1'='1 ; DROP TABLE user; --
 */
#include <stdio.h>

int main(void) {
    printf("hello\n");
    return 0;
}
