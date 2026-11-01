/**
 * 车控自然语言理解（NLU）—— 纯规则实现，无需联网、无需 API Key。
 *
 * 输入：一句中文口语（来自语音识别或文字输入）
 * 输出：{ actions: Action[], reply: string, understood: boolean }
 *
 * Action 结构：
 *   { type: 'window',  targets: ['fl','fr','rl','rr'], value: 0~1 }   // 0=关闭 1=全开
 *   { type: 'lights',  on: boolean }
 *   { type: 'trunk',   open: boolean }
 *   { type: 'port',    open: boolean }                                // 充电口
 *   { type: 'lock',    locked: boolean }
 *   { type: 'mirrors', folded: boolean }
 *   { type: 'ambient', on: boolean, color?: string }
 *   { type: 'paint',   color: string }
 *   { type: 'view',    name: 'front'|'rear'|'left'|'right'|'top'|'default' }
 *
 * 该模块不依赖浏览器，可直接用 `node car-demo/tests/nlu.test.mjs` 测试。
 */

export const WINDOW_NAMES = { fl: '主驾', fr: '副驾', rl: '左后', rr: '右后' };

export const PAINTS = {
  gray:   { name: '星空灰', hex: '#5b6470', words: ['灰', '银'] },
  white:  { name: '珍珠白', hex: '#e9ecef', words: ['白'] },
  black:  { name: '曜石黑', hex: '#111317', words: ['黑'] },
  blue:   { name: '海湾蓝', hex: '#1f5fae', words: ['蓝'] },
  red:    { name: '赤焰红', hex: '#a3161c', words: ['红'] },
  orange: { name: '熔岩橙', hex: '#d9631e', words: ['橙', '橘'] },
  green:  { name: '松石绿', hex: '#2f7d6d', words: ['绿'] },
  purple: { name: '暮光紫', hex: '#5a3d8a', words: ['紫'] },
};

export const AMBIENT_COLORS = {
  blue:   { name: '冰川蓝', hex: '#3aa0ff', words: ['蓝'] },
  purple: { name: '星云紫', hex: '#a35bff', words: ['紫'] },
  red:    { name: '激情红', hex: '#ff3b4a', words: ['红'] },
  green:  { name: '极光绿', hex: '#2fe39a', words: ['绿'] },
  orange: { name: '暖阳橙', hex: '#ff9a3a', words: ['橙', '黄', '暖'] },
  white:  { name: '月光白', hex: '#f2f5ff', words: ['白'] },
  pink:   { name: '樱花粉', hex: '#ff6fb5', words: ['粉'] },
};

const CN_DIGIT = { 零: 0, 〇: 0, 一: 1, 二: 2, 两: 2, 三: 3, 四: 4, 五: 5, 六: 6, 七: 7, 八: 8, 九: 9 };

/** 把 "三十五" / "一百" / "五十" / "35" 转成数字 */
export function cnToNumber(s) {
  if (!s) return NaN;
  if (/^\d+(\.\d+)?$/.test(s)) return Number(s);
  if (s === '百' || s === '一百') return 100;
  let total = 0;
  let cur = 0;
  for (const ch of s) {
    if (ch in CN_DIGIT) cur = CN_DIGIT[ch];
    else if (ch === '十') { total += (cur || 1) * 10; cur = 0; }
    else if (ch === '百') { total += (cur || 1) * 100; cur = 0; }
    else return NaN;
  }
  return total + cur;
}

const NUM = '[0-9零〇一二两三四五六七八九十百]+';

/** 从子句中提取开度（0~1），没有明确开度时返回 null */
function parseAmount(clause) {
  let m = clause.match(new RegExp(`百分之(${NUM})`));
  if (m) return clamp01(cnToNumber(m[1]) / 100);
  m = clause.match(/(\d+)\s*[%％]/);
  if (m) return clamp01(Number(m[1]) / 100);
  m = clause.match(new RegExp(`(${NUM})成`));
  if (m) return clamp01(cnToNumber(m[1]) / 10);
  if (/一半|半开|开半/.test(clause)) return 0.5;
  if (/一点|一些|一条缝|条缝|小缝|一丝|透气|通风|透风|微开/.test(clause)) return 0.2;
  if (/三分之一/.test(clause)) return 0.33;
  if (/三分之二/.test(clause)) return 0.66;
  if (/全开|全部打开|开到底|降到底|完全打开/.test(clause)) return 1;
  return null;
}

const clamp01 = (v) => Math.max(0, Math.min(1, v));

const RE_CLOSE = /(关|升起|升上|升高|摇上|合上|闭|收起来|上升|升)/;
const RE_OPEN = /(开|降|放下|摇下|落下|打下)/;

/** 判断子句的动作方向：true=打开 false=关闭 null=未知 */
function direction(clause) {
  // 先判关闭（“关闭” 中不含 “开”，但 “打开” 不含 “关”，所以顺序无副作用）
  if (RE_CLOSE.test(clause)) return false;
  if (RE_OPEN.test(clause)) return true;
  return null;
}

