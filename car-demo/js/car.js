/**
 * 程序化建模的新能源轿车（无外部模型文件，所有部件可独立控制）。
 *
 * 坐标约定：车头朝 +X，车顶朝 +Y；驾驶员坐在左侧（-Z），即 fl=左前=主驾。
 *
 * 可控部件：4 个车窗、大灯/尾灯、后备箱、充电口、后视镜、氛围灯、车漆颜色、车锁（闪灯+鸣笛）。
 */
import * as THREE from 'three';
import Delaunator from '../vendor/delaunator/index.js';

// ———————————————————— 尺寸参数 ————————————————————
const W = 1.92;          // 车身宽度
const BODY_BT = 0.14;    // 车身侧面圆角（Z 方向）
const BODY_BS = 0.04;    // 车身轮廓圆角（XY 方向外扩）
const GH_HALF = 0.84;    // 玻璃舱（greenhouse）底部半宽
const GH_BT = 0.06;
const GH_BS = 0.025;
const WHEEL_R = 0.36;
const WHEEL_X = 1.45;
const WHEEL_Y = 0.36;
const WHEEL_Z = W / 2 - 0.13;
const SILL_Y = 1.0;      // 车窗下沿（腰线）——车窗玻璃在此平面以下被裁剪

/** 车身俯视收窄 + 侧面微鼓：让挤出体看起来不那么“方” */
function bodyTaper(x, y) {
  const front = Math.max(0, (x - 1.5) / 0.95);
  const rear = Math.max(0, (-x - 1.6) / 0.85);
  const plan = 1 - 0.26 * Math.pow(front, 2.4) - 0.17 * Math.pow(rear, 2.4);
  const t = THREE.MathUtils.clamp((y - 0.6) / 0.45, -1, 1);
  const bulge = 1 - 0.04 * t * t;   // 侧面中部微微鼓起，上下内收
  return plan * bulge;
}

/** 玻璃舱内倾（tumblehome）：越往上越窄 */
function cabinTaper(y) {
  const t = THREE.MathUtils.clamp((y - 0.95) / 0.5, 0, 1);
  return 1 - 0.3 * t;
}

function deform(geometry, fn) {
  const p = geometry.attributes.position;
  for (let i = 0; i < p.count; i++) {
    const x = p.getX(i), y = p.getY(i), z = p.getZ(i);
    p.setZ(i, z * fn(x, y));
  }
  p.needsUpdate = true;
  geometry.computeVertexNormals();
  return geometry;
}

function extrude(shape, halfWidth, bt, bs, segs = 6) {
  const depth = halfWidth * 2 - bt * 2;
  const g = new THREE.ExtrudeGeometry(shape, {
    depth, bevelEnabled: true, bevelThickness: bt, bevelSize: bs, bevelSegments: segs, curveSegments: 28, steps: 1,
  });
  g.translate(0, 0, -depth / 2);
  return g;
}

/**
 * “枕头式”挤出：把侧视轮廓挤出成车身，边缘圆角，且侧面用 Delaunay 网格细分，
 * 这样后续的非线性变形（俯视收窄、侧面鼓起）才能得到平滑曲面。
 * three.js 自带的 ExtrudeGeometry 侧面只有几个巨大的三角形，变形后会出现折痕。
 */
