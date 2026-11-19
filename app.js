(() => {
  'use strict';
  if ('serviceWorker' in navigator && (location.protocol === 'https:' || location.hostname === 'localhost')) {
    window.addEventListener('load', () => navigator.serviceWorker.register('./service-worker.js').catch(() => {}));
  }
  let installPrompt = null;
  const installButton = document.getElementById('installBtn');
  const isInstalled = window.matchMedia('(display-mode: standalone)').matches || navigator.standalone === true;
  if (installButton && isInstalled) installButton.hidden = true;
  window.addEventListener('beforeinstallprompt', event => {
    event.preventDefault(); installPrompt = event;
    if (installButton) installButton.hidden = false;
  });
  if (installButton) installButton.addEventListener('click', async () => {
    if (installPrompt) {
      installPrompt.prompt();
      await installPrompt.userChoice;
      installPrompt = null; installButton.hidden = true;
    } else if (/iPhone|iPad|iPod/i.test(navigator.userAgent) || navigator.standalone !== undefined) {
      toast('En Safari, toca Compartir y elige “Añadir a pantalla de inicio”.');
    } else {
      toast('Abre el menú del navegador y elige “Instalar aplicación” o “Añadir a pantalla principal”.');
    }
  });
  window.addEventListener('appinstalled', () => { if (installButton) installButton.hidden = true; });
  const $ = id => document.getElementById(id);
  const inputs = ['dh','substrateRate','agitation','heatFactor','tBroth','tIn','tOut','coolantFlow','cp','rho','mu','kf','hBroth','wallK','wallT','fouling','tubeD','tankD','clearance','pitch'];
  const defaults = Object.fromEntries(inputs.map(id => [id, $(id).value]));
  let latest = null, timer;
  const fmt = (x, d = 2) => Number.isFinite(x) ? x.toLocaleString('es-MX', { minimumFractionDigits: d, maximumFractionDigits: d }) : '—';
  const show = (id, html) => { $(id).innerHTML = html; };
  function toast(m) { const el = $('toast'); el.textContent = m; el.classList.add('show'); clearTimeout(timer); timer = setTimeout(() => el.classList.remove('show'), 3000); }
  function readValues() {
    const v = {};
    for (const id of inputs) {
      const el = $(id), x = Number(el.value);
      const min = el.min === '' ? -Infinity : Number(el.min), max = el.max === '' ? Infinity : Number(el.max);
      if (el.value.trim() === '' || !Number.isFinite(x) || x < min || x > max) { el.setAttribute('aria-invalid', 'true'); return { bad: el }; }
      el.removeAttribute('aria-invalid'); v[id] = x;
    }
    return { v };
  }
  function calculate(quiet) {
    latest = null;
    const { v, bad } = readValues();
    if (bad) {
      if (!quiet) { bad.focus(); toast('Revisa los valores: hay un dato fuera del rango permitido.'); }
      else { $('metricState').textContent = 'Entrada pendiente'; $('metricStateNote').textContent = 'Completa un valor válido'; $('insightTitle').textContent = 'Esperando datos válidos'; $('insightText').textContent = 'El diseño se actualizará en cuanto completes la entrada.'; }
      return;
    }
    const r = ThermaCore.design(v);
    if (r.errors) {
      if (!quiet) toast(r.errors[0]);
      $('metricState').textContent = 'Revisar datos'; $('metricStateNote').textContent = 'Datos incompatibles';
      $('insightTitle').textContent = 'Condiciones no válidas'; $('insightText').textContent = r.errors.join(' '); return;
    }
    const status = r.sufficient ? 'Caudal suficiente' : 'Revisar refrigeración';
    show('metricDuty', `${fmt(r.qTotal)} <small>kW</small>`); show('metricArea', `${fmt(r.area)} <small>m²</small>`);
    show('metricLength', `${fmt(r.length)} <small>m</small>`); show('metricTurns', `${fmt(r.turns, 1)} vueltas · ${fmt(r.height)} m de altura`);
    $('metricState').textContent = status;
    $('metricStateNote').textContent = r.sufficient ? 'Caudal disponible ≥ requerido' : 'Caudal disponible menor que el requerido';
    show('legendReaction', `${fmt(r.qReaction)} kW`); show('legendAgitation', `${fmt(v.agitation)} kW`); show('legendTotal', `${fmt(r.qTotal)} kW`);
    show('outU', `${fmt(r.u, 0)} <small>W/m²·K</small>`); show('outLmtD', `${fmt(r.lmtd)} <small>K</small>`);
    show('outArea', `${fmt(r.area)} <small>m²</small>`); show('outLength', `${fmt(r.length)} <small>m</small>`); show('outTurns', `${fmt(r.turns, 1)} <small>vueltas</small>`);
    show('outFlow', `${fmt(r.mReq, 3)} <small>kg/s</small>`); show('outRe', `${fmt(r.Re, 0)} <small>${r.regime} · ${fmt(r.vel)} m/s</small>`);
    show('outHi', `${fmt(r.hi, 0)} <small>W/m²·K</small>`); show('outDp', `${fmt(r.dP / 1000)} <small>kPa</small>`);
    const max = Math.max(r.qTotal, 0.001);
    $('barReaction').style.width = `${Math.max(2, r.qReaction / max * 100)}%`; $('barAgitation').style.width = `${Math.max(2, v.agitation / max * 100)}%`; $('barTotal').style.width = '100%';
    $('insightTitle').textContent = r.sufficient ? 'Caudal de refrigerante suficiente' : 'Caudal de refrigerante insuficiente';
    let warn = '';
    if (!r.correlationValid) warn += ' Las propiedades o el Reynolds quedan fuera del intervalo recomendado para Gnielinski (Re 3000–5×10⁶; Pr 0.5–2000); hᵢ es solo orientativo.';
    else if (r.regime !== 'turbulento') warn += ` El flujo interior es ${r.regime}; el cálculo de hᵢ en transición es aproximado.`;
    $('insightText').innerHTML = `Carga de ${fmt(r.qTotal)} kW. Se requieren ${fmt(r.mReq, 3)} kg/s de refrigerante frente a ${fmt(v.coolantFlow, 3)} kg/s disponibles (${fmt(r.flowRatio * 100, 0)} %). Área ${fmt(r.area)} m²; hélice de ${fmt(r.Dh)} m de diámetro y ${fmt(r.height)} m de altura.${warn}`;
    latest = { v, r, status };
  }
  function report() {
    calculate(false); if (!latest) return;
    const { v, r } = latest, g = x => Number(x.toPrecision(6));
    const rows = [['Parámetro', 'Valor', 'Unidad'], ['Fecha', new Date().toLocaleString('es-MX'), ''], ['Estado', latest.status, '']];
    const units = { dh: 'kJ/mol', substrateRate: 'mol/h', agitation: 'kW', heatFactor: '-', tBroth: '°C', tIn: '°C', tOut: '°C', coolantFlow: 'kg/s', cp: 'J/kg·K', rho: 'kg/m³', mu: 'mPa·s', kf: 'W/m·K', hBroth: 'W/m²·K', wallK: 'W/m·K', wallT: 'mm', fouling: 'm²·K/W', tubeD: 'mm', tankD: 'm', clearance: 'mm', pitch: 'mm' };
    for (const id of inputs) rows.push(['Entrada: ' + id, v[id], units[id]]);
    const out = [['Q reacción', r.qReaction, 'kW'], ['Q total', r.qTotal, 'kW'], ['ΔT log. media', r.lmtd, 'K'], ['Caudal requerido', r.mReq, 'kg/s'], ['Velocidad interior', r.vel, 'm/s'], ['Reynolds', r.Re, '-'], ['Prandtl', r.Pr, '-'], ['h interior', r.hi, 'W/m²·K'], ['U exterior', r.u, 'W/m²·K'], ['Área', r.area, 'm²'], ['Longitud', r.length, 'm'], ['Diámetro de hélice', r.Dh, 'm'], ['Vueltas', r.turns, '-'], ['Altura', r.height, 'm'], ['Caída de presión', r.dP, 'Pa']];
    for (const [k, x, u] of out) rows.push(['Resultado: ' + k, g(x), u]);
    rows.push(['Advertencia', 'Estimación académica preliminar; verificar correlaciones, ensuciamiento y seguridad antes de construir.', '']);
    const csv = '\uFEFF' + rows.map(row => row.map(c => '"' + String(c).replaceAll('"', '""') + '"').join(',')).join('\r\n');
    const a = document.createElement('a'); a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    a.download = 'informe-thermabio.csv'; a.click(); URL.revokeObjectURL(a.href); toast('Informe descargado en formato CSV.');
  }
  $('calculateBtn').addEventListener('click', () => calculate(false)); $('exportBtn').addEventListener('click', report);
  $('resetBtn').addEventListener('click', () => { for (const id of inputs) $(id).value = defaults[id]; calculate(false); toast('Se restauraron los valores iniciales.'); });
  for (const id of inputs) { $(id).addEventListener('input', () => calculate(true)); $(id).addEventListener('change', () => calculate(false)); }
  calculate(true);
})();
