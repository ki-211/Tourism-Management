import { readFileSync, writeFileSync } from 'node:fs'
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const manifestPath = resolve(root, 'src/manifest.json')
const original = readFileSync(manifestPath, 'utf8')
const target = process.argv[2] || 'h5'
const mode = process.argv[3] || 'build'

if (!['h5', 'mp-weixin'].includes(target) || !['build', 'dev'].includes(mode)) {
  throw new Error('用法：node scripts/build-uni.mjs <h5|mp-weixin> <build|dev>')
}

function value(name) { return process.env[name] || '' }
function configuredManifest() {
  const manifest = JSON.parse(original)
  const amap = manifest.h5.sdkConfigs.maps.amap
  amap.key = value('VITE_AMAP_JS_KEY')
  amap.securityJsCode = value('AMAP_H5_SECURITY_JS_CODE')
  amap.serviceHost = value('AMAP_H5_SERVICE_HOST')
  return JSON.stringify(manifest, null, 2) + '\n'
}

let restored = false
function restoreManifest() {
  if (restored) return
  restored = true
  writeFileSync(manifestPath, original)
}

writeFileSync(manifestPath, configuredManifest())
process.on('exit', restoreManifest)

const args = [resolve(root, 'node_modules', '@dcloudio', 'vite-plugin-uni', 'bin', 'uni.js')]
if (mode === 'build') args.push('build')
if (target === 'mp-weixin') args.push('-p', 'mp-weixin')
const env = {
  ...process.env,
  VITE_AMAP_SECURITY_CONFIGURED: String(Boolean(value('AMAP_H5_SECURITY_JS_CODE') || value('AMAP_H5_SERVICE_HOST')))
}
const child = spawn(process.execPath, args, { cwd: root, stdio: 'inherit', env })
let childFailed = false
child.on('error', error => {
  childFailed = true
  console.error(error)
})
child.on('close', (code, signal) => {
  restoreManifest()
  if (signal) process.kill(process.pid, signal)
  else process.exit(childFailed ? 1 : (code ?? 1))
})

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => child.kill(signal))
}
