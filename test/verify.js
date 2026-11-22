/* Verificación sin framework: node test/verify.js */
'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { design, lmtd, nusselt, darcy } = require('../core.js');
const { solve } = require('../despejes-core.js');
for (const file of ['despejes.html', 'despejes-core.js']) {
  assert.equal(
    fs.readFileSync(path.join(__dirname, '..', file), 'utf8'),
    fs.readFileSync(path.join(__dirname, '..', 'android', 'app', 'src', 'main', 'assets', file), 'utf8'),
    `${file}: la versión web y Android deben usar el mismo archivo`
  );
}

const defaults = {
  dh: 460, substrateRate: 50, agitation: 1.2, heatFactor: 1,
  tBroth: 30, tIn: 10, tOut: 18, coolantFlow: 0.25,
  cp: 4185, rho: 999, mu: 1.17, kf: 0.589, hBroth: 1200,
  wallK: 16, wallT: 2, fouling: 0.0002, tubeD: 25,
  tankD: 1.2, clearance: 100, pitch: 75
};

function close(actual, expected, label, tolerance = 1e-9) {
  assert.ok(Number.isFinite(actual), `${label}: se esperaba un valor finito, recibido ${actual}`);
  assert.ok(Math.abs(actual - expected) <= tolerance * Math.max(1, Math.abs(expected)),
    `${label}: esperado ${expected}, recibido ${actual}`);
}

// Referencia independiente, calculada con las ecuaciones documentadas y los valores predeterminados.
const reference = {
  qReaction: 6.388888888888889,
  qTotal: 7.5888888888888895,
  mReq: 0.22666932165140052,
  lmtd: 15.66092151176974,
  vel: 0.6550867055374135,
  Re: 11746.208543136237,
  Pr: 8.313157894736841,
  Nu: 98.3571380018493,
  hi: 2966.645834716437,
  u: 636.6045675686507,
  area: 0.7611865940506732,
  length: 9.691728724675883,
  turns: 3.163126882399987,
  height: 0.23723451617999902,
  dP: 2978.311979314069
};
const base = design(defaults);
assert.equal(base.errors, undefined);
for (const [key, expected] of Object.entries(reference)) close(base[key], expected, key);
assert.equal(base.regime, 'turbulento');
assert.equal(base.correlationValid, true);

// Despejes independientes, con valores de referencia calculados desde las ecuaciones mostradas.
function solveValue(target, values) { return solve(target, values).value; }
close(solveValue('duty', { dh: 460, rate: 50, factor: 1, agitation: 1.2 }), 7.5888888888888895, 'despeje Q');
close(solveValue('dutyVol', { qMet: 38, volume: 1.2, agitation: 2.9 }), 48.5, 'despeje Q base volumétrica');
close(solveValue('flow', { q: 8, cp: 4180, tin: 10, tout: 18 }), 0.23923444976076555, 'despeje caudal');
close(solveValue('lmtd', { tb: 30, tin: 10, tout: 18 }), 15.660921511769747, 'despeje LMTD');
close(solveValue('lmtd', { tb: 22, tin: 10, tout: 10 }), 12, 'despeje LMTD con diferencias iguales');
close(solveValue('u', { q: 8, area: 2, tb: 30, tin: 10, tout: 18 }), 255.41281188299527, 'despeje U');
close(solveValue('area', { q: 8, u: 700, tb: 30, tin: 10, tout: 18 }), 0.7297508910942722, 'despeje área');
close(solveValue('length', { area: 2, do: 25.4 }), 25.06377056565281, 'despeje longitud');
close(solveValue('turns', { length: 30, dh: 550, pitch: 50, do: 25.4 }), 17.35509267377625, 'despeje vueltas');
close(solveValue('height', { turns: 18, pitch: 50 }), 0.9, 'despeje altura');
close(solveValue('pressure', { mass: 0.25, length: 30, rho: 999, mu: 1.17, do: 25.4, wall: 1.65 }), 8.577330504100097, 'despeje ΔP');
assert.equal(solveValue('pressure', { mass: 0, length: 30, rho: 999, mu: 1.17, do: 25.4, wall: 1.65 }), 0);
assert.throws(() => solve('lmtd', { tb: 20, tin: 10, tout: 22 }), /T_caldo/);
assert.throws(() => solve('pressure', { mass: 0.2, length: 30, rho: 999, mu: 1.17, do: 20, wall: 10 }), /diámetro interior/);

// LMTD: igualdad exacta y casi igual sin cancelación numérica.
close(lmtd(12, 12), 12, 'LMTD igual');
close(lmtd(12, 12.000000001), 12.0000000005, 'LMTD casi igual', 1e-10);
close(lmtd(1e-300, 1e300), 1e300 / (Math.log(1e300) - Math.log(1e-300)), 'LMTD amplio rango', 1e-10);
assert.throws(() => lmtd(0, 12), RangeError);
assert.throws(() => lmtd(12, -1), RangeError);

