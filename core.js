/* Motor de cálculo de ThermaBio (sin DOM, reutilizable en navegador y Node). */
(function (root) {
  'use strict';
  const gn = (Re, Pr) => {
    const f = Math.pow(0.79 * Math.log(Re) - 1.64, -2);
    return (f / 8) * (Re - 1000) * Pr / (1 + 12.7 * Math.sqrt(f / 8) * (Math.pow(Pr, 2 / 3) - 1));
  };
  // Nu en tubo: 3.66 (laminar, T de pared const.), Gnielinski (Re >= 3000), interpolación lineal en transición.
  function nusselt(Re, Pr) {
    if (Re < 2300) return 3.66;
    if (Re >= 3000) return gn(Re, Pr);
    const w = (Re - 2300) / 700;
    return 3.66 * (1 - w) + gn(3000, Pr) * w;
  }
  const darcy = Re => Re < 2300 ? 64 / Re : Math.pow(0.79 * Math.log(Re) - 1.64, -2);
  function lmtd(d1, d2) {
    return Math.abs(d1 - d2) < 1e-9 * Math.max(d1, d2) ? (d1 + d2) / 2 : (d2 - d1) / Math.log(d2 / d1);
  }
  function design(v) {
    const errors = [];
    if (!(v.tBroth > v.tOut && v.tBroth > v.tIn)) errors.push('La temperatura del caldo debe ser mayor que la entrada y la salida del refrigerante.');
    if (!(v.tOut > v.tIn)) errors.push('La salida del refrigerante debe ser mayor que su entrada.');
    const Do = v.tubeD / 1000, t = v.wallT / 1000, Di = Do - 2 * t;
    if (Di <= 0) errors.push('El espesor de pared deja un diámetro interior nulo o negativo.');
    if (v.pitch / 1000 < Do) errors.push('El paso entre vueltas no puede ser menor que el diámetro exterior del tubo.');
    const Dh = v.tankD - 2 * v.clearance / 1000 - Do;
    if (Dh <= Do) errors.push('El diámetro de la hélice resulta demasiado pequeño: reduce la holgura o el tubo.');
    if (errors.length) return { errors };
    const qReaction = v.dh * v.substrateRate * v.heatFactor / 3600;   // kW
    const qTotal = qReaction + v.agitation;                           // kW
    if (!(qTotal > 0)) return { errors: ['La carga térmica total debe ser mayor que cero para dimensionar el serpentín.'] };
    const dTc = v.tBroth - v.tIn, dTh = v.tBroth - v.tOut;
    const lm = lmtd(dTh, dTc);
    const mReq = qTotal * 1000 / (v.cp * (v.tOut - v.tIn));           // kg/s
    const mu = v.mu / 1000;                                           // mPa·s -> Pa·s
    const vel = mReq / (v.rho * Math.PI * Di * Di / 4);
    const Re = 4 * mReq / (Math.PI * Di * mu);
    const Pr = mu * v.cp / v.kf;
    const hStraight = nusselt(Re, Pr) * v.kf / Di;
    const hi = Re >= 2300 ? hStraight * (1 + 3.5 * Di / Dh) : hStraight; // corrección por curvatura
    const ho = v.hBroth;                                              // caldo por fuera
    const rIn = Do / (Di * hi), rWall = Do * Math.log(Do / Di) / (2 * v.wallK), rOut = 1 / ho;
    const u = 1 / (rIn + rWall + rOut + v.fouling);
    const area = qTotal * 1000 / (u * lm);
    const length = area / (Math.PI * Do);
    const p = v.pitch / 1000;
    const turnLength = Math.sqrt(Math.pow(Math.PI * Dh, 2) + p * p);
    const turns = length / turnLength, height = turns * p;
    const dP = darcy(Re) * (length / Di) * v.rho * vel * vel / 2;     // Pa, tubo recto equivalente
    const flowRatio = v.coolantFlow / mReq;
    const regime = Re >= 4000 ? 'turbulento' : Re >= 2300 ? 'transición' : 'laminar';
    const correlationValid = Re >= 3000 && Re <= 5e6 && Pr >= 0.5 && Pr <= 2000;
    return { qReaction, qTotal, lmtd: lm, mReq, vel, Re, Pr, hi, ho, u, area, length, turns, height, Dh, dP,
      flowRatio, sufficient: flowRatio >= 1, regime, correlationValid };
  }
  const api = { design, nusselt, lmtd, darcy };
  if (typeof module !== 'undefined' && module.exports) module.exports = api; else root.ThermaCore = api;
})(typeof window !== 'undefined' ? window : globalThis);
