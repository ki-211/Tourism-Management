import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { RoomEnvironment } from 'three/addons/environments/RoomEnvironment.js';
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js';
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js';
import { OutputPass } from 'three/addons/postprocessing/OutputPass.js';
import { Car } from './car.js';
import { parseCommand, PAINTS, AMBIENT_COLORS, WINDOW_NAMES } from './nlu.js';
import { createVoice, speak } from './voice.js';

const $ = (s) => document.querySelector(s);
const BG = new THREE.Color('#0b0f15');

// ———————————————————— 渲染器 / 场景 ————————————————————
const canvas = $('#scene');
const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, preserveDrawingBuffer: true });
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.0;
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
renderer.localClippingEnabled = true;

const scene = new THREE.Scene();
scene.background = BG;
scene.fog = new THREE.Fog(BG, 11, 24);
const pmrem = new THREE.PMREMGenerator(renderer);
scene.environment = pmrem.fromScene(new RoomEnvironment(), 0.04).texture;
scene.environmentIntensity = 0.6;

const camera = new THREE.PerspectiveCamera(34, 1, 0.1, 100);
camera.position.set(5.6, 2.3, -6.1);

const controls = new OrbitControls(camera, canvas);
controls.target.set(0, 0.62, 0);
controls.enableDamping = true;
controls.dampingFactor = 0.08;
controls.enablePan = false;
controls.minDistance = 4;
controls.maxDistance = 13;
controls.minPolarAngle = 0.05;
controls.maxPolarAngle = THREE.MathUtils.degToRad(86);
controls.autoRotate = true;
controls.autoRotateSpeed = 0.6;

// 灯光
const key = new THREE.DirectionalLight('#ffffff', 1.8);
key.position.set(3, 8, 2);
key.castShadow = true;
key.shadow.mapSize.set(2048, 2048);
Object.assign(key.shadow.camera, { left: -4, right: 4, top: 4, bottom: -4, near: 1, far: 20 });
key.shadow.radius = 6;
key.shadow.bias = -0.0005;
scene.add(key);
const rim = new THREE.DirectionalLight('#7fb4ff', 1.2);
rim.position.set(-6, 3, -4);
scene.add(rim);
scene.add(new THREE.HemisphereLight('#c9d8ff', '#0b0f15', 0.35));

// 地面 + 展台光圈
const floor = new THREE.Mesh(
  new THREE.CircleGeometry(30, 64),
  new THREE.MeshLambertMaterial({ color: '#141a22' }),
);
floor.rotation.x = -Math.PI / 2;
floor.receiveShadow = true;
scene.add(floor);
const ringMat = new THREE.MeshBasicMaterial({ color: '#2b8cff', transparent: true, opacity: 0.55 });
const ring = new THREE.Mesh(new THREE.RingGeometry(3.25, 3.28, 128), ringMat);
ring.rotation.x = -Math.PI / 2;
ring.position.y = 0.002;
scene.add(ring);
const ring2 = new THREE.Mesh(new THREE.RingGeometry(3.45, 3.46, 128), new THREE.MeshBasicMaterial({ color: '#2b8cff', transparent: true, opacity: 0.18 }));
ring2.rotation.x = -Math.PI / 2;
ring2.position.y = 0.002;
scene.add(ring2);

// 车
const car = new Car();
scene.add(car);
car.updateMatrixWorld(true);

// 后处理：发光（让灯带、氛围灯有光晕）
const composer = new EffectComposer(renderer);
composer.addPass(new RenderPass(scene, camera));
const bloom = new UnrealBloomPass(new THREE.Vector2(1, 1), 0.5, 0.45, 1.0);
composer.addPass(bloom);
composer.addPass(new OutputPass());

