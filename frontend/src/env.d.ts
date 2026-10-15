/// <reference types="vite/client" />
declare module '*.vue' { import type { DefineComponent } from 'vue'; const component: DefineComponent; export default component }
declare const wx: any

interface ImportMetaEnv {
  readonly VITE_AMAP_JS_KEY?: string
  readonly VITE_AMAP_SECURITY_CONFIGURED?: 'true' | 'false'
}
