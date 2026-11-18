<template>
  <div class="auth-page">
    <aside class="brand" aria-hidden="true">
      <div class="brand-head">
        <div class="brand-logo">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="8 6 3 12 8 18" />
            <polyline points="16 6 21 12 16 18" />
            <line x1="13.5" y1="4.5" x2="10.5" y2="19.5" />
          </svg>
          <span>Code<b>OJ</b></span>
        </div>
        <h1 class="brand-title">把每一次提交<br />都变成看得见的进步</h1>
        <p class="brand-desc">
          从题目练习到竞赛排名，提交即判题、用例级结果实时推送。
          写错的每一行，都会告诉你错在哪里。
        </p>
        <ul class="brand-features">
          <li>
            <span class="fi">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M13 2 4 14h7l-1 8 9-12h-7l1-8z" />
              </svg>
            </span>
            <span><b>实时判题</b> · WebSocket 推送排队、判题中、出结果三态，无需刷新</span>
          </li>
          <li>
            <span class="fi">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="4" width="18" height="16" rx="3" />
                <path d="M8 10h8M8 14h5" />
              </svg>
            </span>
            <span><b>五种语言</b> · C / C++ / Java / Python / JavaScript，独立沙箱编译执行</span>
          </li>
          <li>
            <span class="fi">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M8 21h8M12 17v4M17 4h3v3a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5V4h3" />
                <path d="M7 4v4a5 5 0 0 0 10 0V4H7z" />
              </svg>
            </span>
            <span><b>竞赛与排行</b> · ACM 赛制、实时榜单、解题数稳定性统计</span>
          </li>
        </ul>
      </div>
      <p class="brand-foot">CodeOJ · 在线代码评测与编程练习平台</p>
    </aside>

    <main class="panel">
      <section class="auth-card card">
        <div class="auth-logo">Code<span>OJ</span></div>
        <p class="mobile-sub">在线代码评测与编程练习平台</p>
        <h2 class="auth-title">{{ title }}</h2>
        <p v-if="subtitle" class="auth-sub">{{ subtitle }}</p>
        <slot />
      </section>
    </main>
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
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(0, 1fr);
  background: var(--oj-gradient-brand-deep);
}

/* ---------- 左侧品牌区 ---------- */
.brand {
  position: relative;
  overflow: hidden;
  padding: 56px 56px 40px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: var(--oj-on-brand);
}
/* 网格底纹 */
.brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(to right, var(--oj-grid-line-04) 1px, transparent 1px),
    linear-gradient(to bottom, var(--oj-grid-line-04) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: radial-gradient(120% 90% at 20% 0%, #000 35%, transparent 78%);
  -webkit-mask-image: radial-gradient(120% 90% at 20% 0%, #000 35%, transparent 78%);
  pointer-events: none;
}
/* 右上角光晕 */
.brand::after {
  content: '';
  position: absolute;
  top: -180px;
  right: -140px;
  width: 460px;
  height: 460px;
  background: var(--oj-glow-accent);
  pointer-events: none;
}
.brand-head {
  position: relative;
  z-index: 1;
}
.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  font-size: 21px;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: var(--oj-white);
}
.brand-logo svg {
  width: 26px;
  height: 26px;
  color: var(--oj-on-brand-85);
}
.brand-logo b {
  color: var(--oj-indigo-400);
}
.brand-title {
  margin: 40px 0 16px;
  font-size: clamp(25px, 2.5vw, 33px);
  line-height: 1.42;
  font-weight: 800;
  letter-spacing: 0.6px;
  color: var(--oj-white);
}
.brand-desc {
  margin: 0;
  max-width: 27em;
  font-size: 13.5px;
  line-height: 1.95;
  color: var(--oj-on-brand-75);
}
.brand-features {
  list-style: none;
  margin: 34px 0 0;
  padding: 0;
  display: grid;
  gap: 15px;
}
.brand-features li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  font-size: 13px;
  line-height: 1.75;
  color: var(--oj-on-brand-70);
}
.brand-features b {
  color: var(--oj-on-brand);
  font-weight: 600;
}
.fi {
  flex: none;
  width: 28px;
  height: 28px;
  margin-top: 1px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  color: var(--oj-on-brand-90);
  background: var(--oj-on-brand-08);
  border: 1px solid var(--oj-on-brand-16);
}
.fi svg {
  width: 15px;
  height: 15px;
}
.brand-foot {
  position: relative;
  z-index: 1;
  margin: 44px 0 0;
  font-size: 11.5px;
  letter-spacing: 0.4px;
  color: var(--oj-on-brand-70);
}

/* ---------- 右侧表单区 ---------- */
.panel {
  display: grid;
  place-items: center;
  padding: 40px 24px;
}
.auth-card {
  width: 100%;
  max-width: 412px;
  padding: 40px 36px 32px;
  border-radius: var(--oj-radius);
  box-shadow: var(--oj-shadow-lg);
}
.auth-logo {
  display: none;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: var(--oj-indigo-900);
}
.auth-logo span {
  color: var(--oj-primary);
}
.mobile-sub {
  display: none;
  margin: 6px 0 22px;
  font-size: 12px;
  color: var(--oj-gray-400);
}
.auth-title {
  margin: 0 0 6px;
  font-size: 21px;
  font-weight: 700;
  letter-spacing: 0.3px;
  color: var(--oj-gray-900);
}
.auth-sub {
  margin: 0 0 26px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--oj-text-sub);
}

/* ---------- 桌面/移动断点 ---------- */
@media (max-width: 1024px) {
  .brand {
    padding: 40px 36px 32px;
  }
  .brand-title {
    margin-top: 30px;
  }
}
@media (max-width: 880px) {
  .auth-page {
    grid-template-columns: 1fr;
  }
  .brand {
    display: none;
  }
  .auth-logo,
  .mobile-sub {
    display: block;
  }
  .auth-title {
    margin-top: 4px;
  }
  .auth-card {
    max-width: 400px;
    padding: 34px 28px 28px;
  }
}
</style>

<!-- 表单元素由各页面插槽提供，作用域样式命中不到，故这些类放在非 scoped 块 -->
<style>
.auth-card .el-form-item {
  margin-bottom: 18px;
}
.auth-btn {
  width: 100%;
  margin-top: 6px;
  height: 44px;
  letter-spacing: 4px;
  font-weight: 600;
  box-shadow: var(--oj-shadow-brand);
}
.auth-links {
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
}
.auth-links a {
  color: var(--oj-primary);
  font-weight: 500;
}
.auth-links a:hover {
  color: var(--oj-primary-hover);
  text-decoration: underline;
}
.auth-hint {
  margin: 16px 0 0;
  padding: 10px 12px;
  border-radius: var(--oj-radius-sm);
  background: var(--oj-gray-50);
  border: 1px solid var(--oj-border);
  font-size: 12px;
  line-height: 1.7;
  color: var(--oj-text-sub);
}
@media (max-width: 880px) {
  .auth-btn {
    letter-spacing: 2px;
  }
}
</style>
