/* Motor de cálculo de ThermaBio (sin DOM, reutilizable en navegador y Node). */
(function (root) {
  'use strict';

  const INPUT_KEYS = [
    'dh', 'substrateRate', 'agitation', 'heatFactor', 'tBroth', 'tIn', 'tOut',
    'coolantFlow', 'cp', 'rho', 'mu', 'kf', 'hBroth', 'wallK', 'wallT',
    'fouling', 'tubeD', 'tankD', 'clearance', 'pitch'
  ];
  const INPUT_LABELS = {
    dh: 'entalpía molar', substrateRate: 'consumo molar', agitation: 'potencia de agitación',
    heatFactor: 'factor de calor', tBroth: 'temperatura del caldo', tIn: 'temperatura de entrada',
    tOut: 'temperatura de salida', coolantFlow: 'caudal disponible', cp: 'calor específico',
    rho: 'densidad', mu: 'viscosidad', kf: 'conductividad del refrigerante', hBroth: 'coeficiente exterior',
    wallK: 'conductividad de pared', wallT: 'espesor de pared', fouling: 'resistencia de ensuciamiento',
    tubeD: 'diámetro exterior del tubo', tankD: 'diámetro del biorreactor', clearance: 'holgura radial',
    pitch: 'separación entre vueltas'
  };
  const GNIELINSKI_MIN_RE = 3000;
  const GNIELINSKI_MAX_RE = 5e6;
  const GNIELINSKI_MIN_PR = 0.5;
  const GNIELINSKI_MAX_PR = 2000;

  function gnielinskiFriction(Re) {
    return Math.pow(0.79 * Math.log(Re) - 1.64, -2);
  }

  function gnielinski(Re, Pr) {
    const f = gnielinskiFriction(Re);
    return (f / 8) * (Re - 1000) * Pr /
      (1 + 12.7 * Math.sqrt(f / 8) * (Math.pow(Pr, 2 / 3) - 1));
  }

  // Straight-tube baseline in laminar flow; blend to Gnielinski over 2300–3000.
  function nusselt(Re, Pr) {
    if (!Number.isFinite(Re) || Re <= 0 || !Number.isFinite(Pr) || Pr <= 0) return NaN;
    if (Re < 2300) return 3.66;
    if (Re >= GNIELINSKI_MIN_RE) return gnielinski(Re, Pr);
    const weight = (Re - 2300) / (GNIELINSKI_MIN_RE - 2300);
    return 3.66 * (1 - weight) + gnielinski(GNIELINSKI_MIN_RE, Pr) * weight;
  }

  // Smooth, straight-tube Darcy factor. Transition values are a labeled estimate.
  function darcy(Re) {
    if (!Number.isFinite(Re) || Re <= 0) return NaN;
    if (Re <= 2300) return 64 / Re;
    if (Re >= 4000) return gnielinskiFriction(Re);
    const weight = (Re - 2300) / (4000 - 2300);
    const laminarAtBoundary = 64 / 2300;
    const turbulentAtBoundary = gnielinskiFriction(4000);
    return laminarAtBoundary * (1 - weight) + turbulentAtBoundary * weight;
  }

  function lmtd(d1, d2) {
    if (!Number.isFinite(d1) || !Number.isFinite(d2) || d1 <= 0 || d2 <= 0) {
      throw new RangeError('Las diferencias de temperatura de LMTD deben ser positivas y finitas.');
    }
    const relativeDifference = (d2 - d1) / d1;
    if (Math.abs(relativeDifference) < 1e-8) {
      if (relativeDifference === 0) return d1;
      return d1 * relativeDifference / Math.log1p(relativeDifference);
    }
    return (d2 - d1) / (Math.log(d2) - Math.log(d1));
  }

  function design(v) {
    const errors = [];
    if (!v || typeof v !== 'object' || Array.isArray(v)) {
      return { errors: ['Proporciona un objeto con los parámetros del diseño.'] };
    }
    for (const key of INPUT_KEYS) {
      if (!Number.isFinite(v[key])) errors.push(`El parámetro ${INPUT_LABELS[key]} debe ser un número finito.`);
    }
    if (errors.length) return { errors };

    if (v.dh < 0) errors.push('La magnitud de la entalpía no puede ser negativa.');
    if (v.substrateRate < 0 || v.agitation < 0) errors.push('El consumo y la potencia de agitación no pueden ser negativos.');
    if (v.heatFactor < 0 || v.heatFactor > 1) errors.push('El factor de calor debe estar entre 0 y 1.');
    if (!(v.tBroth > v.tOut && v.tBroth > v.tIn)) errors.push('La temperatura del caldo debe ser mayor que la entrada y la salida del refrigerante.');
    if (!(v.tOut > v.tIn)) errors.push('La salida del refrigerante debe ser mayor que su entrada.');

    for (const key of ['cp', 'rho', 'mu', 'kf', 'hBroth', 'wallK', 'tubeD', 'tankD', 'pitch']) {
      if (v[key] <= 0) errors.push(`El parámetro ${INPUT_LABELS[key]} debe ser mayor que cero.`);
    }
    for (const key of ['coolantFlow', 'fouling', 'clearance', 'wallT']) {
      if (v[key] < 0) errors.push(`El parámetro ${INPUT_LABELS[key]} no puede ser negativo.`);
    }

    const Do = v.tubeD / 1000;
    const wallThickness = v.wallT / 1000;
    const Di = Do - 2 * wallThickness;
    const Dh = v.tankD - 2 * (v.clearance / 1000) - Do;
    const pitch = v.pitch / 1000;
    if (![Do, Di, Dh, pitch].every(Number.isFinite)) errors.push('Las dimensiones convertidas a metros deben ser finitas.');
    if (!(Di > 0)) errors.push('El espesor de pared deja un diámetro interior nulo o negativo.');
    if (Number.isFinite(Do) && pitch < Do) errors.push('El paso entre vueltas no puede ser menor que el diámetro exterior del tubo.');
    if (Number.isFinite(Dh) && Number.isFinite(Do) && Dh <= Do) errors.push('El diámetro de la hélice resulta demasiado pequeño: reduce la holgura o el tubo.');
    if (errors.length) return { errors };

    const dT1 = v.tBroth - v.tOut;
    const dT2 = v.tBroth - v.tIn;
    const coolantDeltaT = v.tOut - v.tIn;
    if (![dT1, dT2, coolantDeltaT].every(Number.isFinite) || dT1 <= 0 || dT2 <= 0 || coolantDeltaT <= 0) {
      return { errors: ['Las diferencias de temperatura deben ser positivas y finitas.'] };
    }

    const qReaction = v.dh * v.substrateRate * v.heatFactor / 3600; // kW
    const qTotal = qReaction + v.agitation; // kW
    if (!Number.isFinite(qTotal) || !(qTotal > 0)) {
      return { errors: ['La carga térmica total debe ser finita y mayor que cero para dimensionar el serpentín.'] };
    }

    const lm = lmtd(dT1, dT2);
    const mReq = qTotal * 1000 / (v.cp * coolantDeltaT); // kg/s
    const mu = v.mu / 1000; // mPa·s -> Pa·s
    const velocity = mReq / (v.rho * Math.PI * Di * Di / 4);
    const Re = 4 * mReq / (Math.PI * Di * mu);
    const Pr = mu * v.cp / v.kf;
    const Nu = nusselt(Re, Pr);
    const hStraight = Nu * v.kf / Di;

    const curvatureWeight = Math.max(0, Math.min(1, (Re - 2300) / 700));
    const curvatureFactor = 1 + curvatureWeight * 3.5 * Di / Dh;
    const hi = hStraight * curvatureFactor;
    const ho = v.hBroth;
    const rIn = Do / (Di * hi);
    const rWall = Do * Math.log(Do / Di) / (2 * v.wallK);
    const rOut = 1 / ho;
    const u = 1 / (rIn + rWall + rOut + v.fouling);
    const area = qTotal * 1000 / (u * lm);
    const length = area / (Math.PI * Do);
    const turnLength = Math.sqrt(Math.pow(Math.PI * Dh, 2) + pitch * pitch);
    const turns = length / turnLength;
    const height = turns * pitch;
    const darcyFactor = darcy(Re);
    const dP = darcyFactor * (length / Di) * v.rho * velocity * velocity / 2; // Pa, recto equivalente
    const flowRatio = v.coolantFlow / mReq;

    const derived = { lm, mReq, velocity, Re, Pr, Nu, hStraight, curvatureFactor, hi, u, area, length, turns, height, darcyFactor, dP, flowRatio };
    if (Object.values(derived).some(value => !Number.isFinite(value)) || Object.values(derived).some(value => value < 0)) {
      return { errors: ['Los parámetros producen un resultado no finito o físicamente inválido; revisa las unidades y dimensiones.'] };
    }

    const regime = Re >= 4000 ? 'turbulento' : Re >= 2300 ? 'transición' : 'laminar';
    const correlationValid = Re >= GNIELINSKI_MIN_RE && Re <= GNIELINSKI_MAX_RE && Pr >= GNIELINSKI_MIN_PR && Pr <= GNIELINSKI_MAX_PR;
    const modelWarnings = [];
    if (Re < 2300) modelWarnings.push('Nu = 3.66 es una referencia de tubo recto completamente desarrollado con pared a temperatura constante; no incorpora curvatura/Dean. hᵢ es incierto para la hélice.');
    if (Re >= 2300 && Re < 4000) modelWarnings.push('Flujo en transición: interpolaciones de Nu y factor Darcy son aproximaciones; la transición en hélices depende de la geometría.');
    const prOutsideGnielinskiRange = Pr < GNIELINSKI_MIN_PR || Pr > GNIELINSKI_MAX_PR;
    if ((Re >= GNIELINSKI_MIN_RE && Re > GNIELINSKI_MAX_RE) || (Re >= 2300 && prOutsideGnielinskiRange)) {
      modelWarnings.push('Fuera del intervalo orientativo de Gnielinski (Re 3000–5×10⁶; Pr 0.5–2000).');
    }
    modelWarnings.push('ΔP usa Darcy–Weisbach de tubo recto equivalente; omite el incremento por curvatura helicoidal, accesorios y pérdidas locales. No usar como selección final de bomba.');

    return {
      qReaction, qTotal, lmtd: lm, mReq, vel: velocity, Re, Pr, Nu, hStraight,
      hi, ho, u, area, length, turns, height, Dh, dP, darcyFactor, dean: Re * Math.sqrt(Di / Dh),
      curvatureFactor, flowRatio, sufficient: flowRatio >= 1, regime, correlationValid,
      nusseltMethod: Re < 2300 ? 'Nu = 3.66, baseline de tubo recto; sin corrección laminar de hélice' :
        Re < 3000 ? 'Interpolación 2300–3000 y corrección helicoidal mezclada' :
          'Gnielinski con corrección helicoidal aproximada',
      pressureDropModel: 'Darcy–Weisbach de tubo recto equivalente',
      modelWarnings
    };
  }

  const api = { design, nusselt, lmtd, darcy };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else root.ThermaCore = api;
})(typeof window !== 'undefined' ? window : globalThis);