function pillowExtrude(shape, halfW, bt, bs, fn, grid = 0.05) {
  // 1) 轮廓等距重采样（逆时针）
  let outline = shape.getSpacedPoints(Math.ceil(shape.getLength() / 0.025));
  if (outline[0].distanceTo(outline[outline.length - 1]) < 1e-6) outline.pop();
  if (THREE.ShapeUtils.isClockWise(outline)) outline.reverse();
  const n = outline.length;

  const inside = (x, y) => {
    let c = false;
    for (let i = 0, j = n - 1; i < n; j = i++) {
      const a = outline[i], b = outline[j];
      if ((a.y > y) !== (b.y > y) && x < ((b.x - a.x) * (y - a.y)) / (b.y - a.y) + a.x) c = !c;
    }
    return c;
  };

  // 2) 侧面（两个盖面）：轮廓点 + 内部网格点 → Delaunay
  const pts = outline.map((p) => [p.x, p.y]);
  const box = new THREE.Box2().setFromPoints(outline);
  for (let x = box.min.x + grid / 2; x < box.max.x; x += grid) {
    for (let y = box.min.y + grid / 2; y < box.max.y; y += grid) {
      if (!inside(x, y)) continue;
      let near = false;
      for (const p of outline) if (Math.abs(p.x - x) < grid * 0.6 && Math.abs(p.y - y) < grid * 0.6) { near = true; break; }
      if (!near) pts.push([x, y]);
    }
  }
  const tri = Delaunator.from(pts).triangles;
  const capTris = [];
  for (let i = 0; i < tri.length; i += 3) {
    const [a, b, c] = [tri[i], tri[i + 1], tri[i + 2]];
    const cx = (pts[a][0] + pts[b][0] + pts[c][0]) / 3, cy = (pts[a][1] + pts[b][1] + pts[c][1]) / 3;
    if (!inside(cx, cy)) continue;
    const area = (pts[b][0] - pts[a][0]) * (pts[c][1] - pts[a][1]) - (pts[c][0] - pts[a][0]) * (pts[b][1] - pts[a][1]);
    capTris.push(area > 0 ? [a, b, c] : [a, c, b]); // 统一为逆时针
  }
  const parts = [];
  for (const side of [1, -1]) {
    const pos = [];
    for (const [x, y] of pts) pos.push(x, y, side * halfW);
    const idx = [];
    for (const [a, b, c] of capTris) side > 0 ? idx.push(a, b, c) : idx.push(a, c, b);
    parts.push({ pos, idx });
  }

  // 3) 四周圆角墙：沿轮廓 × 圆角剖面
  const S = 8;
  const prof = [];
  for (let k = 0; k <= S; k++) { const t = (k / S) * Math.PI / 2; prof.push([bs * Math.sin(t), halfW - bt * (1 - Math.cos(t))]); }
  for (let k = S; k >= 0; k--) { const t = (k / S) * Math.PI / 2; prof.push([bs * Math.sin(t), -(halfW - bt * (1 - Math.cos(t)))]); }
  const M = prof.length;
  const pos = [], idx = [];
  for (let i = 0; i < n; i++) {
    const prev = outline[(i - 1 + n) % n], next = outline[(i + 1) % n];
    const tx = next.x - prev.x, ty = next.y - prev.y;
    const len = Math.hypot(tx, ty) || 1;
    const nx = ty / len, ny = -tx / len; // 逆时针轮廓的外法线
    for (const [off, z] of prof) pos.push(outline[i].x + nx * off, outline[i].y + ny * off, z);
  }
  for (let i = 0; i < n; i++) {
    const i2 = (i + 1) % n;
    for (let j = 0; j < M - 1; j++) {
      const a = i * M + j, b = i2 * M + j, c = i2 * M + j + 1, d = i * M + j + 1;
      idx.push(a, c, b, a, d, c);
    }
  }
  parts.push({ pos, idx });

  // 4) 变形 + 法线 + 合并
  const geos = parts.map(({ pos, idx }) => {
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
    g.setIndex(idx);
    return deform(g, fn);
  });
  return mergeGeos(geos);
}

function mergeGeos(geos) {
  const pos = [], nor = [], idx = [];
  let base = 0;
  for (const g of geos) {
    for (const v of g.attributes.position.array) pos.push(v);
    for (const v of g.attributes.normal.array) nor.push(v);
    for (const i of g.index.array) idx.push(i + base);
    base += g.attributes.position.count;
  }
  const out = new THREE.BufferGeometry();
  out.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
  out.setAttribute('normal', new THREE.Float32BufferAttribute(nor, 3));
  out.setIndex(idx);
  out.computeBoundingSphere();
  return out;
}

// ———————————————————— 侧视轮廓 ————————————————————
function bodyShape() {
  const s = new THREE.Shape();
  const archR = 0.43, archY = WHEEL_Y, bottom = 0.22;
  const a = Math.asin((bottom - archY) / archR); // 轮拱与底线交点角度
  s.moveTo(-2.18, bottom);
  s.lineTo(-WHEEL_X - archR * Math.cos(a), bottom);
  s.absarc(-WHEEL_X, archY, archR, Math.PI - a, a, true);   // 后轮拱
  s.lineTo(WHEEL_X - archR * Math.cos(a), bottom);
  s.absarc(WHEEL_X, archY, archR, Math.PI - a, a, true);    // 前轮拱
  s.lineTo(2.22, bottom);
  s.quadraticCurveTo(2.37, 0.24, 2.41, 0.40);               // 前包围
  s.quadraticCurveTo(2.45, 0.56, 2.39, 0.64);               // 车鼻
  s.quadraticCurveTo(2.27, 0.75, 1.95, 0.80);
  s.quadraticCurveTo(1.45, 0.875, 0.95, 0.95);              // 引擎盖
  s.lineTo(-1.70, 0.97);                                     // 腰线
  s.lineTo(-1.74, 0.90);                                     // 后备箱凹槽（被行李箱盖盖住）
  s.lineTo(-2.22, 0.86);
  s.quadraticCurveTo(-2.37, 0.85, -2.41, 0.76);             // 车尾
  s.quadraticCurveTo(-2.45, 0.50, -2.34, 0.30);
  s.quadraticCurveTo(-2.30, 0.22, -2.18, 0.22);
  return s;
}