/** 解析车窗目标 */
function parseWindowTargets(clause) {
  const set = new Set();
  if (/(副驾|右前)/.test(clause)) set.add('fr');
  // “副驾驶” 里也含 “驾驶”，所以先去掉副驾再判断主驾
  if (/(主驾|驾驶|司机|左前)/.test(clause.replace(/副驾驶?/g, ''))) set.add('fl');
  if (/左后/.test(clause)) set.add('rl');
  if (/右后/.test(clause)) set.add('rr');
  if (set.size === 0) {
    if (/(前排|前面|前边|前窗|前两)/.test(clause)) { set.add('fl'); set.add('fr'); }
    if (/(后排|后面|后边|后窗|后两)/.test(clause)) { set.add('rl'); set.add('rr'); }
    if (/左(边|侧)?/.test(clause) && set.size === 0) { set.add('fl'); set.add('rl'); }
    if (/右(边|侧)?/.test(clause) && set.size === 0) { set.add('fr'); set.add('rr'); }
  }
  if (set.size === 0) ['fl', 'fr', 'rl', 'rr'].forEach((k) => set.add(k)); // 默认全部
  return [...set];
}

function describeTargets(targets) {
  if (targets.length === 4) return '全部车窗';
  const s = new Set(targets);
  if (targets.length === 2 && s.has('fl') && s.has('fr')) return '前排车窗';
  if (targets.length === 2 && s.has('rl') && s.has('rr')) return '后排车窗';
  if (targets.length === 2 && s.has('fl') && s.has('rl')) return '左侧车窗';
  if (targets.length === 2 && s.has('fr') && s.has('rr')) return '右侧车窗';
  return targets.map((t) => WINDOW_NAMES[t]).join('、') + '车窗';
}

function pickColor(clause, table) {
  for (const [key, c] of Object.entries(table)) {
    if (c.words.some((w) => clause.includes(w)) || clause.includes(c.name)) return key;
  }
  return null;
}

/** 把一句话切成多个子句：“打开车窗然后开灯” → ["打开车窗", "开灯"] */
export function splitClauses(text) {
  return text
    .replace(/[，,。.!！？?；;、\s]+/g, '|')
    .replace(/(并且|而且|然后|接着|同时|顺便|再把|再|还有|以及)/g, '|')
    .split('|')
    .map((s) => s.trim())
    .filter(Boolean);
}

