<template>
  <div class="page-container about-page">
    <header class="oj-stage">
      <div>
        <h1 class="oj-stage-title">帮助与关于</h1>
        <p class="oj-stage-sub">三分钟上手 CodeOJ：怎么提交、状态怎么看、出错了找谁</p>
      </div>
    </header>

    <div class="about-grid">
      <!-- 使用指南 -->
      <div class="card">
        <h2>如何提交第一道题</h2>
        <ol class="steps">
          <li v-for="(s, i) in STEPS" :key="i" class="step">
            <span class="step-no mono">{{ i + 1 }}</span>
            <div>
              <p class="step-title">{{ s.title }}</p>
              <p class="step-desc">{{ s.desc }}</p>
            </div>
          </li>
        </ol>
      </div>

      <!-- 判题状态对照 -->
      <div class="card">
        <h2>判题状态对照表</h2>
        <p class="card-sub">与论文表 4-8 的状态机一致，共九态</p>
        <ul class="status-list">
          <li v-for="(s, code) in STATUS_MAP" :key="code" class="status-row">
            <span class="status-code mono">{{ code }}</span>
            <span class="status-dot" :style="{ background: statusColor(Number(code)) }" />
            <span class="status-name">{{ s.label }}</span>
            <span class="status-desc">{{ STATUS_DESC[code] }}</span>
          </li>
        </ul>
      </div>

      <!-- 常见问题 -->
      <div class="card">
        <h2>常见问题</h2>
        <el-collapse class="faq">
          <el-collapse-item v-for="(q, i) in FAQ" :key="i" :title="q.q">
            <p class="faq-a">{{ q.a }}</p>
          </el-collapse-item>
        </el-collapse>
      </div>

      <!-- 关于平台 -->
      <div class="card">
        <h2>关于 CodeOJ</h2>
        <p class="about-p">
          CodeOJ 是一个基于 Spring Boot 3 + Vue 3 + Docker 的在线代码评测与编程练习平台，
          由判题服务消费消息队列、调度 Docker 沙箱完成真实编译与运行，业务侧提供题库、竞赛、
          统计与错题本等学习闭环。项目同时是一份毕业设计，设计文档见仓库 README。
        </p>
        <ul class="stack-list">
          <li v-for="t in STACK" :key="t.name" class="stack-row">
            <span class="stack-name">{{ t.name }}</span>
            <span class="stack-desc">{{ t.desc }}</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { STATUS_MAP, statusColor } from '@/utils/status'

const STEPS = [
  { title: '在题库选题', desc: '按难度或标签筛选，点进题阅读题面与样例；题目支持 Markdown 与嵌入式交互示例。' },
  { title: '在线编写代码', desc: '编辑器带语法高亮与补全，切换语言会载入对应模板；JS 题目可在浏览器沙箱先自测。' },
  { title: '提交并等待判题', desc: '提交后进入队列，页面通过 WebSocket 实时推送「排队 → 判题中 → 出结果」。' },
  { title: '对照明细修改', desc: '判题结果给出用例级表格：每个测试点的状态、耗时、内存；点开错误信息定位问题。' },
]

/** 判题状态的一句话解释（与 CONTRACT §2 状态机口径一致） */
const STATUS_DESC: Record<string, string> = {
  0: '已入队，等待判题服务领取',
  1: '沙箱正在编译或执行用例',
  2: '全部测试点输出与期望一致',
  3: '程序跑完了，但输出和期望不一致',
  4: '超出题目时间限制，被沙箱终止',
  5: '超出题目内存限制，被沙箱终止',
  6: '运行时异常（数组越界、除零、栈溢出等）',
  7: '编译失败，见错误信息里的编译器输出',
  8: '判题系统异常（镜像缺失等），请联系管理员',
}

const FAQ = [
  { q: '为什么提交后一直「排队中」？', a: '判题服务（oj-judge）未启动或队列积压。判题服务每 20 秒向平台发一次心跳，可在管理端「队列监控」查看在线实例与积压量；若长时间无实例在线，请联系管理员。' },
  { q: '本地能跑，为什么判题是 WA？', a: '本地跑的是你自己的测试数据，判题用的是题目隐藏用例。先对照样例检查输出格式：行尾多余空格、末尾换行、大小写都可能导致不一致。' },
  { q: '同一份代码两次结果不一样？', a: '判题耗时/内存存在正常波动（沙箱冷启动、并发争抢）。以大多数用例的稳定表现为准；若差异巨大且可复现，请通过公告联系方式反馈。' },
  { q: '改密码后会话为什么失效？', a: '改密成功后当前令牌立即进入 Redis 黑名单，需要重新登录——这是防止旧令牌继续有效的安全设计。' },
]

const STACK = [
  { name: '后端', desc: 'JDK 17 · Spring Boot 3.2 · Spring Security(JWT) · MyBatis-Plus' },
  { name: '消息与缓存', desc: 'RabbitMQ 判题队列（手动 ACK + 死信恢复）· Redis 排行榜/限流/幂等' },
  { name: '判题沙箱', desc: 'docker-java 调度一次性容器，五语言独立镜像，双超时 + 16MB 输出截断' },
  { name: '前端', desc: 'Vue 3 · Vite · TypeScript · Pinia · Element Plus · Monaco Editor · ECharts' },
  { name: '存储', desc: 'MySQL 8（utf8mb4/InnoDB）· Redis 7' },
]
</script>

<style scoped>
.about-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}
@media (max-width: 860px) {
  .about-grid {
    grid-template-columns: 1fr;
  }
}
.card-sub {
  margin: 0 0 10px;
  font-size: 12.5px;
  color: var(--oj-text-sub);
}

/* 步骤 */
.steps {
  list-style: none;
  margin: 0;
  padding: 0;
}
.step {
  display: flex;
  gap: 14px;
  padding: 10px 0;
}
.step + .step {
  border-top: 1px solid var(--oj-gray-100);
}
.step-no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 4px;
  background: var(--oj-primary);
  color: var(--oj-white);
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}
.step-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--oj-text);
}
.step-desc {
  margin: 4px 0 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--oj-text-sub);
}

/* 状态表 */
.status-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.status-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 0;
}
.status-row + .status-row {
  border-top: 1px solid var(--oj-gray-100);
}
.status-code {
  font-size: 11.5px;
  color: var(--oj-text-weak);
  width: 16px;
  text-align: right;
  flex-shrink: 0;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.status-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--oj-text);
  width: 64px;
  flex-shrink: 0;
}
.status-desc {
  font-size: 12.5px;
  color: var(--oj-text-sub);
  min-width: 0;
}

/* FAQ */
.faq :deep(.el-collapse-item__header) {
  font-weight: 600;
  color: var(--oj-text);
}
.faq-a {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--oj-text-sub);
}

/* 关于 */
.about-p {
  margin: 0 0 14px;
  font-size: 13.5px;
  line-height: 1.9;
  color: var(--oj-text-sub);
}
.stack-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.stack-row {
  display: flex;
  gap: 14px;
  padding: 7px 0;
}
.stack-row + .stack-row {
  border-top: 1px solid var(--oj-gray-100);
}
.stack-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--oj-text);
  width: 76px;
  flex-shrink: 0;
}
.stack-desc {
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--oj-text-sub);
}
</style>
