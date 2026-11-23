<template>
  <div class="auth-page">
    <header class="auth-top">
      <router-link to="/home" class="auth-brand" aria-label="返回 CodeOJ 首页">
        <span class="brand-mark" aria-hidden="true">&lt;/&gt;</span>
        <span class="brand-word">Code<span>OJ</span></span>
      </router-link>
      <span class="auth-top-note">在线判题 · 编程练习</span>
      <router-link class="auth-back" to="/problems">返回题库</router-link>
    </header>

    <main class="auth-main">
      <aside class="auth-aside" aria-hidden="true">
        <h1 v-if="mode === 'register'">注册，<br /><span>开始提交。</span></h1>
        <h1 v-else>登录，<br /><span>继续提交。</span></h1>
        <p class="aside-copy">
          {{ mode === 'register'
            ? '注册后即可提交代码，判题结果实时推送，提交记录、错题和排名都跟着账号走。'
            : '判题结果实时推送，提交记录随时回看。错题、统计和排名都跟着账号走。' }}
        </p>

        <ul class="auth-points">
          <li>五种语言沙箱编译</li>
          <li>测试点级判定，结果实时推送</li>
          <li>提交记录、错题本和个人统计</li>
        </ul>
      </aside>

      <section class="auth-card">
        <header class="auth-head">
          <h2 class="auth-title">{{ title }}</h2>
          <p v-if="subtitle" class="auth-sub">{{ subtitle }}</p>
        </header>
        <slot />
      </section>
    </main>

    <footer class="auth-foot">
      <span>© 2026 CodeOJ</span>
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
  background: var(--oj-card);
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
  max-width: 1060px;
  margin: 0 auto;
  padding: 56px 32px 48px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 64px;
  align-items: center;
}

.auth-aside {
  min-width: 0;
}
.auth-aside h1 {
  margin: 0;
  font-size: 42px;
  line-height: 1.2;
  letter-spacing: -0.05em;
  font-weight: 750;
  color: var(--oj-text);
}
.auth-aside h1 span {
  color: var(--oj-primary);
}
.aside-copy {
  margin: 18px 0 0;
  max-width: 27em;
  font-size: 13px;
  line-height: 1.9;
  color: var(--oj-text-sub);
}
.auth-points {
  list-style: none;
  margin: 28px 0 0;
  padding: 0;
  max-width: 430px;
  border-top: 1px solid var(--oj-border);
}
.auth-points li {
  padding: 10px 0;
  border-bottom: 1px solid var(--oj-border);
  font-size: 12.5px;
  color: var(--oj-gray-700);
}
.auth-card {
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 4px;
  padding: 26px 28px 24px;
}
.auth-head {
  margin-bottom: 20px;
}
.auth-title {
  margin: 0;
  font-size: 22px;
  font-weight: 750;
  letter-spacing: -0.03em;
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
  border-radius: 4px;
  font-size: 13px;
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

.auth-foot {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 24px 20px;
  border-top: 1px solid var(--oj-border);
  font-size: 10px;
  color: var(--oj-text-weak);
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
