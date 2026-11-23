/* Verificación sin framework: node test/verify.js */
'use strict';

const assert = require('node:assert/strict');
const { design, lmtd, nusselt, darcy } = require('../core.js');
const { solve } = require('../balance-metabolico-core.js');

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

// Despejes del balance metabólico en su base volumétrica.
function solveValue(target, values) { return solve(target, values).value; }
close(solveValue('qTotal', { qMet: 38, volume: 1.2, agitation: 2.9 }), 48.5, 'balance Q total');
close(solveValue('qMet', { qTotal: 48.5, volume: 1.2, agitation: 2.9 }), 38, 'despeje q metabólico');
close(solveValue('volume', { qTotal: 48.5, qMet: 38, agitation: 2.9 }), 1.2, 'despeje volumen de caldo');
close(solveValue('agitation', { qTotal: 48.5, qMet: 38, volume: 1.2 }), 2.9, 'despeje potencia de agitación');
assert.throws(() => solve('qMet', { qTotal: 2, volume: 1.2, agitation: 2.9 }), /menor que la potencia/);
assert.throws(() => solve('volume', { qTotal: 48.5, qMet: 0, agitation: 2.9 }), /mayor que cero/);
assert.throws(() => solve('agitation', { qTotal: 20, qMet: 38, volume: 1.2 }), /potencia de agitación negativa/);
assert.throws(() => solve('qTotal', { qMet: 38, volume: 0, agitation: 2.9 }), /volumen.*mayor que cero/i);

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

console.log('Verificación matemática completada: caso independiente, balances y casos límite.');