function trunkLidShape() {
  const s = new THREE.Shape();
  s.moveTo(-1.72, 0.92);
  s.lineTo(-1.72, 0.975);
  s.quadraticCurveTo(-2.05, 0.965, -2.33, 0.925);
  s.quadraticCurveTo(-2.42, 0.915, -2.44, 0.885);           // 小鸭尾
  s.lineTo(-2.42, 0.85);
  s.quadraticCurveTo(-2.25, 0.875, -1.76, 0.905);
  s.lineTo(-1.72, 0.92);
  return s;
}

function cabinShape() {
  const s = new THREE.Shape();
  s.moveTo(0.98, 0.93);
  s.quadraticCurveTo(0.45, 1.25, -0.08, 1.42);              // 前挡风
  s.quadraticCurveTo(-0.45, 1.47, -0.85, 1.43);             // 车顶
  s.quadraticCurveTo(-1.35, 1.30, -1.78, 0.97);             // 溜背
  s.lineTo(0.98, 0.93);
  s.holes.push(frontWindowPath(), rearWindowPath());
  return s;
}

function frontWindowPath() {
  const p = new THREE.Path();
  p.moveTo(0.66, 1.03);
  p.lineTo(-0.47, 1.03);
  p.lineTo(-0.47, 1.365);
  p.quadraticCurveTo(-0.22, 1.39, -0.03, 1.355);
  p.quadraticCurveTo(0.30, 1.20, 0.66, 1.03);
  return p;
}

function rearWindowPath() {
  const p = new THREE.Path();
  p.moveTo(-0.60, 1.03);
  p.lineTo(-1.40, 1.03);
  p.quadraticCurveTo(-1.18, 1.27, -0.88, 1.36);
  p.quadraticCurveTo(-0.72, 1.375, -0.60, 1.37);
  p.lineTo(-0.60, 1.03);
  return p;
}

// ———————————————————— 材质 ————————————————————
function makeMaterials() {
  return {
    paint: new THREE.MeshPhysicalMaterial({
      color: '#5b6470', metalness: 0.65, roughness: 0.32, clearcoat: 1, clearcoatRoughness: 0.03, envMapIntensity: 1.25,
    }),
    blackGloss: new THREE.MeshPhysicalMaterial({ color: '#07090c', metalness: 0.2, roughness: 0.06, clearcoat: 1, envMapIntensity: 1.6 }),
    blackMatte: new THREE.MeshStandardMaterial({ color: '#0b0c0e', metalness: 0.1, roughness: 0.75 }),
    glass: new THREE.MeshPhysicalMaterial({
      color: '#1a242e', metalness: 0, roughness: 0.03, transparent: true, opacity: 0.5, envMapIntensity: 2.2,
      side: THREE.DoubleSide, depthWrite: false,
    }),
    tire: new THREE.MeshStandardMaterial({ color: '#141414', roughness: 0.88, metalness: 0 }),
    rim: new THREE.MeshStandardMaterial({ color: '#c3c8cf', metalness: 1, roughness: 0.22 }),
    rimDark: new THREE.MeshStandardMaterial({ color: '#25282d', metalness: 0.9, roughness: 0.35 }),
    disc: new THREE.MeshStandardMaterial({ color: '#7a7d82', metalness: 1, roughness: 0.45 }),
    caliper: new THREE.MeshStandardMaterial({ color: '#d8462a', metalness: 0.3, roughness: 0.4 }),
    chrome: new THREE.MeshStandardMaterial({ color: '#dfe3e8', metalness: 1, roughness: 0.12 }),
    seam: new THREE.MeshBasicMaterial({ color: '#050607' }),
    leather: new THREE.MeshStandardMaterial({ color: '#2b2522', roughness: 0.6 }),
    headLamp: new THREE.MeshStandardMaterial({ color: '#ffffff', emissive: '#e8f4ff', emissiveIntensity: 1.2 }),
    tailLamp: new THREE.MeshStandardMaterial({ color: '#330000', emissive: '#ff1a1a', emissiveIntensity: 0.9 }),
    ambient: new THREE.MeshStandardMaterial({ color: '#000000', emissive: '#3aa0ff', emissiveIntensity: 0 }),
    portGlow: new THREE.MeshStandardMaterial({ color: '#002211', emissive: '#22ff88', emissiveIntensity: 0 }),
  };
}