function resize() {
  const w = window.innerWidth, h = window.innerHeight;
  renderer.setSize(w, h, false);
  composer.setSize(w, h);
  camera.aspect = w / h;
  // 竖屏（手机）时拉远一点，保证整车可见
  camera.fov = w < h ? 55 : 34;
  controls.maxDistance = 13 * viewScale();
  // 桌面端左侧有控制面板，把画面中心右移，让车不被遮挡
  const shift = w > 760 ? Math.min(170, w * 0.12) : 0;
  if (shift) camera.setViewOffset(w, h, -shift, 0, w, h); else camera.clearViewOffset();
  camera.updateProjectionMatrix();
}
window.addEventListener('resize', resize);
resize();

// ———————————————————— 视角切换 ————————————————————
const VIEWS = {
  default: new THREE.Vector3(5.6, 2.3, -6.1),
  front: new THREE.Vector3(7.6, 1.3, 0.001),
  rear: new THREE.Vector3(-7.6, 1.6, 0.001),
  left: new THREE.Vector3(0.001, 1.3, -8),
  right: new THREE.Vector3(0.001, 1.3, 8),
  top: new THREE.Vector3(0.4, 9, 0.001),
};
let viewTween = null;
function viewScale() { return camera.aspect < 1 ? 1.3 : 1; } // 竖屏拉远
function goView(name, instant = false) {
  if (!VIEWS[name]) return;
  const to = VIEWS[name].clone().sub(controls.target).multiplyScalar(viewScale()).add(controls.target);
  if (instant) { viewTween = null; camera.position.copy(to); controls.update(); return; }
  controls.autoRotate = false;
  syncAutoBtn();
  const t = controls.target;
  const from = new THREE.Spherical().setFromVector3(camera.position.clone().sub(t));
  const dest = new THREE.Spherical().setFromVector3(to.clone().sub(t));
  let dTheta = dest.theta - from.theta;
  if (dTheta > Math.PI) dTheta -= Math.PI * 2;
  if (dTheta < -Math.PI) dTheta += Math.PI * 2;
  viewTween = { from, dTheta, dest, t: 0 };
}
function updateViewTween(dt) {
  if (!viewTween) return;
  viewTween.t = Math.min(1, viewTween.t + dt / 1.2);
  const k = 1 - Math.pow(1 - viewTween.t, 3);
  const s = new THREE.Spherical(
    THREE.MathUtils.lerp(viewTween.from.radius, viewTween.dest.radius, k),
    THREE.MathUtils.lerp(viewTween.from.phi, viewTween.dest.phi, k),
    viewTween.from.theta + viewTween.dTheta * k,
  );
  camera.position.copy(new THREE.Vector3().setFromSpherical(s).add(controls.target));
  if (viewTween.t >= 1) viewTween = null;
}
controls.addEventListener('start', () => { viewTween = null; controls.autoRotate = false; syncAutoBtn(); });

// ———————————————————— 点击控车 ————————————————————
// 车窗的点击热区：固定在关闭位置的不可见面片（车窗降下后也能点到窗框里的空洞）
const hotMat = new THREE.MeshBasicMaterial({ color: '#4fb3ff', transparent: true, opacity: 0, depthWrite: false, side: THREE.DoubleSide });
const hotZones = [];
for (const [id, pane] of Object.entries(car.windows)) {
  const z = new THREE.Mesh(pane.geometry, hotMat.clone());
  z.userData = { part: 'window', id };
  z.renderOrder = 3;
  car.add(z);
  hotZones.push(z);
}
const pickTargets = [...car.clickables.filter((o) => o.userData.part !== 'window'), ...hotZones, car.body];
const ray = new THREE.Raycaster();
const ndc = new THREE.Vector2();

function pick(ev) {
  const r = canvas.getBoundingClientRect();
  ndc.set(((ev.clientX - r.left) / r.width) * 2 - 1, -((ev.clientY - r.top) / r.height) * 2 + 1);
  ray.setFromCamera(ndc, camera);
  const hit = ray.intersectObjects(pickTargets, false)[0];
  return hit && hit.object.userData.part ? hit.object : null;
}

