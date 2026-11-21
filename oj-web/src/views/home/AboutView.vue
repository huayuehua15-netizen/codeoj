<template>
  <div class="colophon">
    <header class="page-head">
      <div>
        <p class="kicker">DOCS</p>
        <h1 class="page-head-title">帮助与关于</h1>
        <p class="page-head-sub">三分钟上手 CodeOJ：怎么提交、状态怎么看、出错了找谁。</p>
      </div>
    </header>

    <div class="colophon-body">
      <aside class="toc" aria-label="页内目录">
        <p class="toc-title">DOCS INDEX</p>
        <a href="#guide" class="toc-link"><span>01</span>如何提交第一道题</a>
        <a href="#status" class="toc-link"><span>02</span>判题状态对照</a>
        <a href="#faq" class="toc-link"><span>03</span>常见问题</a>
        <a href="#about" class="toc-link"><span>04</span>关于与致谢</a>
        <p class="toc-note">CodeOJ<br />Spring Boot 3 · Vue 3</p>
      </aside>

      <main class="chapters">
        <section id="guide" class="chapter">
          <div class="chapter-head">
            <span class="chapter-no">01</span>
            <h2>如何提交第一道题</h2>
          </div>
          <ol class="steps">
            <li v-for="(s, i) in STEPS" :key="i" class="step">
              <span class="step-no serif-num">{{ String(i + 1).padStart(2, '0') }}</span>
              <div class="step-main">
                <p class="step-title">{{ s.title }}</p>
                <p class="step-desc">{{ s.desc }}</p>
              </div>
            </li>
          </ol>
        </section>

        <section id="status" class="chapter">
          <div class="chapter-head">
            <span class="chapter-no">02</span>
            <h2>判题状态对照表</h2>
            <span class="chapter-meta">9 种判题状态 · 与判题机一致</span>
          </div>
          <div class="status-table">
            <div class="status-head" aria-hidden="true">
              <span>CODE</span>
              <span>状态</span>
              <span>说明</span>
            </div>
            <div v-for="(s, code) in STATUS_MAP" :key="code" class="status-row">
              <span class="status-code mono">{{ code }}</span>
              <span class="status-name">
                <span class="status-dot" :style="{ background: statusColor(Number(code)) }" aria-hidden="true" />
                {{ s.label }}
              </span>
              <span class="status-desc">{{ STATUS_DESC[String(code)] }}</span>
            </div>
          </div>
        </section>

        <section id="faq" class="chapter">
          <div class="chapter-head">
            <span class="chapter-no">03</span>
            <h2>常见问题</h2>
          </div>
          <el-collapse class="faq">
            <el-collapse-item v-for="(q, i) in FAQ" :key="i" :title="q.q">
              <p class="faq-a">{{ q.a }}</p>
            </el-collapse-item>
          </el-collapse>
        </section>

        <section id="about" class="chapter">
          <div class="chapter-head">
            <span class="chapter-no">04</span>
            <h2>关于 CodeOJ</h2>
          </div>
          <p class="about-p">
            CodeOJ 是一个基于 Spring Boot 3 + Vue 3 + Docker 的在线代码评测与编程练习平台：
            判题服务消费消息队列、调度 Docker 沙箱完成真实编译与运行，业务侧提供题库、竞赛、
            统计与错题本等学习闭环。项目同时是一份毕业设计，设计文档见仓库 README。
          </p>
          <dl class="stack">
            <div v-for="t in STACK" :key="t.name" class="stack-row">
              <dt>{{ t.name }}</dt>
              <dd>{{ t.desc }}</dd>
            </div>
          </dl>
          <p class="colophon-end">
            CodeOJ · 在线判题系统 · 用代码验证想法
          </p>
        </section>
      </main>
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
.colophon {
  max-width: 1160px;
  margin: 0 auto;
  padding: 44px 32px 64px;
}

.colophon-body {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 22px;
  align-items: start;
  margin-top: 22px;
}

/* 文档导航 */
.toc {
  position: sticky;
  top: 92px;
  padding: 14px 12px 12px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
}
.toc-title {
  margin: 0 0 8px;
  padding: 0 6px;
  font-family: var(--oj-font-mono);
  font-size: 8px;
  letter-spacing: 0.1em;
  color: var(--oj-text-weak);
}
.toc-link {
  display: flex;
  gap: 8px;
  padding: 7px 6px;
  border-radius: 5px;
  font-size: 12px;
  color: var(--oj-text-sub);
  transition: all var(--oj-duration) var(--oj-ease);
}
.toc-link span {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  color: var(--oj-gray-400);
  padding-top: 2px;
}
.toc-link:hover {
  background: var(--oj-primary-soft);
  color: var(--oj-primary);
}
.toc-note {
  margin: 14px 0 0;
  padding: 11px 6px 0;
  border-top: 1px solid var(--oj-border);
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  line-height: 1.8;
  color: var(--oj-text-weak);
}

