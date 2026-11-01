// 运行：node car-demo/tests/nlu.test.mjs
import assert from 'node:assert/strict';
import { parseCommand, cnToNumber } from '../js/nlu.js';

const fresh = () => ({ lastWindowTargets: null, lastWindowValue: null });
const one = (t) => parseCommand(t, fresh()).actions;
let n = 0;
const ok = (name, fn) => { fn(); n++; console.log('✓', name); };

ok('中文数字', () => { assert.equal(cnToNumber('三十五'), 35); assert.equal(cnToNumber('一百'), 100); assert.equal(cnToNumber('十'), 10); });
ok('打开主驾车窗', () => assert.deepEqual(one('打开主驾车窗'), [{ type: 'window', targets: ['fl'], value: 1 }]));
ok('关闭副驾车窗', () => assert.deepEqual(one('关闭副驾驶的车窗'), [{ type: 'window', targets: ['fr'], value: 0 }]));
ok('后排车窗开一半', () => assert.deepEqual(one('后排车窗开一半'), [{ type: 'window', targets: ['rl', 'rr'], value: 0.5 }]));
ok('百分之三十', () => assert.equal(one('左后车窗打开百分之三十')[0].value, 0.3));
ok('40%', () => assert.equal(one('车窗开到40%')[0].value, 0.4));
ok('默认全部车窗', () => assert.deepEqual(one('把车窗都打开')[0].targets, ['fl', 'fr', 'rl', 'rr']));
ok('升起=关闭', () => assert.equal(one('升起主驾车窗')[0].value, 0));
ok('隐式：有点闷', () => assert.equal(one('车里有点闷')[0].value, 0.25));
ok('隐式：下雨了', () => assert.equal(one('下雨了')[0].value, 0));
ok('大灯', () => assert.deepEqual(one('打开大灯'), [{ type: 'lights', on: true }]));
ok('关灯', () => assert.deepEqual(one('关灯'), [{ type: 'lights', on: false }]));
ok('氛围灯颜色', () => assert.deepEqual(one('氛围灯换成紫色'), [{ type: 'ambient', on: true, color: 'purple' }]));
ok('后备箱', () => assert.deepEqual(one('打开后备箱'), [{ type: 'trunk', open: true }]));
ok('充电口', () => assert.deepEqual(one('关闭充电口'), [{ type: 'port', open: false }]));
ok('解锁优先于锁', () => assert.deepEqual(one('解锁'), [{ type: 'lock', locked: false }]));
ok('锁车', () => assert.deepEqual(one('锁车'), [{ type: 'lock', locked: true }]));
ok('车漆', () => assert.deepEqual(one('车身换成蓝色'), [{ type: 'paint', color: 'blue' }]));
ok('视角', () => assert.deepEqual(one('看看车尾'), [{ type: 'view', name: 'rear' }]));
ok('多意图', () => assert.deepEqual(one('打开主驾车窗然后打开大灯').map((a) => a.type), ['window', 'lights']));
ok('上下文追问', () => {
  const ctx = fresh();
  parseCommand('打开副驾车窗一半', ctx);
  const r = parseCommand('再开大一点', ctx);
  assert.deepEqual(r.actions[0].targets, ['fr']);
  assert.equal(r.actions[0].value, 0.8);
});
ok('听不懂兜底', () => assert.equal(parseCommand('今天股票怎么样', fresh()).understood, false));
ok('天窗说明', () => { const r = parseCommand('打开天窗', fresh()); assert.equal(r.actions.length, 0); assert.match(r.reply, /全景天幕/); });
console.log(`\n全部 ${n} 个用例通过`);
