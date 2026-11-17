(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const inputs = ['dh','substrateRate','agitation','heatFactor','tBroth','tIn','tOut','coolantFlow','hBroth','hCoolant','wallK','wallT','fouling','tubeD','tankD','pitch'];
  const defaults = Object.fromEntries(inputs.map(id => [id, $(id).value]));
  let latest = null;
  const n = id => Number($(id).value);
  const fmt = (x, digits=2) => Number.isFinite(x) ? x.toLocaleString('es-MX',{minimumFractionDigits:digits,maximumFractionDigits:digits}) : '—';
  const show = (id, value) => { $(id).innerHTML = value; };
  function calculate() {
    const v = Object.fromEntries(inputs.map(id => [id,n(id)]));
    const nonnegative = ['dh','substrateRate','agitation','coolantFlow','fouling'];
    for (const id of inputs) {
      const el=$(id), min=el.min===''?-Infinity:Number(el.min), max=el.max===''?Infinity:Number(el.max);
      if (!Number.isFinite(v[id]) || v[id]<min || v[id]>max) { el.focus(); el.setAttribute('aria-invalid','true'); toast('Revisa los valores: hay un dato fuera del rango permitido.'); return; }
      el.removeAttribute('aria-invalid');
    }
    const problems=[];
    if (v.tBroth<=v.tIn || v.tBroth<=v.tOut) problems.push('La temperatura del caldo debe ser mayor que ambas temperaturas del refrigerante.');
    if (v.tOut<=v.tIn) problems.push('La salida del refrigerante debe ser mayor que su entrada.');
    if (problems.length) { toast(problems[0]); $('metricState').textContent='Revisar datos'; $('metricStateNote').textContent='Perfil de temperatura no válido'; $('insightTitle').textContent='Condiciones no válidas'; $('insightText').textContent=problems.join(' '); return; }
    const qReaction=v.dh*v.substrateRate*v.heatFactor/3600;
    const qTotal=qReaction+v.agitation;
    const dt1=v.tBroth-v.tOut, dt2=v.tBroth-v.tIn;
    const lmtd=Math.abs(dt1-dt2)<1e-9 ? (dt1+dt2)/2 : (dt2-dt1)/Math.log(dt2/dt1);
    const doTube=v.tubeD/1000, wall=v.wallT/1000;
    const diTube=doTube-2*wall;
    if (diTube<=0) { toast('El espesor de pared no puede ser mayor que el radio del tubo.'); return; }
    // Resistances are on the outer-area basis: Do/(Di hi) + Do ln(Do/Di)/(2k) + 1/ho + Rf.
    const rInner=doTube/(diTube*v.hBroth), rWall=doTube*Math.log(doTube/diTube)/(2*v.wallK), rOuter=1/v.hCoolant;
    const u=1/(rInner+rWall+rOuter+v.fouling);
    const area=qTotal*1000/(u*lmtd);
    const length=area/(Math.PI*doTube);
    const coilD=Math.max(v.tankD-doTube,doTube);
    const turnPitch=v.pitch/1000;
    const turnLength=Math.sqrt((Math.PI*coilD)**2+turnPitch**2);
    const turns=length/turnLength;
    const height=turns*turnPitch;
    const coolantCapacity=v.coolantFlow*4180*(v.tOut-v.tIn)/1000;
    const sufficient=coolantCapacity>=qTotal;
    const status=sufficient?'Capacidad suficiente':'Revisar refrigeración';
    const note=sufficient?'Caudal de refrigerante suficiente':'Aumenta el caudal o reduce la carga';
    show('metricDuty',`${fmt(qTotal)} <small>kW</small>`); show('metricArea',`${fmt(area)} <small>m²</small>`);
    show('metricLength',`${fmt(length)} <small>m</small>`); show('metricTurns',`${fmt(turns,1)} vueltas · ${fmt(height)} m de altura`);
    $('metricState').textContent=status; $('metricStateNote').textContent=note;
    show('legendReaction',`${fmt(qReaction)} kW`); show('legendAgitation',`${fmt(v.agitation)} kW`); show('legendTotal',`${fmt(qTotal)} kW`);
    show('outU',`${fmt(u,0)} <small>W/m²·K</small>`); show('outLmtD',`${fmt(lmtd)} <small>K</small>`);
    show('outArea',`${fmt(area)} <small>m²</small>`); show('outLength',`${fmt(length)} <small>m</small>`); show('outTurns',`${fmt(turns,1)} <small>vueltas</small>`);
    const max=Math.max(qTotal,qReaction,v.agitation,0.001);
    $('barReaction').style.width=`${Math.max(2,qReaction/max*100)}%`; $('barAgitation').style.width=`${Math.max(2,v.agitation/max*100)}%`; $('barTotal').style.width='100%';
    const margin=coolantCapacity-qTotal;
    $('insightTitle').textContent=sufficient?'Capacidad térmica disponible':'Capacidad de refrigeración limitada';
    $('insightText').textContent=`Carga estimada de ${fmt(qTotal)} kW frente a una capacidad sensible del refrigerante de ${fmt(coolantCapacity)} kW (${sufficient?'margen':'déficit'} de ${fmt(Math.abs(margin))} kW). Área requerida ${fmt(area)} m²; altura helicoidal estimada ${fmt(height)} m.`;
    latest={v,qReaction,qTotal,lmtd,u,area,length,turns,height,coolantCapacity,status};
  }
  function inputsAreCalculable(){
    if(!inputs.every(id=>$(id).value.trim()!==''&&$(id).validity.valid))return false;
    const broth=n('tBroth'), inlet=n('tIn'), outlet=n('tOut');
    const outer=n('tubeD')/1000, wall=n('wallT')/1000;
    return broth>inlet&&broth>outlet&&outlet>inlet&&outer-2*wall>0;
  }
  let timer;
  function toast(message){const el=$('toast');el.textContent=message;el.classList.add('show');clearTimeout(timer);timer=setTimeout(()=>el.classList.remove('show'),3000);}
  function report(){if(!latest)calculate();if(!latest)return;const r=latest, rows=[
    ['THERMABIO — INFORME DE DISEÑO TÉRMICO',''],['Fecha local',new Date().toLocaleString('es-MX')],['Estado',r.status],['',''],['BALANCE DE ENERGÍA',''],['Calor de reacción (kW)',fmt(r.qReaction)],['Potencia de agitación (kW)',fmt(r.v.agitation)],['Carga total de enfriamiento (kW)',fmt(r.qTotal)],['Capacidad sensible del refrigerante (kW)',fmt(r.coolantCapacity)],['',''],['TRANSFERENCIA Y GEOMETRÍA',''],['Coeficiente global U (W/m²·K)',fmt(r.u,1)],['ΔT logarítmica media (K)',fmt(r.lmtd)],['Área requerida (m²)',fmt(r.area)],['Longitud de tubo (m)',fmt(r.length)],['Vueltas estimadas',fmt(r.turns,1)],['Altura helicoidal estimada (m)',fmt(r.height)],['',''],['Supuestos','Estado estacionario; caldo perfectamente mezclado a temperatura constante; cp del refrigerante = 4.18 kJ/kg·K; resistencias térmicas en serie referidas al área exterior; flujo de calor de reacción calculado con |ΔHr| ingresado por el usuario.'],['Advertencia','Estimación académica preliminar. Verifique estequiometría, correlaciones, ensuciamiento, hidráulica, materiales, presión y seguridad antes de construcción.']
    ];
    const csv='\uFEFF'+rows.map(row=>row.map(cell=>'"'+String(cell).replaceAll('"','""')+'"').join(',')).join('\r\n');
    const a=document.createElement('a');a.href=URL.createObjectURL(new Blob([csv],{type:'text/csv;charset=utf-8'}));a.download='informe-thermabio.csv';a.click();URL.revokeObjectURL(a.href);toast('Informe descargado en formato CSV.');
  }
  $('calculateBtn').addEventListener('click',calculate); $('exportBtn').addEventListener('click',report);
  $('resetBtn').addEventListener('click',()=>{for(const id of inputs)$(id).value=defaults[id];calculate();toast('Se restauraron los valores iniciales.');});
  for(const id of inputs){
    $(id).addEventListener('input',()=>{if(inputsAreCalculable())calculate();});
    $(id).addEventListener('change',()=>{if(!inputsAreCalculable())calculate();});
  }
  calculate();
})();
