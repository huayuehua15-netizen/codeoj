<template>
  <div class="auth-page">
    <header class="auth-brand">
      <span class="seal" aria-hidden="true">判</span>
      <span class="wordmark">Code<em>OJ</em></span>
    </header>

    <main class="auth-main">
      <section class="auth-card card">
        <h1 class="auth-title">{{ title }}</h1>
        <p v-if="subtitle" class="auth-sub">{{ subtitle }}</p>
        <slot />
      </section>
    </main>

    <footer class="auth-foot">
      <span class="foot-motto">判题如批改，落笔即成长</span>
      <span class="dot" aria-hidden="true">·</span>
      <span>CodeOJ · 在线代码评测与编程练习平台</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  /** 卡片主标题，例如「欢迎回来」「创建账号」 */
  title: string
  /** 标题下方的一句引导文案，可省略 */
  subtitle?: string
}>()
</script>

<style scoped>
/* 登录/注册：全暗稿纸页。
   深松烟墨铺满整页，绿相稿纸竖线 + 纸张噪点做质感，
   纸白衬线词标压在正中——一张纸卡浮在墨卷上，"作答"的仪式感。 */
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-rows: auto 1fr auto;
  background-image:
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3CfeComponentTransfer%3E%3CfeFuncA type='linear' slope='0.05'/%3E%3C/feComponentTransfer%3E%3C/filter%3E%3Crect width='160' height='160' filter='url(%23n)'/%3E%3C/svg%3E"),
    repeating-linear-gradient(
      to right,
      transparent 0,
      transparent 31px,
      rgba(159, 214, 176, 0.06) 31px,
      rgba(159, 214, 176, 0.06) 32px
    ),
    linear-gradient(160deg, #14211a 0%, #1c2a22 55%, #16231c 100%);
}

.auth-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 34px 16px 0;
}
.seal {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 7px;
  background: var(--oj-seal);
  color: var(--oj-white);
  font-family: var(--oj-font-display);
  font-size: 19px;
  font-weight: 700;
  line-height: 1;
  user-select: none;
}
.wordmark {
  font-family: var(--oj-font-display);
  font-size: 21px;
  font-weight: 700;
  color: var(--oj-white);
  letter-spacing: 0.03em;
}
.wordmark em {
  font-style: normal;
  color: #9fd6b0;
}

.auth-main {
  display: grid;
  place-items: center;
  padding: 28px 16px 40px;
}
.auth-card {
  width: 100%;
  max-width: 380px;
  padding: 28px 28px 24px;
  box-shadow: var(--oj-shadow-lg);
}
.auth-title {
  margin: 0 0 6px;
  font-family: var(--oj-font-display);
  font-size: 20px;
  font-weight: 700;
  color: var(--oj-text);
  letter-spacing: 0.02em;
}
.auth-sub {
  margin: 0 0 18px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--oj-text-sub);
}

.auth-foot {
  padding: 14px 16px 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
  color: rgba(215, 228, 216, 0.45);
}
.foot-motto {
  font-family: var(--oj-font-display);
  letter-spacing: 0.12em;
  color: rgba(215, 228, 216, 0.6);
}
.auth-foot .dot {
  opacity: 0.6;
}
</style>
