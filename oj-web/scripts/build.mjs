import { mkdirSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
if (Number(process.versions.node.split('.')[0]) < 22) {
  console.error('前端构建需要 Node22+，请升级 Node 后重试。')
  process.exit(1)
}

const env = { ...process.env }
if (process.platform === 'win32') {
  // Keep esbuild's temporary files in the project, away from restricted system TEMP.
  const temporary = resolve(root, '../.tmp/oj-web-build')
  mkdirSync(temporary, { recursive: true })
  Object.assign(env, { TEMP: temporary, TMP: temporary, TMPDIR: temporary })
}

for (const [cli, ...args] of [
  ['vue-tsc/bin/vue-tsc.js', '--noEmit'],
  ['vite/bin/vite.js', 'build', ...process.argv.slice(2)],
]) {
  const result = spawnSync(process.execPath, [resolve(root, 'node_modules', cli), ...args], {
    cwd: root,
    env,
    stdio: 'inherit',
  })
  if (result.error) console.error(result.error.message)
  if (result.status !== 0) process.exit(result.status ?? 1)
}