const PART_LABEL = {
  trunk: () => (car.state.trunk ? '后备箱 · 点击关闭' : '后备箱 · 点击打开'),
  lights: () => (car.state.lights ? '大灯 · 点击关闭' : '大灯 · 点击打开'),
  port: () => (car.state.port ? '充电口 · 点击关闭' : '充电口 · 点击打开'),
  mirrors: () => (car.state.mirrorsFolded ? '后视镜 · 点击展开' : '后视镜 · 点击折叠'),
};
const tooltip = $('#tooltip');
let hovered = null;
canvas.addEventListener('pointermove', (ev) => {
  if (ev.buttons) { tooltip.hidden = true; return; }
  const obj = pick(ev);
  if (hovered && hovered !== obj && hovered.material === hovered.userData.hot) hovered.material.opacity = 0;
  hovered = obj;
  if (!obj) { tooltip.hidden = true; canvas.style.cursor = 'grab'; return; }
  canvas.style.cursor = 'pointer';
  let label;
  if (obj.userData.part === 'window') {
    const v = car.state.windows[obj.userData.id];
    label = `${WINDOW_NAMES[obj.userData.id]}车窗 · ${v > 0.05 ? `已开 ${Math.round(v * 100)}%，点击关闭` : '点击打开'}`;
    obj.userData.hot = obj.material;
    obj.material.opacity = 0.16;
  } else label = PART_LABEL[obj.userData.part]?.() ?? '';
  tooltip.textContent = label;
  tooltip.style.transform = `translate(${ev.clientX + 14}px, ${ev.clientY + 14}px)`;
  tooltip.hidden = false;
});
canvas.addEventListener('pointerleave', () => { tooltip.hidden = true; });

let downAt = null;
canvas.addEventListener('pointerdown', (ev) => { downAt = { x: ev.clientX, y: ev.clientY }; canvas.style.cursor = 'grabbing'; });
canvas.addEventListener('pointerup', (ev) => {
  canvas.style.cursor = 'grab';
  if (!downAt || Math.hypot(ev.clientX - downAt.x, ev.clientY - downAt.y) > 6) return; // 拖拽不算点击
  const obj = pick(ev);
  if (!obj) return;
  const { part, id } = obj.userData;
  if (part === 'window') {
    car.toggleWindow(id);
    toast(`${WINDOW_NAMES[id]}车窗${car.state.windows[id] > 0 ? '打开' : '关闭'}中…`);
  }
  if (part === 'trunk') { car.setTrunk(!car.state.trunk); toast(car.state.trunk ? '后备箱已打开' : '后备箱已关闭'); }
  if (part === 'lights') { car.setLights(!car.state.lights); toast(car.state.lights ? '大灯已打开' : '大灯已关闭'); }
  if (part === 'port') { car.setPort(!car.state.port); toast(car.state.port ? '充电口已打开' : '充电口已关闭'); }
  if (part === 'mirrors') { car.setMirrors(!car.state.mirrorsFolded); toast(car.state.mirrorsFolded ? '后视镜已折叠' : '后视镜已展开'); }
});

// ———————————————————— 控制面板 ————————————————————
const winList = $('#windows');
for (const [id, name] of Object.entries(WINDOW_NAMES)) {
  const row = document.createElement('div');
  row.className = 'win-row';
  row.innerHTML = `
    <button class="win-btn" data-win="${id}"><span class="win-name">${name}</span><span class="win-val" data-val="${id}">关闭</span></button>
    <input type="range" min="0" max="100" step="1" value="0" data-slider="${id}" aria-label="${name}车窗开度">`;
  winList.appendChild(row);
}
winList.addEventListener('click', (e) => {
  const b = e.target.closest('[data-win]');
  if (b) car.toggleWindow(b.dataset.win);
});
winList.addEventListener('input', (e) => {
  const id = e.target.dataset.slider;
  if (id) car.setWindow(id, e.target.value / 100);
});
$('#win-all-open').onclick = () => car.setAllWindows(1);
$('#win-vent').onclick = () => car.setAllWindows(0.25);
$('#win-all-close').onclick = () => car.setAllWindows(0);