// Correlaciones y clasificación exactamente en sus fronteras.
const mu = defaults.mu / 1000;
const Di = defaults.tubeD / 1000 - 2 * defaults.wallT / 1000;
function inputAtRe(targetRe) {
  const v = { ...defaults, agitation: 0 };
  const m = targetRe * Math.PI * Di * mu / 4;
  const qTotal = m * v.cp * (v.tOut - v.tIn) / 1000;
  v.dh = qTotal * 3600 / (v.substrateRate * v.heatFactor);
  return v;
}
for (const [target, expectedRegime] of [[2300, 'transición'], [3000, 'transición'], [4000, 'turbulento']]) {
  const result = design(inputAtRe(target));
  assert.equal(result.errors, undefined, `Re ${target}: ${result.errors?.join('; ')}`);
  close(result.Re, target, `Re frontera ${target}`, 1e-10);
  assert.equal(result.regime, expectedRegime);
  assert.ok(Number.isFinite(result.Nu));
  assert.ok(Number.isFinite(result.darcyFactor));
}
close(nusselt(2300, base.Pr), 3.66, 'Nu en Re=2300');
assert.ok(nusselt(2300.000001, base.Pr) >= 3.66, 'Nu debe continuar desde el límite laminar');
close(nusselt(3000, base.Pr), nusselt(3000.000001, base.Pr), 'Nu continuo en Re=3000', 1e-8);
close(darcy(2300), 64 / 2300, 'Darcy laminar en Re=2300');
close(darcy(4000), Math.pow(0.79 * Math.log(4000) - 1.64, -2), 'Darcy turbulento en Re=4000');
close(darcy(2300.000001), darcy(2300), 'Darcy continuo en Re=2300', 1e-6);
close(darcy(3999.999999), darcy(4000), 'Darcy continuo en Re=4000', 1e-6);
assert.ok(Number.isNaN(nusselt(0, 1)));
assert.ok(Number.isNaN(darcy(-1)));

// Casos físicos inválidos y decimal admitido.
for (const [name, change] of [
  ['Di nulo', { wallT: 12.5 }],
  ['paso menor que Do', { pitch: 24 }],
  ['hélice demasiado pequeña', { tankD: 0.12, clearance: 50 }],
  ['salto de refrigerante nulo', { tOut: 10 }],
  ['carga cero', { dh: 460, substrateRate: 0, agitation: 0 }],
  ['entrada no finita', { tBroth: Infinity }],
  ['diferencia térmica desbordada', { tBroth: 1e308, tIn: -1e308, tOut: 0 }],
  ['entrada incompleta', { kf: undefined }]
]) {
  const result = design({ ...defaults, ...change });
  assert.ok(result.errors?.length, `${name}: el modelo debía rechazar el caso`);
}
const decimalCase = design({ ...defaults, tBroth: 30.3 });
assert.equal(decimalCase.errors, undefined, decimalCase.errors?.join('; '));
assert.ok(Number.isFinite(decimalCase.lmtd));
const agitationOnly = design({ ...defaults, dh: 0 });
assert.equal(agitationOnly.errors, undefined, agitationOnly.errors?.join('; '));

