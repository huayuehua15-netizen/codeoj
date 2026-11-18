<template>
  <div class="auth-page">
    <header class="auth-brand" aria-hidden="true">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="8 6 3 12 8 18" />
        <polyline points="16 6 21 12 16 18" />
        <line x1="13.5" y1="4.5" x2="10.5" y2="19.5" />
      </svg>
      <span>Code<b>OJ</b></span>
    </header>

    <main class="auth-main">
      <section class="auth-card card">
        <h1 class="auth-title">{{ title }}</h1>
        <p v-if="subtitle" class="auth-sub">{{ subtitle }}</p>
        <slot />
      </section>
    </main>

    <footer class="auth-foot">
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
/* 登录/注册：平面居中一栏式。
   之前的双栏渐变品牌页是典型的"模板脸"——工程产品的登录页
   （GitHub/Linear）都极简：一个词标、一张表单卡，结束。 */
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-rows: auto 1fr auto;
  background: var(--oj-bg);
}

.auth-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 28px 16px 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--oj-text);
}
.auth-brand svg {
  width: 18px;
  height: 18px;
  color: var(--oj-text);
}
.auth-brand b {
  color: var(--oj-primary);
}

.auth-main {
  display: grid;
  place-items: center;
  padding: 24px 16px 40px;
}
.auth-card {
  width: 100%;
  max-width: 380px;
  padding: 28px 28px 24px;
}
.auth-title {
  margin: 0 0 6px;
  font-size: 19px;
  font-weight: 600;
  color: var(--oj-text);
}
.auth-sub {
  margin: 0 0 18px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--oj-text-sub);
}

.auth-foot {
  padding: 14px 16px 22px;
  text-align: center;
  font-size: 12px;
  color: var(--oj-text-weak);
}
</style>
