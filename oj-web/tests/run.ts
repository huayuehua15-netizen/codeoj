/**
 * 前端测试统一入口。
 *
 * 为什么不用 `node --test tests/`：
 * 那条命令会为每个测试文件**派生一个子进程**，而本机 DSH 沙箱禁止父进程捕获子进程的管道输出，
 * 实测会直接报 `spawn EPERM`（`node:internal/test_runner/runner:377`）。
 * 改成「在一个进程里 import 所有测试文件」即可绕开子进程，
 * 语义完全一致：Node 的 `node:test` 在模块被 import 时就会注册用例，进程退出码同样反映成败。
 *
 * 新增测试文件时在这里补一行 import 即可。
 */
import './status.test.ts'
import './datetime.test.ts'
import './heatmap.test.ts'
import './preferences.test.ts'
