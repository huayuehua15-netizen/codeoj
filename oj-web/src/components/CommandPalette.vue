<template>
  <Teleport to="body">
    <Transition name="palette">
      <div v-if="open" class="palette-mask" @mousedown.self="close">
        <div class="palette" role="dialog" aria-modal="true" aria-label="命令面板">
          <input
            ref="inputEl"
            v-model="query"
            class="palette-input"
            type="text"
            placeholder="搜索题目 / #编号直达 / 输入任意字浏览页面…"
            @keydown.down.prevent="move(1)"
            @keydown.up.prevent="move(-1)"
            @keydown.enter.prevent="go(visibleItems[active])"
            @keydown.esc.prevent="close"
          />

          <div class="palette-group">
            <p class="palette-group-label">{{ groupLabel }}</p>
            <template v-if="visibleItems.length">
              <button
                v-for="(item, i) in visibleItems"
                :key="item.key"
                type="button"
                class="palette-item"
                :class="{ active: i === active }"
                @mouseenter="active = i"
                @click="go(item)"
              >
                <span class="palette-item-label">
                  <span v-if="item.pid" class="palette-pid">#{{ item.pid }}</span>{{ item.label }}
                </span>
                <span class="palette-item-hint">{{ item.hint }}</span>
              </button>
            </template>
            <p v-else class="palette-empty">没有匹配的题目，换个关键词试试</p>
          </div>

          <div class="palette-foot">
            <span>↑↓ 选择</span>
            <span>↵ 打开</span>
            <span>Esc 关闭</span>
            <button type="button" class="palette-close" @click="close">关闭</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
// 全局命令面板（Ctrl/⌘ + K）：页面直达 + 题库搜索，类似编辑器的 quick open。
// 列表按上下文切换：无输入 → 页面清单；有输入 → 题目结果（#编号 直接生成直达项），
// 两组不同时显示，键盘 ↑↓ 的索引空间始终唯一。
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { pageProblems } from '@/api/problem'
import { DIFFICULTY_MAP } from '@/utils/status'

interface PaletteItem {
  key: string
  label: string
  hint: string
  to?: string
  pid?: number
}

const router = useRouter()
const open = ref(false)
const query = ref('')
const inputEl = ref<HTMLInputElement>()
const active = ref(0)

const pageItems: PaletteItem[] = [
  { key: 'p-home', label: '首页', to: '/home', hint: '落地页' },
  { key: 'p-problems', label: '题库', to: '/problems', hint: '全部题目' },
  { key: 'p-submissions', label: '提交记录', to: '/submissions', hint: '登录' },
  { key: 'p-wrongbook', label: '错题本', to: '/wrongbook', hint: '登录' },
  { key: 'p-statistics', label: '统计', to: '/statistics', hint: '登录' },
  { key: 'p-leaderboard', label: '排行榜', to: '/leaderboard', hint: 'AC 榜' },
  { key: 'p-contests', label: '竞赛', to: '/contests', hint: '比赛' },
  { key: 'p-announcements', label: '公告中心', to: '/announcements', hint: '通知' },
  { key: 'p-profile', label: '个人中心', to: '/profile', hint: '登录' },
  { key: 'p-settings', label: '账号设置', to: '/settings', hint: '登录' },
]

const problems = ref<PaletteItem[]>([])
const searching = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null
let searchVersion = 0

/** 输入以 # 开头时按编号精确直达（如 #12 → /problems/12） */
const directId = computed(() => {
  const m = /^#(\d{1,6})$/.exec(query.value.trim())
  return m ? Number(m[1]) : undefined
})

const visibleItems = computed<PaletteItem[]>(() => {
  if (directId.value) {
    return [{ key: 'direct', label: `跳转到题目`, to: `/problems/${directId.value}`, pid: directId.value, hint: '编号直达' }]
  }
  if (query.value.trim()) return problems.value
  return pageItems
})

const groupLabel = computed(() => {
  if (directId.value) return '直达'
  return query.value.trim() ? '题目' : '页面'
})

function openPalette() {
  open.value = true
  query.value = ''
  active.value = 0
  problems.value = []
  void nextTick(() => inputEl.value?.focus())
}

