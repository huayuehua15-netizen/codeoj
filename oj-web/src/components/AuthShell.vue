<template>
  <div class="auth-page">
    <header class="auth-top">
      <router-link to="/home" class="auth-brand" aria-label="返回 CodeOJ 首页">
        <span class="brand-mark" aria-hidden="true">&lt;/&gt;</span>
        <span class="brand-word">Code<span>OJ</span></span>
        <span class="brand-sub" aria-hidden="true">ONLINE<br />JUDGE</span>
      </router-link>
      <span class="auth-top-note">在线代码评测 · 判题系统</span>
      <router-link class="auth-back" to="/problems">先去题库看看 →</router-link>
    </header>

    <main class="auth-main">
      <aside class="auth-aside" aria-hidden="true">
        <p class="kicker">ONLINE JUDGE / ACCESS</p>
        <h1 v-if="mode === 'register'">注册，<br /><span>开始提交。</span></h1>
        <h1 v-else>登录，<br /><span>继续提交。</span></h1>
        <p class="aside-copy">
          {{ mode === 'register'
            ? '注册后即可提交代码，判题结果实时推送，提交记录、错题和排名都跟着账号走。'
            : '判题结果实时推送，提交记录随时回看。错题、统计和排名都跟着账号走。' }}
        </p>

        <div class="auth-points">
          <div class="point">
            <span class="point-no">01</span>
            <span>五语言沙箱编译</span>
            <span class="point-en">COMPILE</span>
          </div>
          <div class="point">
            <span class="point-no">02</span>
            <span>测试点级判定</span>
            <span class="point-en">JUDGE</span>
          </div>
          <div class="point">
            <span class="point-no">03</span>
            <span>提交记录与错题本</span>
            <span class="point-en">TRACK</span>
          </div>
        </div>

        <div class="auth-terminal">
          <div class="auth-term-head">
            <span class="term-dots"><i></i><i></i><i></i></span>
            <span>codeoj / session</span>
          </div>
          <div class="auth-term-body">
            <p><span class="prompt">$</span> codeoj auth --{{ mode === 'register' ? 'register' : 'login' }}</p>
            <p class="dim">→ {{ mode === 'register' ? 'creating account' : 'verifying credentials' }}</p>
            <p class="ok">✓ {{ mode === 'register' ? 'account ready' : 'session ready' }}</p>
            <p class="dim">→ redirect /problems</p>
          </div>
        </div>
      </aside>

      <section class="auth-card">
        <header class="auth-head">
          <p class="kicker">ACCOUNT</p>
          <h2 class="auth-title">{{ title }}</h2>
          <p v-if="subtitle" class="auth-sub">{{ subtitle }}</p>
        </header>
        <slot />
        <footer class="auth-card-foot">
          <span class="foot-line" aria-hidden="true"></span>
          <span>CodeOJ · JUDGE SYSTEM</span>
        </footer>
      </section>
    </main>

    <footer class="auth-foot">
      <span class="auth-foot-motto">WRITE CODE. GET VERDICT.</span>
      <span class="dot" aria-hidden="true">·</span>
      <span>Spring Boot 3 · Vue 3 · Docker</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
  mode?: 'login' | 'register'
}>()
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-rows: auto 1fr auto;
  background: var(--oj-bg);
}

/* ---------- 顶栏 ---------- */
.auth-top {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px 32px;
  border-bottom: 1px solid var(--oj-border);
  background: rgba(255, 255, 255, 0.82);
}
.auth-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}
.brand-mark {
  width: 30px;
  height: 30px;
  border-radius: 7px;
  background: var(--oj-gray-900);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--oj-font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: -0.06em;
  line-height: 1;
}
.brand-word {
  font-size: 18px;
  font-weight: 750;
  letter-spacing: -0.035em;
  color: var(--oj-text);
  line-height: 1;
}
.brand-word span {
  color: var(--oj-primary);
}
.brand-sub {
  padding-left: 11px;
  border-left: 1px solid var(--oj-border-strong);
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  line-height: 1.45;
  letter-spacing: 0.1em;
  color: var(--oj-text-weak);
}
.auth-top-note {
  font-size: 11px;
  color: var(--oj-text-weak);
  padding-left: 18px;
  border-left: 1px solid var(--oj-border);
}
.auth-back {
  margin-left: auto;
  font-size: 12px;
  color: var(--oj-text-sub);
}
.auth-back:hover {
  color: var(--oj-primary);
}

/* ---------- 主区 ---------- */
.auth-main {
  width: 100%;
  max-width: 1160px;
  margin: 0 auto;
  padding: 64px 32px 56px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 404px;
  gap: 76px;
  align-items: center;
}