/** 解析一个子句，返回 { action, reply } 或 null */
function parseClause(clause, ctx) {
  const dir = direction(clause);

  // —— 帮助
  if (/(帮助|你能做什么|能干什么|会什么|怎么用|有哪些功能)/.test(clause)) {
    return { action: null, reply: '你可以说：打开主驾车窗、车窗开一半、关闭所有车窗、打开大灯、打开后备箱、打开充电口、锁车、氛围灯换成紫色、车身换成蓝色、看看车尾。' };
  }

  // —— 天窗（本车为全景天幕）
  if (/天窗|天幕/.test(clause)) {
    return { action: null, reply: '本车配备一体式全景天幕，不支持开启哦。' };
  }

  // —— 氛围灯（要在“灯”之前判断）
  if (/氛围灯|环境灯|气氛灯|ambient/i.test(clause)) {
    const color = pickColor(clause, AMBIENT_COLORS);
    if (dir === false) return { action: { type: 'ambient', on: false }, reply: '已关闭氛围灯' };
    if (color) return { action: { type: 'ambient', on: true, color }, reply: `氛围灯已切换为${AMBIENT_COLORS[color].name}` };
    return { action: { type: 'ambient', on: true }, reply: '已打开氛围灯' };
  }

  // —— 车身颜色
  if (/(车漆|车身|颜色|换色|涂装|外观)/.test(clause) || (/(换成|改成|变成|来个|换个)/.test(clause) && /色/.test(clause))) {
    const color = pickColor(clause, PAINTS);
    if (color) return { action: { type: 'paint', color }, reply: `车身已切换为${PAINTS[color].name}` };
    return { action: null, reply: '支持的车漆有：星空灰、珍珠白、曜石黑、海湾蓝、赤焰红、熔岩橙、松石绿、暮光紫。' };
  }

  // —— 视角
  if (/(看看|看一下|看下|视角|转到|展示|瞧瞧)/.test(clause)) {
    if (/(车头|前脸|正面|前面)/.test(clause)) return { action: { type: 'view', name: 'front' }, reply: '为您展示车头' };
    if (/(车尾|尾部|后面|屁股|背面)/.test(clause)) return { action: { type: 'view', name: 'rear' }, reply: '为您展示车尾' };
    if (/(俯视|顶部|上面|鸟瞰|车顶)/.test(clause)) return { action: { type: 'view', name: 'top' }, reply: '为您切换到俯视视角' };
    if (/(左侧|左边)/.test(clause)) return { action: { type: 'view', name: 'left' }, reply: '为您展示左侧' };
    if (/(右侧|右边|侧面)/.test(clause)) return { action: { type: 'view', name: 'right' }, reply: '为您展示右侧' };
    if (/(全车|整车|默认|复位)/.test(clause)) return { action: { type: 'view', name: 'default' }, reply: '已恢复默认视角' };
  }

  // —— 车锁（先判“解锁”，因为它包含“锁”）
  if (/(解锁|开锁|解除锁)/.test(clause)) return { action: { type: 'lock', locked: false }, reply: '车辆已解锁' };
  if (/(锁车|上锁|锁门|锁上|锁一下|闭锁)/.test(clause)) return { action: { type: 'lock', locked: true }, reply: '车辆已锁定，车窗已自动关闭' };

  // —— 后视镜
  if (/(后视镜|反光镜|镜子)/.test(clause)) {
    if (/(折叠|收起|收回|合上|关)/.test(clause)) return { action: { type: 'mirrors', folded: true }, reply: '后视镜已折叠' };
    return { action: { type: 'mirrors', folded: false }, reply: '后视镜已展开' };
  }

  // —— 后备箱
  if (/(后备箱|尾门|后箱|行李箱|后盖)/.test(clause)) {
    const open = dir !== false;
    return { action: { type: 'trunk', open }, reply: open ? '后备箱已打开' : '后备箱已关闭' };
  }

  // —— 充电口
  if (/(充电口|充电盖|充电门|充电)/.test(clause)) {
    const open = dir !== false;
    return { action: { type: 'port', open }, reply: open ? '充电口已打开' : '充电口已关闭' };
  }

  // —— 车灯
  if (/(大灯|车灯|前灯|灯光|远光|近光|头灯|灯)/.test(clause)) {
    const on = dir !== false;
    return { action: { type: 'lights', on }, reply: on ? '大灯已打开' : '大灯已关闭' };
  }

  // —— 车窗
  if (/(窗|玻璃)/.test(clause)) {
    const targets = parseWindowTargets(clause);
    let value = parseAmount(clause);
    if (value === null) {
      if (dir === false) value = 0;
      else if (dir === true) value = 1;
      else return { action: null, reply: `请问要打开还是关闭${describeTargets(targets)}？` };
    } // 注意：“关一半”“关到百分之二十”都按目标开度理解
    const desc = describeTargets(targets);
    let reply;
    if (value === 0) reply = `已为您关闭${desc}`;
    else if (value === 1) reply = `已为您打开${desc}`;
    else reply = `${desc}已调整到${Math.round(value * 100)}%开度`;
    return { action: { type: 'window', targets, value }, reply };
  }

  // —— 隐式意图（体现“理解”而不只是关键词）
  if (/(闷|热|透透气|透气|通风|空气不好|憋)/.test(clause)) {
    return { action: { type: 'window', targets: ['fl', 'fr', 'rl', 'rr'], value: 0.25 }, reply: '感觉有点闷？已为您开启车窗通风' };
  }
  if (/(冷|下雨|风太大|好吵|太吵|灰尘|雾霾)/.test(clause)) {
    return { action: { type: 'window', targets: ['fl', 'fr', 'rl', 'rr'], value: 0 }, reply: '已为您关闭全部车窗' };
  }
  if (/(太黑|好黑|看不清|天黑)/.test(clause)) {
    return { action: { type: 'lights', on: true }, reply: '已为您打开大灯' };
  }

  // 省略主语的追问：“再开大一点” / “关上吧” → 沿用上一次的车窗目标
  if (ctx.lastWindowTargets && (dir !== null || parseAmount(clause) !== null) && /(一点|些|吧|上|下|大|小)/.test(clause)) {
    const targets = ctx.lastWindowTargets;
    let value = parseAmount(clause);
    if (/(大一点|大点|再开)/.test(clause)) value = Math.min(1, (ctx.lastWindowValue ?? 0) + 0.3);
    else if (/(小一点|小点)/.test(clause)) value = Math.max(0, (ctx.lastWindowValue ?? 1) - 0.3);
    else if (value === null) value = dir ? 1 : 0;
    return { action: { type: 'window', targets, value }, reply: `${describeTargets(targets)}已调整到${Math.round(value * 100)}%开度` };
  }

  return null;
}

const context = { lastWindowTargets: null, lastWindowValue: null };

/** 主入口 */
export function parseCommand(text, ctx = context) {
  const clean = String(text || '').replace(/(请|帮我|麻烦|给我|把|一下子|呀|啊|呢|哦|嘛|小车|你好)/g, '');
  const clauses = splitClauses(clean);
  const actions = [];
  const replies = [];
  for (const c of clauses) {
    const r = parseClause(c, ctx);
    if (!r) continue;
    if (r.action) {
      actions.push(r.action);
      if (r.action.type === 'window') {
        ctx.lastWindowTargets = r.action.targets;
        ctx.lastWindowValue = r.action.value;
      }
    }
    if (r.reply) replies.push(r.reply);
  }
  if (replies.length === 0) {
    return { actions: [], understood: false, reply: `抱歉，我没听懂“${text}”。你可以说：打开主驾车窗、车窗开一半、打开大灯。` };
  }
  return { actions, understood: actions.length > 0, reply: replies.join('；') };
}
