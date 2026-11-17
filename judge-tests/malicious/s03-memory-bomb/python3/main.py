# S-03 内存炸弹 Python 版（论文 6-2）
# 一次性分配 2GB：cgroup 限制下要么 OOM kill（exit=137），要么 MemoryError（exit=1）。
try:
    data = bytearray(2 * 1024 * 1024 * 1024)
except MemoryError:
    exit(1)
print(len(data))
