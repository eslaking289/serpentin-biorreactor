(function (root) {
  'use strict';

  const definitions = {
    qTotal: {
      title: 'CARGA TÉRMICA TOTAL', unit: 'kW', digits: 3, mode: 'Despeje del balance metabólico',
      formula: 'Q̇_total = q̇_met · V_caldo + P_agitación',
      inputs: [['qMet', 'Calor metabólico específico', 'kW/m³', 38], ['volume', 'Volumen de caldo', 'm³', 1.2], ['agitation', 'Potencia de agitación', 'kW', 2.9]]
    },
    qMet: {
      title: 'CALOR METABÓLICO ESPECÍFICO', unit: 'kW/m³', digits: 3, mode: 'Despeje del calor metabólico específico',
      formula: 'q̇_met = (Q̇_total − P_agitación) / V_caldo',
      inputs: [['qTotal', 'Carga térmica total conocida', 'kW', 48.5], ['volume', 'Volumen de caldo', 'm³', 1.2], ['agitation', 'Potencia de agitación', 'kW', 2.9]]
    },
    volume: {
      title: 'VOLUMEN DE CALDO', unit: 'm³', digits: 3, mode: 'Despeje del volumen de caldo',
      formula: 'V_caldo = (Q̇_total − P_agitación) / q̇_met',
      inputs: [['qTotal', 'Carga térmica total conocida', 'kW', 48.5], ['qMet', 'Calor metabólico específico', 'kW/m³', 38], ['agitation', 'Potencia de agitación', 'kW', 2.9]]
    },
    agitation: {
      title: 'POTENCIA DE AGITACIÓN', unit: 'kW', digits: 3, mode: 'Despeje de la potencia disipada al caldo',
      formula: 'P_agitación = Q̇_total − q̇_met · V_caldo',
      inputs: [['qTotal', 'Carga térmica total conocida', 'kW', 48.5], ['qMet', 'Calor metabólico específico', 'kW/m³', 38], ['volume', 'Volumen de caldo', 'm³', 1.2]]
    }
  };

  function finite(value, label) {
    if (!Number.isFinite(value)) throw new RangeError(`${label} debe ser un valor numérico finito.`);
  }
  function nonnegative(value, label) {
    finite(value, label);
    if (value < 0) throw new RangeError(`${label} no puede ser negativo.`);
  }
  function positive(value, label) {
    finite(value, label);
    if (!(value > 0)) throw new RangeError(`${label} debe ser mayor que cero.`);
  }
  function solve(target, values) {
    const definition = definitions[target];
    if (!definition) throw new RangeError('Selecciona qué variable quieres calcular.');
    for (const [key, label] of Object.entries({ qMet: 'Calor metabólico', volume: 'Volumen de caldo', agitation: 'Potencia de agitación', qTotal: 'Carga térmica' })) {
      if (Object.prototype.hasOwnProperty.call(values, key)) finite(values[key], label);
    }
    let result;
    if (target === 'qTotal') {
      nonnegative(values.qMet, 'El calor metabólico específico');
      positive(values.volume, 'El volumen de caldo');
      nonnegative(values.agitation, 'La potencia de agitación');
      const metabolic = values.qMet * values.volume;
      result = { value: metabolic + values.agitation, detail: `Carga metabólica: ${format(metabolic, 3)} kW; agitación disipada: ${format(values.agitation, 3)} kW.` };
    } else if (target === 'qMet') {
      nonnegative(values.qTotal, 'La carga térmica total');
      positive(values.volume, 'El volumen de caldo');
      nonnegative(values.agitation, 'La potencia de agitación');
      if (values.qTotal < values.agitation) throw new RangeError('La carga total no puede ser menor que la potencia de agitación.');
      result = { value: (values.qTotal - values.agitation) / values.volume, detail: `Calor metabólico total = ${format(values.qTotal - values.agitation, 3)} kW.` };
    } else if (target === 'volume') {
      nonnegative(values.qTotal, 'La carga térmica total');
      positive(values.qMet, 'El calor metabólico específico');
      nonnegative(values.agitation, 'La potencia de agitación');
      if (values.qTotal < values.agitation) throw new RangeError('La carga total no puede ser menor que la potencia de agitación.');
      result = { value: (values.qTotal - values.agitation) / values.qMet, detail: `Carga atribuida al metabolismo = ${format(values.qTotal - values.agitation, 3)} kW.` };
    } else {
      nonnegative(values.qTotal, 'La carga térmica total');
      nonnegative(values.qMet, 'El calor metabólico específico');
      positive(values.volume, 'El volumen de caldo');
      const metabolic = values.qMet * values.volume;
      if (values.qTotal < metabolic) throw new RangeError('La carga total no puede ser menor que q̇_met · V_caldo; resultaría una potencia de agitación negativa.');
      result = { value: values.qTotal - metabolic, detail: `Carga metabólica = ${format(metabolic, 3)} kW.` };
    }
    if (!Number.isFinite(result.value)) throw new RangeError('El resultado no es finito; revisa los valores y las unidades.');
    return result;
  }
  function format(value, digits = 3) {
    return Number.isFinite(value) ? value.toLocaleString('es-MX', { minimumFractionDigits: digits, maximumFractionDigits: digits }) : '—';
  }

  const api = { definitions, solve, format };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  root.ThermaMetabolicBalance = api;
})(typeof window !== 'undefined' ? window : globalThis);