const tiles = {
  lights: () => car.setLights(!car.state.lights),
  trunk: () => car.setTrunk(!car.state.trunk),
  port: () => car.setPort(!car.state.port),
  mirrors: () => car.setMirrors(!car.state.mirrorsFolded),
  lock: () => car.setLocked(!car.state.locked),
  ambient: () => car.setAmbient(!car.state.ambient),
};
document.querySelectorAll('[data-tile]').forEach((el) => { el.onclick = () => tiles[el.dataset.tile](); });

const swatch = (wrap, table, onPick) => {
  for (const [k, c] of Object.entries(table)) {
    const b = document.createElement('button');
    b.className = 'swatch';
    b.title = c.name;
    b.style.background = c.hex;
    b.dataset.key = k;
    b.onclick = () => onPick(k, c);
    wrap.appendChild(b);
  }
};
swatch($('#paints'), PAINTS, (k, c) => car.setPaint(c.hex));
swatch($('#ambients'), AMBIENT_COLORS, (k, c) => car.setAmbient(true, c.hex));

function syncUI(state) {
  for (const id of Object.keys(WINDOW_NAMES)) {
    const v = state.windows[id];
    document.querySelector(`[data-val="${id}"]`).textContent = v < 0.01 ? '关闭' : v > 0.99 ? '全开' : `${Math.round(v * 100)}%`;
    const sl = document.querySelector(`[data-slider="${id}"]`);
    if (document.activeElement !== sl) sl.value = Math.round(v * 100);
    sl.style.setProperty('--p', `${v * 100}%`);
    document.querySelector(`[data-win="${id}"]`).classList.toggle('on', v > 0.01);
  }
  const set = (k, on, label) => {
    const el = document.querySelector(`[data-tile="${k}"]`);
    el.classList.toggle('on', on);
    el.querySelector('small').textContent = label;
  };
  set('lights', state.lights, state.lights ? '已开启' : '已关闭');
  set('trunk', state.trunk, state.trunk ? '已打开' : '已关闭');
  set('port', state.port, state.port ? '已打开' : '已关闭');
  set('mirrors', state.mirrorsFolded, state.mirrorsFolded ? '已折叠' : '已展开');
  set('lock', state.locked, state.locked ? '已锁定' : '已解锁');
  set('ambient', state.ambient, state.ambient ? '已开启' : '已关闭');
  $('#lock-chip').textContent = state.locked ? '🔒 已锁车' : '🔓 已解锁';
  $('#lock-chip').classList.toggle('locked', state.locked);
  document.querySelectorAll('#paints .swatch').forEach((b) => b.classList.toggle('sel', PAINTS[b.dataset.key].hex === state.paint));
  document.querySelectorAll('#ambients .swatch').forEach((b) => b.classList.toggle('sel', state.ambient && AMBIENT_COLORS[b.dataset.key].hex === state.ambientColor));
}
car.onChange(syncUI);
syncUI(car.state);

document.querySelectorAll('[data-view]').forEach((b) => { b.onclick = () => goView(b.dataset.view); });
const autoBtn = $('#auto-rotate');
function syncAutoBtn() { autoBtn.classList.toggle('on', controls.autoRotate); }
autoBtn.onclick = () => { controls.autoRotate = !controls.autoRotate; syncAutoBtn(); };
syncAutoBtn();

let toastTimer;
function toast(msg) {
  const t = $('#toast');
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove('show'), 1800);
}

// 顶栏时间
function tick() {
  const d = new Date();
  $('#clock').textContent = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}
tick();
setInterval(tick, 10000);

// ———————————————————— 语音 / 文字指令 ————————————————————
const dialog = $('#dialog');
function bubble(text, who) {
  const el = document.createElement('div');
  el.className = `bubble ${who}`;
  el.textContent = text;
  dialog.appendChild(el);
  while (dialog.children.length > 4) dialog.firstChild.remove();
  return el;
}

