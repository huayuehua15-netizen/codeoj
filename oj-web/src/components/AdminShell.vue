<template>
  <div class="admin">
    <aside class="admin-rail">
      <div class="rail-head">
        <h1 class="rail-title">管理台</h1>
      </div>
      <nav class="rail-nav" aria-label="管理导航">
        <router-link
          v-for="item in NAV"
          :key="item.to"
          :to="item.to"
          class="rail-link"
          :class="{ active: isActive(item.to) }"
        >
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
      <p class="rail-note">
        工作台仅管理员可见。<br />
        修改即时生效，请谨慎操作。
      </p>
    </aside>

    <main class="admin-main">
      <header class="admin-head">
        <div class="admin-head-text">
          <h2 class="admin-title">{{ title }}</h2>
          <p v-if="description" class="admin-desc">{{ description }}</p>
        </div>
        <div v-if="$slots.actions" class="admin-actions">
          <slot name="actions" />
        </div>
      </header>
      <div class="admin-body">
        <slot />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

defineProps<{
  title: string
  section: string
  description?: string
}>()

const route = useRoute()

const NAV = [
  { to: '/admin/problems', label: '题目管理' },
  { to: '/admin/contests', label: '竞赛管理' },
  { to: '/admin/courses', label: '班级作业' },
  { to: '/admin/users', label: '用户管理' },
  { to: '/admin/queue', label: '队列监控' },
]

function isActive(to: string) {
  return route.path.startsWith(to)
}
</script>

<style scoped>
.admin {
  max-width: 1280px;
  margin: 0 auto;
  padding: 28px 32px 64px;
  display: grid;
  grid-template-columns: 172px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.admin-rail {
  position: sticky;
  top: 86px;
  padding: 14px 12px 12px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 4px;
}
.rail-head {
  padding: 0 6px 10px;
  border-bottom: 1px solid var(--oj-border);
}
.rail-title {
  margin: 4px 0 0;
  font-size: 18px;
  font-weight: 750;
  letter-spacing: -0.03em;
  color: var(--oj-text);
}
.rail-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 6px;
}
.rail-link {
  display: flex;
  align-items: center;
  padding: 7px 8px;
  border-radius: 4px;
  font-size: 11.5px;
  color: var(--oj-text-sub);
  transition: color var(--oj-duration) var(--oj-ease), background var(--oj-duration) var(--oj-ease);
}
.rail-link:hover {
  background: var(--oj-gray-50);
  color: var(--oj-text);
}
.rail-link.active {
  color: var(--oj-primary);
  font-weight: 600;
}
.rail-note {
  margin: 12px 6px 0;
  padding-top: 10px;
  border-top: 1px solid var(--oj-border);
  font-size: 10.5px;
  line-height: 1.8;
  color: var(--oj-text-weak);
}

.admin-main {
  min-width: 0;
}
.admin-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
  padding-bottom: 13px;
  border-bottom: 1px solid var(--oj-border);
}
.admin-head-text {
  min-width: 0;
}
.admin-title {
  margin: 0;
  font-size: 22px;
  font-weight: 750;
  letter-spacing: -0.03em;
  line-height: 1.3;
}
.admin-desc {
  margin: 5px 0 0;
  font-size: 11.5px;
  line-height: 1.7;
  color: var(--oj-text-sub);
}
.admin-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.admin-body {
  margin-top: 14px;
  min-height: 240px;
}

@media (max-width: 960px) {
  .admin {
    grid-template-columns: 1fr;
    padding: 22px 18px 52px;
  }
  .admin-rail {
    position: static;
    min-width: 0;
    padding: 10px;
  }
  .rail-head {
    border-bottom: 0;
    padding-bottom: 2px;
  }
  .rail-nav {
    flex-direction: row;
    flex-wrap: wrap;
    max-width: 100%;
    gap: 4px;
    margin-top: 4px;
  }
  .rail-nav::-webkit-scrollbar {
    display: none;
  }
  .rail-link {
    padding: 6px 9px;
    white-space: nowrap;
  }
  .rail-note {
    display: none;
  }
  .admin-main {
    padding-left: 0;
  }
}
</style>
