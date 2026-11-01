/**
 * 语音模块：浏览器原生 Web Speech API
 *  - 语音识别（ASR）：SpeechRecognition / webkitSpeechRecognition，中文 zh-CN
 *  - 语音播报（TTS）：speechSynthesis
 *
 * 兼容性说明（重要）：
 *  - Chrome 的语音识别会把音频发到 Google 服务器，国内网络下常报 "network" 错误；
 *  - Microsoft Edge（走微软服务）与 Safari（走苹果服务）在国内通常可直接使用；
 *  - 必须在 https 或 http://localhost 下运行，否则浏览器不给麦克风权限；
 *  - 任何情况下都可以用页面上的“文字输入”走同一套 NLU，保证演示不翻车。
 */

const ERROR_TEXT = {
  'not-allowed': '麦克风权限被拒绝，请在地址栏左侧允许麦克风后重试',
  'service-not-allowed': '浏览器禁止了语音服务，请确认在 localhost 或 https 下打开',
  network: '语音识别服务连不上（Chrome 依赖 Google 服务）。建议换用 Edge 浏览器，或直接在下方输入文字指令',
  'no-speech': '没有听到声音，请靠近麦克风再说一次',
  'audio-capture': '没有检测到麦克风设备',
  aborted: '',
};

export function createVoice({ onStart, onInterim, onFinal, onEnd, onError } = {}) {
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
  const supported = !!SR;
  let rec = null;
  let listening = false;
  let gotFinal = false;

  function start() {
    if (!supported) {
      onError?.('当前浏览器不支持语音识别，请使用 Edge / Chrome / Safari，或在下方输入文字指令');
      return;
    }
    if (listening) { stop(); return; }
    window.speechSynthesis?.cancel();
    rec = new SR();
    rec.lang = 'zh-CN';
    rec.interimResults = true;
    rec.continuous = false;
    rec.maxAlternatives = 1;
    gotFinal = false;

    rec.onstart = () => { listening = true; onStart?.(); };
    rec.onresult = (e) => {
      let interim = '';
      let final = '';
      for (let i = e.resultIndex; i < e.results.length; i++) {
        const r = e.results[i];
        if (r.isFinal) final += r[0].transcript;
        else interim += r[0].transcript;
      }
      if (interim) onInterim?.(interim);
      if (final) { gotFinal = true; onFinal?.(final.trim()); }
    };
    rec.onerror = (e) => {
      const msg = ERROR_TEXT[e.error] ?? `语音识别出错：${e.error}`;
      if (msg) onError?.(msg);
    };
    rec.onend = () => { listening = false; onEnd?.(gotFinal); };
    try { rec.start(); } catch (err) { onError?.(String(err)); }
  }

  function stop() { try { rec?.stop(); } catch { /* ignore */ } }

  return { supported, start, stop, get listening() { return listening; } };
}

let zhVoice = null;
function pickVoice() {
  const voices = window.speechSynthesis?.getVoices() || [];
  zhVoice = voices.find((v) => /zh[-_]CN/i.test(v.lang) && /Xiaoxiao|Female|Ting|晓|Yaoyao|Huihui/i.test(v.name))
    || voices.find((v) => /zh[-_]CN/i.test(v.lang))
    || voices.find((v) => /^zh/i.test(v.lang))
    || null;
}
if (typeof window !== 'undefined' && window.speechSynthesis) {
  pickVoice();
  window.speechSynthesis.onvoiceschanged = pickVoice;
}

export function speak(text) {
  if (!window.speechSynthesis || !text) return;
  window.speechSynthesis.cancel();
  const u = new SpeechSynthesisUtterance(text);
  u.lang = 'zh-CN';
  u.rate = 1.08;
  if (zhVoice) u.voice = zhVoice;
  window.speechSynthesis.speak(u);
}