function close() {
  open.value = false
  if (searchTimer) clearTimeout(searchTimer)
}

function move(delta: number) {
  const total = visibleItems.value.length
  if (!total) return
  active.value = (active.value + delta + total) % total
}

function go(item: PaletteItem) {
  if (!item.to) return
  close()
  router.push(item.to)
}

function searchProblems(keyword: string) {
  const version = ++searchVersion
  searching.value = true
  pageProblems({ keyword: keyword || undefined, page: 1, size: 8 })
    .then((data) => {
      if (version !== searchVersion) return
      problems.value = (data.records || []).map((r) => ({
        key: `prob-${r.id}`,
        label: r.title,
        hint: DIFFICULTY_MAP[r.difficulty]?.label ?? '',
        to: `/problems/${r.id}`,
        pid: r.id,
      }))
    })
    .catch(() => {
      // 拦截器已提示；面板保留空态
    })
    .finally(() => {
      if (version === searchVersion) searching.value = false
    })
}

watch(query, (value) => {
  active.value = 0
  if (searchTimer) clearTimeout(searchTimer)
  // #编号 直达不发起请求；页面清单也无须请求
  if (directId.value || !value.trim()) {
    problems.value = []
    return
  }
  searchTimer = setTimeout(() => searchProblems(value.trim()), 300)
})

function onGlobalKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && (e.key === 'k' || e.key === 'K')) {
    e.preventDefault()
    open.value ? close() : openPalette()
  }
}

window.addEventListener('keydown', onGlobalKeydown)
onBeforeUnmount(() => window.removeEventListener('keydown', onGlobalKeydown))

defineExpose({ openPalette })
</script>

<style scoped>
.palette-mask {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: rgba(16, 24, 40, 0.45);
  backdrop-filter: blur(2px);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 12vh 16px 0;
}
.palette {
  width: 560px;
  max-width: 100%;
  max-height: 64vh;
  overflow: hidden auto;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 12px;
  box-shadow: 0 24px 60px -12px rgba(16, 24, 40, 0.35);
}
.palette-input {
  width: 100%;
  box-sizing: border-box;
  padding: 14px 18px;
  border: 0;
  border-bottom: 1px solid var(--oj-border);
  outline: none;
  font-family: inherit;
  font-size: 14.5px;
  color: var(--oj-text);
  background: transparent;
}
.palette-input::placeholder {
  color: var(--oj-text-weak);
}
.palette-group {
  padding: 8px 8px 4px;
}
.palette-group-label {
  margin: 6px 10px 4px;
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.08em;
  color: var(--oj-text-weak);
}
.palette-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  color: var(--oj-text);
  cursor: pointer;
  text-align: left;
}
.palette-item.active,
.palette-item:hover {
  background: var(--oj-gray-100);
}
.palette-item-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.palette-pid {
  font-family: var(--oj-font-mono);
  font-size: 11px;
  color: var(--oj-text-weak);
  margin-right: 4px;
}
.palette-item-hint {
  flex-shrink: 0;
  font-family: var(--oj-font-mono);
  font-size: 9.5px;
  color: var(--oj-text-weak);
}
.palette-empty {
  margin: 4px 10px 8px;
  font-size: 12px;
  color: var(--oj-text-weak);
}
.palette-foot {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 9px 16px;
  border-top: 1px solid var(--oj-border);
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.04em;
  color: var(--oj-text-weak);
}
.palette-close {
  margin-left: auto;
  border: 0;
  background: none;
  padding: 0;
  font-family: inherit;
  font-size: 11px;
  color: var(--oj-text-weak);
  cursor: pointer;
}
.palette-close:hover {
  color: var(--oj-primary);
}

.palette-enter-active,
.palette-leave-active {
  transition: opacity 0.14s ease;
}
.palette-enter-active .palette,
.palette-leave-active .palette {
  transition: transform 0.14s ease, opacity 0.14s ease;
}
.palette-enter-from,
.palette-leave-to {
  opacity: 0;
}
.palette-enter-from .palette,
.palette-leave-to .palette {
  transform: translateY(-8px) scale(0.985);
  opacity: 0;
}
</style>