.auth-aside {
  min-width: 0;
}
.auth-aside h1 {
  margin: 18px 0 0;
  font-size: 52px;
  line-height: 1.15;
  letter-spacing: -0.055em;
  font-weight: 800;
  color: var(--oj-text);
}
.auth-aside h1 span {
  color: var(--oj-primary);
}
.aside-copy {
  margin: 20px 0 0;
  max-width: 27em;
  font-size: 13.5px;
  line-height: 1.9;
  color: var(--oj-text-sub);
}
.auth-points {
  margin-top: 30px;
  max-width: 430px;
  border-top: 1px solid var(--oj-border);
}
.point {
  display: grid;
  grid-template-columns: 28px 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--oj-border);
  font-size: 12.5px;
  color: var(--oj-gray-700);
}
.point-no {
  font-family: var(--oj-font-mono);
  font-size: 9px;
  color: var(--oj-primary);
}
.point-en {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.1em;
  color: var(--oj-text-weak);
}

/* 左侧终端：给登录页一点“系统感”，不做插画 */
.auth-terminal {
  margin-top: 30px;
  max-width: 430px;
  background: var(--oj-code-bg);
  border: 1px solid var(--oj-code-border);
  border-radius: 8px;
  overflow: hidden;
}
.auth-term-head {
  height: 34px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border-bottom: 1px solid var(--oj-code-border);
  font-family: var(--oj-font-mono);
  font-size: 9px;
  color: var(--oj-code-text-sub);
}
.term-dots {
  display: flex;
  gap: 4px;
}
.term-dots i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #3b4552;
}
.auth-term-body {
  padding: 11px 13px 12px;
  font-family: var(--oj-font-mono);
  font-size: 11px;
  line-height: 1.85;
  color: #c7d0dc;
}
.auth-term-body p {
  margin: 0;
}
.prompt {
  color: #8ea6ff;
  margin-right: 6px;
}
.dim {
  color: #7c8899;
}
.ok {
  color: #57d9a3;
}

/* ---------- 表单卡 ---------- */
.auth-card {
  position: relative;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  padding: 30px 30px 24px;
  box-shadow: var(--oj-shadow);
}
.auth-card::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 3px;
  background: var(--oj-primary);
  border-radius: 10px 10px 0 0;
  opacity: 0.9;
}
.auth-head {
  margin-bottom: 22px;
}
.auth-title {
  margin: 9px 0 0;
  font-size: 24px;
  font-weight: 750;
  letter-spacing: -0.035em;
  color: var(--oj-text);
}
.auth-sub {
  margin: 7px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--oj-text-sub);
}
.auth-card :deep(.el-form-item) {
  margin-bottom: 15px;
}
.auth-card :deep(.el-input__wrapper) {
  background: var(--oj-gray-50);
  box-shadow: 0 0 0 1px var(--oj-border) inset;
  padding: 3px 12px;
}
.auth-card :deep(.el-input__wrapper.is-focus) {
  background: var(--oj-card);
  box-shadow: 0 0 0 1px var(--oj-primary) inset, var(--oj-ring-primary);
}
.auth-card :deep(.el-input__inner) {
  height: 40px;
}
.auth-card :deep(.auth-btn) {
  width: 100%;
  height: 44px;
  margin-top: 5px;
  border-radius: 7px;
  font-size: 13px;
  letter-spacing: 0.08em;
}
.auth-card :deep(.auth-links) {
  margin: 16px 0 0;
  font-size: 12px;
  color: var(--oj-text-sub);
}
.auth-card :deep(.auth-links a) {
  color: var(--oj-primary);
  font-weight: 600;
}
.auth-card :deep(.auth-links a:hover) {
  color: var(--oj-primary-hover);
  text-decoration: underline;
  text-underline-offset: 3px;
}

.auth-card-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 24px;
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.08em;
  color: var(--oj-text-weak);
}
.foot-line {
  flex: 1;
  border-top: 1px solid var(--oj-border);
}

.auth-foot {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px 24px 20px;
  border-top: 1px solid var(--oj-border);
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.06em;
  color: var(--oj-text-weak);
}
.auth-foot-motto {
  color: var(--oj-gray-500);
}
.auth-foot .dot {
  opacity: 0.6;
}

@media (max-width: 900px) {
  .auth-main {
    grid-template-columns: 1fr;
    gap: 30px;
    padding: 38px 20px;
  }
  .auth-aside {
    display: none;
  }
  .auth-card {
    max-width: 404px;
    width: 100%;
    margin: 0 auto;
  }
  .auth-top {
    padding: 16px 20px;
  }
  .auth-top-note {
    display: none;
  }
}
</style>
