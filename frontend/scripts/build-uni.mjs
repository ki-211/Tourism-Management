import { readFileSync, writeFileSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const manifestPath = resolve(root, 'src/manifest.json')
const original = readFileSync(manifestPath, 'utf8')
const target = process.argv[2] || 'h5'

function value(name) { return process.env[name] || '' }
function configuredManifest() {
  const manifest = JSON.parse(original)
  const amap = manifest.h5.sdkConfigs.maps.amap
  amap.key = value('VITE_AMAP_JS_KEY')
  amap.securityJsCode = value('AMAP_H5_SECURITY_JS_CODE')
  amap.serviceHost = value('AMAP_H5_SERVICE_HOST')
  return JSON.stringify(manifest, null, 2) + '\n'
}

let status = 1
try {
  writeFileSync(manifestPath, configuredManifest())
  const command = process.execPath
  const args = [resolve(root, 'node_modules', '@dcloudio', 'vite-plugin-uni', 'bin', 'uni.js'), 'build']
  if (target === 'mp-weixin') args.push('-p', 'mp-weixin')
  const env = {
    ...process.env,
    VITE_AMAP_SECURITY_CONFIGURED: String(Boolean(value('AMAP_H5_SECURITY_JS_CODE') || value('AMAP_H5_SERVICE_HOST')))
  }
  const result = spawnSync(command, args, { cwd: root, stdio: 'inherit', env })
  status = result.status ?? 1
} finally {
  writeFileSync(manifestPath, original)
}
process.exit(status)