.chapters {
  min-width: 0;
}
.chapter {
  padding: 18px 20px 20px;
  margin-bottom: 12px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
}
.chapter:last-child {
  margin-bottom: 0;
}
.chapter-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 10px;
  margin-bottom: 12px;
  border-bottom: 1px solid var(--oj-border);
}
.chapter-no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 18px;
  padding: 0 5px;
  border-radius: 4px;
  background: var(--oj-primary-soft);
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  font-weight: 600;
  color: var(--oj-primary);
}
.chapter-head h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 750;
  letter-spacing: -0.03em;
  white-space: nowrap;
}
.chapter-meta {
  margin-left: auto;
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.04em;
  color: var(--oj-text-weak);
  white-space: nowrap;
}

/* 步骤 */
.steps {
  list-style: none;
  margin: 0;
  padding: 0;
}
.step {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 12px;
  padding: 11px 0;
  border-top: 1px solid var(--oj-border);
}
.step:first-child {
  border-top: 0;
  padding-top: 2px;
}
.step:last-child {
  padding-bottom: 2px;
}
.step-no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 5px;
  background: var(--oj-gray-100);
  font-family: var(--oj-font-mono);
  font-size: 9.5px;
  font-weight: 600;
  color: var(--oj-text-sub);
}
.step-title {
  margin: 0;
  font-size: 13px;
  font-weight: 650;
  color: var(--oj-text);
}
.step-desc {
  margin: 3px 0 0;
  font-size: 11.5px;
  line-height: 1.8;
  color: var(--oj-text-sub);
}

/* 状态表 */
.status-table {
  border: 1px solid var(--oj-border);
  border-radius: 7px;
  overflow: hidden;
}
.status-head {
  display: grid;
  grid-template-columns: 52px 118px minmax(0, 1fr);
  gap: 14px;
  padding: 7px 10px;
  border-bottom: 1px solid var(--oj-border);
  background: var(--oj-gray-50);
  font-family: var(--oj-font-mono);
  font-size: 8px;
  letter-spacing: 0.08em;
  color: var(--oj-text-weak);
}
.status-row {
  display: grid;
  grid-template-columns: 52px 118px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  padding: 7px 10px;
  border-bottom: 1px solid var(--oj-border);
}
.status-row:last-child {
  border-bottom: 0;
}
.status-code {
  font-size: 10px;
  color: var(--oj-text-weak);
}
.status-name {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--oj-text);
}
.status-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex-shrink: 0;
}
.status-desc {
  font-size: 11px;
  line-height: 1.7;
  color: var(--oj-text-sub);
}

/* FAQ */
.faq :deep(.el-collapse-item__header) {
  font-family: var(--oj-font-sans);
  font-size: 13px;
  font-weight: 600;
  color: var(--oj-text);
  height: 44px;
}
.faq :deep(.el-collapse-item__header.is-active) {
  color: var(--oj-primary);
}
.faq :deep(.el-collapse-item__wrap) {
  border-bottom-color: var(--oj-border);
}
.faq-a {
  margin: 0;
  padding-bottom: 4px;
  font-size: 11.5px;
  line-height: 1.85;
  color: var(--oj-text-sub);
}

/* 关于 */
.about-p {
  margin: 0 0 14px;
  font-size: 12px;
  line-height: 1.9;
  color: var(--oj-text-sub);
}
.stack {
  margin: 0;
  padding: 0;
  border-top: 1px solid var(--oj-border);
}
.stack-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 14px;
  padding: 7px 0;
  border-bottom: 1px solid var(--oj-border);
}
.stack-row dt {
  font-size: 11px;
  font-weight: 600;
  color: var(--oj-text);
}
.stack-row dd {
  margin: 0;
  font-size: 11px;
  line-height: 1.7;
  color: var(--oj-text-sub);
}
.colophon-end {
  margin: 18px 0 0;
  padding-top: 12px;
  text-align: center;
  border-top: 1px solid var(--oj-border);
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.06em;
  color: var(--oj-text-weak);
}

@media (max-width: 800px) {
  .colophon {
    padding: 30px 18px 52px;
  }
  .colophon-body {
    grid-template-columns: 1fr;
    gap: 14px;
    margin-top: 18px;
  }
  .toc {
    position: static;
    display: flex;
    flex-wrap: wrap;
    gap: 2px 6px;
    padding: 10px;
  }
  .toc-title {
    width: 100%;
    margin-bottom: 2px;
  }
  .toc-link {
    padding: 5px 7px;
  }
  .toc-note {
    display: none;
  }
  .chapter-head h2 {
    font-size: 16px;
    white-space: normal;
  }
  .chapter-meta {
    display: none;
  }
  .status-head,
  .status-row {
    grid-template-columns: 36px 100px minmax(0, 1fr);
    gap: 9px;
    padding-left: 8px;
    padding-right: 8px;
  }
}
</style>