// La interfaz debe limpiar valores obsoletos y retrasar la revocación del CSV.
const appSource = fs.readFileSync(path.join(__dirname, '..', 'app.js'), 'utf8');
const elements = new Map();
const inputIds = ['dh','substrateRate','agitation','heatFactor','tBroth','tIn','tOut','coolantFlow','cp','rho','mu','kf','hBroth','wallK','wallT','fouling','tubeD','tankD','clearance','pitch'];
const inputDefaults = Object.fromEntries(Object.entries(defaults).map(([key, value]) => [key, String(value)]));
for (const id of inputIds) {
  const handlers = new Map();
  elements.set(id, { id, value: inputDefaults[id], min: '', max: '', handlers,
    addEventListener(name, handler) { handlers.set(name, handler); },
    setAttribute() {}, removeAttribute() {}, focus() {} });
}
function fakeElement(id) {
  if (!elements.has(id)) {
    const handlers = new Map();
    elements.set(id, { id, innerHTML: '', textContent: '', style: {}, hidden: false,
      classList: { add() {}, remove() {} }, handlers,
      addEventListener(name, handler) { handlers.set(name, handler); },
      querySelector() { return { textContent: '' }; } });
  }
  return elements.get(id);
}
const hiSubtitle = { textContent: '' };
fakeElement('outHi').parentElement = { querySelector() { return hiSubtitle; } };
let appendedAnchor = null;
let anchorClickedAfterAppend = false;
let revoked = 0;
let deferredCallbacks = [];
const appContext = {
  ThermaCore: require('../core.js'),
  navigator: { userAgent: 'Node test' },
  window: { addEventListener() {}, matchMedia() { return { matches: false }; } },
  document: {
    getElementById: fakeElement,
    createElement(tag) {
      assert.equal(tag, 'a');
      return { style: {}, click() { anchorClickedAfterAppend = appendedAnchor === this; }, remove() { this.removed = true; } };
    },
    body: { append(anchor) { appendedAnchor = anchor; } }
  },
  URL: { createObjectURL() { return 'blob:test-report'; }, revokeObjectURL() { revoked++; } },
  Blob, Date, Number, Math, Object, String,
  setTimeout(callback) { deferredCallbacks.push(callback); return deferredCallbacks.length; },
  clearTimeout() {}
};
vm.runInNewContext(appSource, appContext, { filename: 'app.js' });
elements.get('tBroth').value = '';
elements.get('tBroth').handlers.get('input')();
assert.equal(elements.get('metricDuty').innerHTML, '— <small>kW</small>');
assert.equal(elements.get('metricArea').innerHTML, '— <small>m²</small>');
assert.equal(elements.get('metricState').textContent, 'Entrada pendiente');
elements.get('tBroth').value = '30.3';
elements.get('tBroth').handlers.get('input')();
elements.get('exportBtn').handlers.get('click')();
assert.equal(anchorClickedAfterAppend, true, 'el enlace CSV debe estar adjunto al DOM antes del click');
assert.equal(revoked, 0, 'la URL del CSV no debe revocarse durante el click');
assert.ok(deferredCallbacks.length > 0, 'la limpieza diferida debe quedar programada');
for (const callback of deferredCallbacks) callback();
assert.equal(revoked, 1, 'la URL del CSV debe revocarse después de la descarga');
assert.equal(appendedAnchor.removed, true, 'el enlace temporal debe retirarse del DOM');

// El shell de la PWA no debe fallar si falta un icono opcional.
const workerSource = fs.readFileSync(path.join(__dirname, '..', 'service-worker.js'), 'utf8');
const listeners = new Map();
const addAllCalls = [];
const putCalls = [];
const deleted = [];
let skippedWaiting = false;
let claimed = false;
let offline = false;
const cache = {
  async addAll(urls) { addAllCalls.push(urls); },
  async put(request, response) { putCalls.push([request.url, response.status]); }
};
const context = {
  URL, Request, Promise, Response,
  self: {
    location: { href: 'https://example.test/app/service-worker.js', origin: 'https://example.test' },
    addEventListener(name, handler) { listeners.set(name, handler); },
    async skipWaiting() { skippedWaiting = true; },
    clients: { async claim() { claimed = true; } }
  },
  caches: {
    async open() { return cache; },
    async match(request) {
      const url = typeof request === 'string' ? request : request.url || request.href;
      if (url.endsWith('/index.html')) return new Response('cached app shell', { status: 200 });
      return null;
    },
    async keys() { return ['thermabio-v2', 'thermabio-v3']; },
    async delete(key) { deleted.push(key); }
  },
  async fetch(request) {
    if (offline) throw new Error('offline');
    if (request.url.endsWith('icon-192.png')) throw new Error('optional icon missing');
    if (request.url.endsWith('icon-512.png')) return new Response('', { status: 404 });
    return new Response('svg', { status: 200 });
  }
};
vm.runInNewContext(workerSource, context, { filename: 'service-worker.js' });
let installPromise;
listeners.get('install')({ waitUntil(promise) { installPromise = promise; } });
installPromise.then(async () => {
  assert.equal(addAllCalls.length, 1);
  assert.ok(!addAllCalls[0].some(url => url.includes('icon-')),
    'los iconos no deben estar en el grupo de recursos críticos');
  for (const url of addAllCalls[0]) {
    const localPath = url === './' ? path.join(__dirname, '..', 'index.html') : path.join(__dirname, '..', url);
    assert.ok(fs.existsSync(localPath), `falta recurso crítico del shell: ${url}`);
  }
  assert.equal(putCalls.length, 1, 'el icono presente debe guardarse; los faltantes no bloquean install');
  assert.equal(skippedWaiting, true);

  let activatePromise;
  listeners.get('activate')({ waitUntil(promise) { activatePromise = promise; } });
  await activatePromise;
  assert.deepEqual(deleted, ['thermabio-v2', 'thermabio-v3']);
  assert.equal(claimed, true);
  offline = true;
  let offlineResponsePromise;
  listeners.get('fetch')({
    request: { method: 'GET', url: 'https://example.test/app/another-page', mode: 'navigate' },
    respondWith(promise) { offlineResponsePromise = promise; }
  });
  assert.equal(await (await offlineResponsePromise).text(), 'cached app shell');
  console.log('Verificación completa: despejes, caso independiente, fronteras, casos inválidos y shell offline.');
}).catch(error => {
  console.error(error);
  process.exitCode = 1;
});
