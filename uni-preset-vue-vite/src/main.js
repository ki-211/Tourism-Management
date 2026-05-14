import { createSSRApp } from 'vue';
import App from './App.vue';
import uView from 'uview-plus';
import "leaflet/dist/leaflet.css";


export function createApp() {
	const app = createSSRApp(App);
	app.use(uView);// 注册我们改造后的"过滤器"插件
	return { app };
}