// ———————————————————— 车轮 ————————————————————
function makeWheel(m) {
  const wheel = new THREE.Group();
  const spin = new THREE.Group(); // 只有这一层会旋转
  wheel.add(spin);

  const prof = [
    [0.25, -0.12], [0.31, -0.122], [0.345, -0.112], [0.36, -0.08], [0.362, 0], [0.36, 0.08],
    [0.345, 0.112], [0.31, 0.122], [0.25, 0.12],
  ].map(([r, y]) => new THREE.Vector2(r, y));
  const tire = new THREE.Mesh(new THREE.LatheGeometry(prof, 64), m.tire);
  tire.rotation.x = Math.PI / 2;
  tire.castShadow = true;
  spin.add(tire);

  // 轮辋外圈
  const lip = new THREE.Mesh(new THREE.TorusGeometry(0.245, 0.014, 12, 64), m.rim);
  lip.position.z = 0.1;
  spin.add(lip);
  const barrel = new THREE.Mesh(new THREE.CylinderGeometry(0.25, 0.25, 0.2, 48, 1, true), m.rimDark);
  barrel.rotation.x = Math.PI / 2;
  spin.add(barrel);
  // 底盘（挡住视线，模拟轮毂内侧）
  const back = new THREE.Mesh(new THREE.CircleGeometry(0.245, 48), m.rimDark);
  back.position.z = -0.02;
  spin.add(back);

  // 刹车盘 + 卡钳（不随车轮转）
  const disc = new THREE.Mesh(new THREE.CylinderGeometry(0.19, 0.19, 0.025, 48), m.disc);
  disc.rotation.x = Math.PI / 2;
  disc.position.z = 0.02;
  wheel.add(disc);
  const caliper = new THREE.Mesh(new THREE.BoxGeometry(0.1, 0.13, 0.06), m.caliper);
  caliper.position.set(-0.14, 0.1, 0.04);
  caliper.rotation.z = 0.6;
  wheel.add(caliper);

  // 双五辐轮毂
  const spokeGeo = new THREE.BoxGeometry(0.2, 0.034, 0.03);
  spokeGeo.translate(0.13, 0, 0);
  for (let i = 0; i < 5; i++) {
    for (const off of [-0.11, 0.11]) {
      const s = new THREE.Mesh(spokeGeo, m.rim);
      s.rotation.z = (i / 5) * Math.PI * 2 + off;
      s.position.z = 0.085;
      s.castShadow = true;
      spin.add(s);
    }
  }
  const cap = new THREE.Mesh(new THREE.CylinderGeometry(0.055, 0.06, 0.04, 32), m.rim);
  cap.rotation.x = Math.PI / 2;
  cap.position.z = 0.09;
  spin.add(cap);
  const logo = new THREE.Mesh(new THREE.CircleGeometry(0.035, 32), m.blackGloss);
  logo.position.z = 0.111;
  spin.add(logo);

  wheel.userData.spin = spin;
  return wheel;
}

// ———————————————————— 工具：在车身表面放置装饰 ————————————————————
const raycaster = new THREE.Raycaster();
function hitSurface(mesh, origin, dir) {
  raycaster.set(origin, dir.clone().normalize());
  const hit = raycaster.intersectObject(mesh, false)[0];
  if (!hit) return null;
  const n = hit.face.normal.clone().transformDirection(mesh.matrixWorld);
  return { point: hit.point, normal: n };
}

/** 沿车身表面生成一条细管（用于灯带、钣金缝、门把手） */
function surfaceTube(mesh, samples, dir, radius, material, offset = 0.004) {
  const pts = [];
  for (const o of samples) {
    const h = hitSurface(mesh, o, dir);
    if (h) pts.push(h.point.add(h.normal.multiplyScalar(offset + radius * 0.4)));
  }
  if (pts.length < 2) return null;
  const curve = new THREE.CatmullRomCurve3(pts);
  return new THREE.Mesh(new THREE.TubeGeometry(curve, Math.max(16, pts.length * 4), radius, 8, false), material);
}

function linspace(a, b, n) {
  return Array.from({ length: n }, (_, i) => a + ((b - a) * i) / (n - 1));
}

/** 以表面法线为 Z 轴、世界向上为 Y 轴构建局部坐标系 */
function frameAt(hit) {
  const z = hit.normal.clone().normalize();
  const x = new THREE.Vector3(0, 1, 0).cross(z).normalize();
  const y = z.clone().cross(x).normalize();
  const g = new THREE.Group();
  g.matrix.makeBasis(x, y, z).setPosition(hit.point);
  g.matrix.decompose(g.position, g.quaternion, g.scale);
  return g;
}