function runActions(actions) {
  for (const a of actions) {
    switch (a.type) {
      case 'window': a.targets.forEach((id) => car.setWindow(id, a.value)); break;
      case 'lights': car.setLights(a.on); break;
      case 'trunk': car.setTrunk(a.open); break;
      case 'port': car.setPort(a.open); break;
      case 'lock': car.setLocked(a.locked); break;
      case 'mirrors': car.setMirrors(a.folded); break;
      case 'ambient': car.setAmbient(a.on, a.color ? AMBIENT_COLORS[a.color].hex : undefined); break;
      case 'paint': car.setPaint(PAINTS[a.color].hex); break;
      case 'view': goView(a.name); break;
      default: break;
    }
  }
  // 涉及车窗时，自动转到能看清车窗的侧面
  if (actions.some((a) => a.type === 'window') && !actions.some((a) => a.type === 'view')) {
    const t = actions.find((a) => a.type === 'window').targets;
    goView(t.every((id) => id === 'fr' || id === 'rr') ? 'right' : t.every((id) => id === 'fl' || id === 'rl') ? 'left' : 'default');
  }
  if (actions.some((a) => a.type === 'trunk' || a.type === 'port') && !actions.some((a) => a.type === 'view')) {
    goView(actions.some((a) => a.type === 'trunk') ? 'rear' : 'left');
  }
}

let pendingUser = null;
function handleCommand(text) {
  if (!text) return;
  if (pendingUser) pendingUser.textContent = text;
  else bubble(text, 'user');
  pendingUser = null;
  const r = parseCommand(text);
  console.info('[NLU]', text, '→', r);
  runActions(r.actions);
  bubble(r.reply, r.understood ? 'bot' : 'bot warn');
  speak(r.reply);
}

const mic = $('#mic');
const voice = createVoice({
  onStart: () => { mic.classList.add('listening'); $('#voice-state').textContent = '正在聆听…'; pendingUser = bubble('…', 'user interim'); },
  onInterim: (t) => { if (pendingUser) pendingUser.textContent = t; },
  onFinal: (t) => { pendingUser?.classList.remove('interim'); handleCommand(t); },
  onEnd: (got) => {
    mic.classList.remove('listening');
    $('#voice-state').textContent = '点击说话';
    if (!got && pendingUser) { pendingUser.remove(); pendingUser = null; }
  },
  onError: (msg) => { bubble(msg, 'bot warn'); },
});
if (!voice.supported) $('#voice-state').textContent = '浏览器不支持语音，请用文字';
mic.onclick = () => voice.start();
window.addEventListener('keydown', (e) => {
  if (e.code === 'Space' && !/INPUT|TEXTAREA/.test(document.activeElement.tagName)) { e.preventDefault(); voice.start(); }
});

$('#cmd-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const input = $('#cmd');
  const t = input.value.trim();
  input.value = '';
  handleCommand(t);
});
document.querySelectorAll('[data-say]').forEach((b) => { b.onclick = () => handleCommand(b.dataset.say); });
bubble('你好，我是车载助手。点击麦克风或按空格键说“打开主驾车窗”试试。', 'bot');

// 面板折叠（手机）
$('#panel-toggle').onclick = () => document.body.classList.toggle('panel-open');

// ———————————————————— 渲染循环 ————————————————————
goView('default', true);
controls.autoRotate = true;
syncAutoBtn();
const clock = new THREE.Clock();
function loop() {
  const dt = Math.min(clock.getDelta(), 0.05);
  updateViewTween(dt);
  controls.update();
  car.update(dt);
  ringMat.opacity = 0.4 + Math.sin(performance.now() / 900) * 0.15;
  composer.render();
  requestAnimationFrame(loop);
}
loop();

// 暴露给控制台调试 / 自动化测试
window.demo = { car, camera, controls, goView, handleCommand, THREE };