// ———————————————————— 主类 ————————————————————
export class Car extends THREE.Group {
  constructor() {
    super();
    this.m = makeMaterials();
    this.clickables = [];
    this.listeners = new Set();
    this.state = {
      windows: { fl: 0, fr: 0, rl: 0, rr: 0 },
      lights: false,
      trunk: false,
      port: false,
      locked: false,
      mirrorsFolded: false,
      ambient: false,
      ambientColor: '#3aa0ff',
      paint: '#5b6470',
    };
    // 动画当前值（向 state 平滑逼近）
    this.anim = { windows: { fl: 0, fr: 0, rl: 0, rr: 0 }, trunk: 0, port: 0, mirrors: 0, lights: 0, ambient: 0 };
    this.flash = 0;
    this.targetPaint = new THREE.Color(this.state.paint);
    this.clipPlane = new THREE.Plane(new THREE.Vector3(0, 1, 0), -SILL_Y);
    this.m.glass.clippingPlanes = [this.clipPlane];
    this.build();
  }

  // —— 部件构建 ——
  build() {
    const m = this.m;

    // 车身
    const body = new THREE.Mesh(pillowExtrude(bodyShape(), W / 2, BODY_BT, BODY_BS, bodyTaper), m.paint);
    body.castShadow = true;
    body.receiveShadow = true;
    this.add(body);
    this.body = body;
    body.updateMatrixWorld(true);

    // 下护板 / 侧裙（黑色）
    const skirt = surfaceTube(body, linspace(-0.98, 0.98, 20).map((x) => new THREE.Vector3(x, 0.27, 3)), new THREE.Vector3(0, 0, -1), 0.03, m.blackMatte, -0.005);
    const skirt2 = surfaceTube(body, linspace(-0.98, 0.98, 20).map((x) => new THREE.Vector3(x, 0.27, -3)), new THREE.Vector3(0, 0, 1), 0.03, m.blackMatte, -0.005);
    [skirt, skirt2].forEach((s) => s && this.add(s));

    // 轮眉内衬（黑色半圆筒，挡住穿透视线）
    for (const x of [-WHEEL_X, WHEEL_X]) {
      const well = new THREE.Mesh(new THREE.CylinderGeometry(0.41, 0.41, W - 0.1, 40, 1, true, -Math.PI / 2, Math.PI), m.blackMatte);
      well.material = m.blackMatte.clone();
      well.material.side = THREE.BackSide;
      well.rotation.x = Math.PI / 2;
      well.position.set(x, WHEEL_Y, 0);
      this.add(well);
    }

    // 玻璃舱（黑色高光，模拟全景天幕 + 黑化 A/B/C 柱）
    const cabin = new THREE.Mesh(deform(extrude(cabinShape(), GH_HALF, GH_BT, GH_BS, 5), (x, y) => cabinTaper(y)), m.blackGloss);
    cabin.castShadow = true;
    this.add(cabin);

    // 后备箱盖（绕前沿铰链旋转）
    const trunkPivot = new THREE.Group();
    trunkPivot.position.set(-1.72, 0.975, 0);
    const lidGeo = pillowExtrude(trunkLidShape(), W / 2, BODY_BT, 0.02, bodyTaper, 0.03);
    lidGeo.translate(1.72, -0.975, 0);
    const lid = new THREE.Mesh(lidGeo, m.paint);
    lid.castShadow = true;
    lid.userData.part = 'trunk';
    trunkPivot.add(lid);
    this.add(trunkPivot);
    this.trunkPivot = trunkPivot;
    this.clickables.push(lid);
    // 行李舱内饰（盖子打开才看得见）
    const liner = new THREE.Mesh(new THREE.BoxGeometry(0.46, 0.02, 1.3), m.blackMatte);
    liner.position.set(-1.98, 0.9, 0);
    this.add(liner);

    // 车窗玻璃（4 块，可升降）
    this.windows = {};
    const makePane = (path, side, id, travel) => {
      const shape = new THREE.Shape(path.getPoints(24));
      const g = new THREE.ShapeGeometry(shape);
      g.computeBoundingBox();
      const c = g.boundingBox.getCenter(new THREE.Vector3());
      g.translate(-c.x, -c.y, 0);
      g.scale(1.05, 1.12, 1);
      g.translate(c.x, c.y, 0);
      const p = g.attributes.position;
      for (let i = 0; i < p.count; i++) p.setZ(i, side * (GH_HALF * cabinTaper(p.getY(i)) - 0.012));
      g.computeVertexNormals();
      const pane = new THREE.Mesh(g, m.glass);
      pane.renderOrder = 2;
      pane.userData = { part: 'window', id, travel };
      this.add(pane);
      this.windows[id] = pane;
      this.clickables.push(pane);
    };
    makePane(frontWindowPath(), -1, 'fl', 0.4);
    makePane(frontWindowPath(), 1, 'fr', 0.4);
    makePane(rearWindowPath(), -1, 'rl', 0.38);
    makePane(rearWindowPath(), 1, 'rr', 0.38);

    // 车内：头枕、方向盘、氛围灯带（车窗打开后能看到）
    const headGeo = new THREE.CapsuleGeometry(0.075, 0.12, 6, 16);
    headGeo.rotateX(Math.PI / 2);
    headGeo.scale(0.75, 1.05, 1);
    for (const [x, y] of [[-0.3, 1.15], [-1.02, 1.11]]) {
      for (const z of [-0.4, 0.4]) {
        const h = new THREE.Mesh(headGeo, m.leather);
        h.position.set(x, y, z);
        this.add(h);
      }
    }
    const steering = new THREE.Mesh(new THREE.TorusGeometry(0.17, 0.02, 12, 40), m.blackMatte);
    steering.position.set(0.32, 1.03, -0.4);
    steering.rotation.set(0, Math.PI / 2, 0.45);
    this.add(steering);
    for (const z of [-0.62, 0.62]) {
      const strip = new THREE.Mesh(new THREE.BoxGeometry(1.9, 0.012, 0.012), m.ambient);
      strip.position.set(-0.4, 1.04, z);
      this.add(strip);
    }
    this.cabinLight = new THREE.PointLight('#3aa0ff', 0, 2.2, 1.5);
    this.cabinLight.position.set(-0.4, 1.15, 0);
    this.add(this.cabinLight);

    // 前贯穿式日行灯 + 大灯
    const front = (y, zs) => zs.map((z) => new THREE.Vector3(4, y, z));
    const drl = surfaceTube(body, front(0.675, linspace(-0.74, 0.74, 31)), new THREE.Vector3(-1, 0, 0), 0.011, m.headLamp);
    const lampL = surfaceTube(body, front(0.635, linspace(-0.74, -0.46, 8)), new THREE.Vector3(-1, 0, 0), 0.022, m.headLamp);
    const lampR = surfaceTube(body, front(0.635, linspace(0.46, 0.74, 8)), new THREE.Vector3(-1, 0, 0), 0.022, m.headLamp);
    [drl, lampL, lampR].forEach((o) => { if (o) { o.userData.part = 'lights'; this.add(o); this.clickables.push(o); } });
    // 前格栅（下进气口）
    const grille = surfaceTube(body, front(0.33, linspace(-0.55, 0.55, 15)), new THREE.Vector3(-1, 0, 0), 0.035, m.blackGloss, -0.01);
    if (grille) this.add(grille);

    // 贯穿式尾灯
    const back = (y, zs) => zs.map((z) => new THREE.Vector3(-4, y, z));
    const tail = surfaceTube(body, back(0.8, linspace(-0.8, 0.8, 33)), new THREE.Vector3(1, 0, 0), 0.016, m.tailLamp);
    if (tail) { tail.userData.part = 'lights'; this.add(tail); this.clickables.push(tail); }
    const diffuser = surfaceTube(body, back(0.34, linspace(-0.6, 0.6, 15)), new THREE.Vector3(1, 0, 0), 0.04, m.blackMatte, -0.01);
    if (diffuser) this.add(diffuser);

    // 钣金缝：车门、引擎盖
    const seam = (pts2d, side) => {
      const samples = pts2d.map(([x, y]) => new THREE.Vector3(x, y, side * 3));
      const t = surfaceTube(body, samples, new THREE.Vector3(0, 0, -side), 0.0035, m.seam, 0.0005);
      if (t) this.add(t);
    };
    const doorLines = [
      [[0.93, 0.3], [0.94, 0.6], [0.9, 0.85], [0.84, 0.97]],                 // 前门前缝
      [[-0.5, 0.3], [-0.5, 0.6], [-0.5, 0.97]],                              // B 柱缝
      [[-1.0, 0.3], [-1.0, 0.68], [-1.12, 0.85], [-1.32, 0.95], [-1.5, 0.98]], // 后门后缝
      [[0.93, 0.3], [0.2, 0.3], [-0.5, 0.3], [-1.0, 0.3]],                    // 门槛缝
    ];
    for (const side of [-1, 1]) doorLines.forEach((l) => seam(l, side));
    // 引擎盖缝
    const hoodSeam = surfaceTube(body, linspace(-0.7, 0.7, 15).map((z) => new THREE.Vector3(2.08, 3, z)), new THREE.Vector3(0, -1, 0), 0.0035, m.seam, 0.0005);
    if (hoodSeam) this.add(hoodSeam);

    // 隐藏式门把手
    for (const side of [-1, 1]) {
      for (const x0 of [0.28, -0.82]) {
        const h = surfaceTube(body, linspace(x0, x0 - 0.17, 5).map((x) => new THREE.Vector3(x, 0.86, side * 3)), new THREE.Vector3(0, 0, -side), 0.011, m.chrome, -0.006);
        if (h) this.add(h);
      }
    }

    // 后视镜（可折叠）
    this.mirrors = [];
    for (const side of [-1, 1]) {
      const pivot = new THREE.Group();
      pivot.position.set(0.78, 1.03, side * 0.86);
      const arm = new THREE.Mesh(new THREE.BoxGeometry(0.07, 0.03, 0.12), m.blackGloss);
      arm.position.z = side * 0.06;
      const housing = new THREE.Mesh(new THREE.SphereGeometry(1, 24, 16), m.paint);
      housing.scale.set(0.07, 0.065, 0.12);
      housing.position.set(-0.02, 0.05, side * 0.16);
      housing.castShadow = true;
      const glassFace = new THREE.Mesh(new THREE.CircleGeometry(1, 24), m.chrome);
      glassFace.scale.set(0.11, 0.055, 1);
      glassFace.rotation.y = -Math.PI / 2;
      glassFace.position.set(-0.075, 0.05, side * 0.16);
      [arm, housing, glassFace].forEach((o) => { o.userData.part = 'mirrors'; pivot.add(o); this.clickables.push(o); });
      pivot.userData.side = side;
      this.add(pivot);
      this.mirrors.push(pivot);
    }

    // 充电口（左后翼子板）
    const hit = hitSurface(body, new THREE.Vector3(-1.95, 0.78, -3), new THREE.Vector3(0, 0, 1));
    if (hit) {
      const frame = frameAt(hit);
      const socket = new THREE.Mesh(new THREE.CircleGeometry(0.045, 32), m.blackMatte);
      socket.position.z = 0.001;
      const ring = new THREE.Mesh(new THREE.RingGeometry(0.048, 0.056, 40), m.portGlow);
      ring.position.z = 0.0015;
      const flapPivot = new THREE.Group();
      flapPivot.position.x = -0.065; // 局部 -X 指向车头方向的边做铰链
      const flapShape = new THREE.Shape();
      const r = 0.02, w = 0.13, hgt = 0.11;
      flapShape.moveTo(r, -hgt / 2);
      flapShape.lineTo(w - r, -hgt / 2); flapShape.quadraticCurveTo(w, -hgt / 2, w, -hgt / 2 + r);
      flapShape.lineTo(w, hgt / 2 - r); flapShape.quadraticCurveTo(w, hgt / 2, w - r, hgt / 2);
      flapShape.lineTo(r, hgt / 2); flapShape.quadraticCurveTo(0, hgt / 2, 0, hgt / 2 - r);
      flapShape.lineTo(0, -hgt / 2 + r); flapShape.quadraticCurveTo(0, -hgt / 2, r, -hgt / 2);
      const flap = new THREE.Mesh(new THREE.ExtrudeGeometry(flapShape, { depth: 0.008, bevelEnabled: false }), m.paint);
      flap.position.z = 0.002;
      flap.userData.part = 'port';
      flapPivot.add(flap);
      frame.add(socket, ring, flapPivot);
      [socket, ring].forEach((o) => { o.userData.part = 'port'; this.clickables.push(o); });
      this.clickables.push(flap);
      this.add(frame);
      this.portFlap = flapPivot;
    }

    // 车轮
    this.wheels = [];
    for (const x of [-WHEEL_X, WHEEL_X]) {
      for (const side of [-1, 1]) {
        const w = makeWheel(m);
        w.position.set(x, WHEEL_Y, side * WHEEL_Z);
        if (side < 0) w.rotation.y = Math.PI;
        this.add(w);
        this.wheels.push(w);
      }
    }

    // 大灯照射（打开大灯时才亮）
    this.headBeams = [];
    for (const z of [-0.62, 0.62]) {
      const spot = new THREE.SpotLight('#eaf4ff', 0, 14, 0.42, 0.55, 1.2);
      spot.position.set(2.35, 0.67, z);
      spot.target.position.set(7, 0, z * 1.6);
      this.add(spot, spot.target);
      this.headBeams.push(spot);
    }
    this.tailGlow = new THREE.PointLight('#ff2020', 0, 2.5, 2);
    this.tailGlow.position.set(-2.7, 0.8, 0);
    this.add(this.tailGlow);
  }

  // —— 状态 API（UI / 语音都只调用这些方法） ——
  onChange(fn) { this.listeners.add(fn); return () => this.listeners.delete(fn); }
  emit(reason) { for (const fn of this.listeners) fn(this.state, reason); }

  setWindow(id, value) {
    if (this.state.locked && value > 0) this.setLocked(false, true);
    this.state.windows[id] = THREE.MathUtils.clamp(value, 0, 1);
    this.emit({ type: 'window', id });
  }
  toggleWindow(id) { this.setWindow(id, this.state.windows[id] > 0.05 ? 0 : 1); }
  setAllWindows(v) { for (const id of Object.keys(this.state.windows)) this.setWindow(id, v); }
  setLights(on) { this.state.lights = on; this.emit({ type: 'lights' }); }
  setTrunk(open) {
    if (this.state.locked && open) this.setLocked(false, true);
    this.state.trunk = open; this.emit({ type: 'trunk' });
  }
  setPort(open) { this.state.port = open; this.emit({ type: 'port' }); }
  setMirrors(folded) { this.state.mirrorsFolded = folded; this.emit({ type: 'mirrors' }); }
  setAmbient(on, color) {
    this.state.ambient = on;
    if (color) this.state.ambientColor = color;
    this.emit({ type: 'ambient' });
  }
  setPaint(hex) { this.state.paint = hex; this.targetPaint.set(hex); this.emit({ type: 'paint' }); }
  setLocked(locked, silent = false) {
    this.state.locked = locked;
    if (locked) {
      this.setAllWindows(0);
      this.state.trunk = false;
      this.state.mirrorsFolded = true;
    } else {
      this.state.mirrorsFolded = false;
    }
    this.flash = locked ? 2 : 1; // 锁车闪两次，解锁闪一次
    this.flashTime = 0;
    if (!silent) beep(locked ? 2 : 1);
    this.emit({ type: 'lock' });
  }

  // —— 每帧更新 ——
  update(dt) {
    const s = this.state, a = this.anim;
    const approach = (cur, target, speed) => {
      const d = target - cur;
      const step = speed * dt;
      return Math.abs(d) <= step ? target : cur + Math.sign(d) * step;
    };

    for (const id of Object.keys(this.windows)) {
      a.windows[id] = approach(a.windows[id], s.windows[id], 0.42); // 约 2.4 秒全程
      const pane = this.windows[id];
      pane.position.y = -a.windows[id] * pane.userData.travel;
    }

    a.trunk = approach(a.trunk, s.trunk ? 1 : 0, 0.6);
    this.trunkPivot.rotation.z = -easeInOut(a.trunk) * THREE.MathUtils.degToRad(58);

    a.port = approach(a.port, s.port ? 1 : 0, 1.6);
    if (this.portFlap) this.portFlap.rotation.y = -easeInOut(a.port) * THREE.MathUtils.degToRad(100);
    this.m.portGlow.emissiveIntensity = a.port * (1.6 + Math.sin(performance.now() / 300) * 0.6);

    a.mirrors = approach(a.mirrors, s.mirrorsFolded ? 1 : 0, 1.5);
    for (const p of this.mirrors) p.rotation.y = p.userData.side * easeInOut(a.mirrors) * THREE.MathUtils.degToRad(-62);

    a.lights = approach(a.lights, s.lights ? 1 : 0, 3);
    a.ambient = approach(a.ambient, s.ambient ? 1 : 0, 2);

    // 闪灯（锁车/解锁反馈）
    let flashOn = 0;
    if (this.flash > 0) {
      this.flashTime += dt;
      const period = 0.42;
      const k = Math.floor(this.flashTime / period);
      if (k >= this.flash * 2) this.flash = 0;
      else flashOn = k % 2 === 0 ? 1 : 0;
    }

    this.m.headLamp.emissiveIntensity = 1.0 + a.lights * 3.5 + flashOn * 3;
    this.m.headLamp.emissive.set(flashOn ? '#ffb340' : '#e8f4ff');
    this.m.tailLamp.emissiveIntensity = 0.9 + a.lights * 2.2 + flashOn * 3;
    this.m.tailLamp.emissive.set(flashOn ? '#ff8a1a' : '#ff1a1a');
    for (const b of this.headBeams) b.intensity = a.lights * 30;
    this.tailGlow.intensity = 0.3 + a.lights * 2.5;

    this.m.ambient.emissive.set(s.ambientColor);
    this.m.ambient.emissiveIntensity = a.ambient * 2.5;
    this.cabinLight.color.set(s.ambientColor);
    this.cabinLight.intensity = a.ambient * 1.2;

    this.m.paint.color.lerp(this.targetPaint, Math.min(1, dt * 3));
  }
}

function easeInOut(t) { return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2; }

// 锁车提示音（WebAudio 合成，无需音频文件）
let audioCtx;
export function beep(times = 1) {
  try {
    audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)();
    for (let i = 0; i < times; i++) {
      const t0 = audioCtx.currentTime + i * 0.22;
      const o = audioCtx.createOscillator();
      const g = audioCtx.createGain();
      o.type = 'sine';
      o.frequency.setValueAtTime(1046, t0);
      g.gain.setValueAtTime(0.0001, t0);
      g.gain.exponentialRampToValueAtTime(0.18, t0 + 0.02);
      g.gain.exponentialRampToValueAtTime(0.0001, t0 + 0.16);
      o.connect(g).connect(audioCtx.destination);
      o.start(t0);
      o.stop(t0 + 0.18);
    }
  } catch { /* 无音频环境时忽略 */ }
}
