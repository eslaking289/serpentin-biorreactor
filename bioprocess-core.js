/* Optional biological and energy-balance calculations. No literature values are bundled. */
(function (root) {
  'use strict';

  const finite = (v, key) => {
    if (!Number.isFinite(v)) throw new RangeError(`${key} debe ser un número finito.`);
    return v;
  };
  const positive = (v, key) => {
    finite(v, key);
    if (v <= 0) throw new RangeError(`${key} debe ser mayor que cero.`);
    return v;
  };
  const optional = (v, key, nonnegative = false) => {
    if (v == null || v === '') return null;
    finite(v, key);
    if (nonnegative ? v < 0 : v <= 0) throw new RangeError(`${key} debe ser ${nonnegative ? 'no negativo' : 'mayor que cero'}.`);
    return v;
  };

  function analyzeProcess(input) {
    if (!input || typeof input !== 'object' || Array.isArray(input)) throw new TypeError('Proporciona los parámetros del proceso.');
    const x = { ...input };
    const out = { kinetics: null, oxygen: null, heat: null, agitation: null, energy: null, coolant: null, provenance: x.provenance || {} };

    const kineticKeys = ['muMaxPerH', 'substrateGPerL', 'ksGPerL', 'biomassGPerL', 'yieldXPerS', 'maintenanceGPerGXH'];
    if (kineticKeys.slice(0, 5).some(k => x[k] != null && x[k] !== '') &&
        kineticKeys.slice(0, 5).every(k => x[k] != null && x[k] !== '')) {
      const muMax = positive(x.muMaxPerH, 'μmax');
      const substrate = optional(x.substrateGPerL, 'S', true);
      const ks = positive(x.ksGPerL, 'Ks');
      const biomass = positive(x.biomassGPerL, 'X');
      const yieldXS = positive(x.yieldXPerS, 'Yx/s');
      const maintenance = optional(x.maintenanceGPerGXH ?? 0, 'ms', true);
      const mu = muMax * substrate / (ks + substrate);
      const qS = mu / yieldXS + maintenance;
      out.kinetics = { muPerH: mu, qSubstrateGPerGXH: qS, substrateRateGPerLH: qS * biomass,
        biomassRateGPerLH: mu * biomass, doublingTimeH: Math.LN2 / mu,
        model: 'Tasa inicial batch a volumen constante; fed-batch y continuo requieren sus balances propios.' };
    } else if (kineticKeys.slice(0, 5).some(k => x[k] != null && x[k] !== '')) {
      out.kinetics = { missing: kineticKeys.slice(0, 5).filter(k => x[k] == null || x[k] === '') };
    }

    if (x.qO2MolPerGXH != null || x.kLaPerH != null) {
      const biomass = positive(x.biomassGPerL, 'X');
      const qO2 = optional(x.qO2MolPerGXH, 'qO2', true);
      const OUR = qO2 == null ? null : qO2 * biomass; // mol/(L·h)
      let OTR = null;
      if (x.kLaPerH != null) {
        const kLa = optional(x.kLaPerH, 'kLa', true);
        const cStar = positive(x.cStarMolPerL, 'C*');
        const cL = optional(x.cLMolPerL, 'CL', true);
        if (cL > cStar) throw new RangeError('CL no puede superar C* en el modelo de transferencia indicado.');
        OTR = kLa * (cStar - cL); // mol/(L·h)
      }
      out.oxygen = { OURMolPerLH: OUR, OTRMolPerLH: OTR,
        oxygenMarginMolPerLH: OUR == null || OTR == null ? null : OTR - OUR,
        oxygenSufficient: OUR == null || OTR == null ? null : OTR >= OUR,
        basis: 'Concentraciones mol/L y tasas específicas mol/(g·h); confirma coherencia de unidades.' };
    }

    const volume = positive(x.volumeM3, 'V');
    const heatMethod = x.heatMethod || 'manual';
    let metabolicKW = null;
    if (heatMethod === 'oxygen') {
      const y = positive(x.heatKJPerMolO2, 'YQ/O2');
      const qO2 = optional(x.qO2MolPerGXH, 'qO2', true);
      const biomass = positive(x.biomassGPerL, 'X');
      const our = out.oxygen?.OURMolPerLH ?? (qO2 * biomass);
      metabolicKW = y * our * volume * 1000 / 3600; // L × m³ conversion: 1000 L/m³
    } else if (heatMethod === 'reaction') {
      const enthalpy = Math.abs(finite(x.reactionEnthalpyKJPerMol, 'ΔHrxn'));
      if (enthalpy === 0) throw new RangeError('ΔHrxn debe ser distinto de cero.');
      const rate = positive(x.substrateConsumptionMolPerH, 'consumo de sustrato');
      metabolicKW = enthalpy * rate / 3600;
      out.heatMethodNote = 'El consumo molar de sustrato se ingresa explícitamente; no se calcula entalpía ni estequiometría automáticamente.';
    } else if (x.manualMetabolicKW != null) {
      metabolicKW = optional(x.manualMetabolicKW, 'Qmet manual', true);
    } else if (heatMethod !== 'manual') throw new RangeError('Método de calor metabólico no reconocido.');

    let agitationKW = optional(x.agitationKW, 'P_agitación', true);
    if (x.powerNumber != null) {
      const rho = positive(x.brothDensityKgM3, 'ρ caldo');
      const speed = positive(x.impellerSpeedPerS, 'N');
      const diameter = positive(x.impellerDiameterM, 'Dimp');
      const mu = positive(x.brothViscosityPaS, 'μ caldo');
      const np = positive(x.powerNumber, 'Np');
      const reImp = rho * speed * diameter ** 2 / mu;
      agitationKW = np * rho * speed ** 3 * diameter ** 5 / 1000;
      out.agitation = { reynolds: reImp, powerKW: agitationKW,
        note: 'Np se trata como dato ingresado: depende del impulsor, tanque, Re y aireación.' };
    }

    if (x.brothCpKJPerKgK != null || x.brothConductivityWPerMK != null) {
      const rho = positive(x.brothDensityKgM3, 'ρ caldo');
      const speed = positive(x.impellerSpeedPerS, 'N');
      const diameter = positive(x.impellerDiameterM, 'Dimp');
      const viscosity = positive(x.brothViscosityPaS, 'μ caldo');
      const cp = positive(x.brothCpKJPerKgK, 'Cp caldo');
      const conductivity = positive(x.brothConductivityWPerMK, 'k caldo');
      const wallViscosity = positive(x.brothWallViscosityPaS, 'μ pared');
      const tankDiameter = positive(x.tankDiameterM, 'D tanque');
      const re = rho * speed * diameter ** 2 / viscosity;
      const pr = cp * 1000 * viscosity / conductivity;
      const nu = 0.9 * re ** 0.62 * pr ** 0.33 * (viscosity / wallViscosity) ** 0.14;
      out.brothTransfer = { reynolds: re, prandtl: pr, nusselt: nu,
        hBrothWPerM2K: nu * conductivity / tankDiameter,
        correlation: 'Doran, Bioprocess Engineering Principles, 2nd ed., capítulo 9, ecuación 9.36. Revisar aplicabilidad y condiciones.' };
    }

    if (metabolicKW != null || agitationKW != null || x.otherHeatKW != null) {
      const qOther = optional(x.otherHeatKW ?? 0, 'Qotros', true);
      const qFeed = optional(x.feedSensibleHeatKW ?? 0, 'Qalimentación', true);
      const generated = (metabolicKW || 0) + (agitationKW || 0) + qOther + qFeed;
      out.heat = { method: heatMethod, metabolicKW, agitationKW, otherKW: qOther, feedSensibleKW: qFeed, totalGeneratedKW: generated,
        warning: 'No se suman los métodos de O2 y ΔH: selecciona solo uno para evitar contar dos veces el metabolismo.' };
      if (x.coolingRemovedKW != null || x.brothMassKg != null) {
        const removed = optional(x.coolingRemovedKW ?? 0, 'Qremovido', true);
        const mass = positive(x.brothMassKg, 'M caldo');
        const cp = positive(x.brothCpKJPerKgK, 'Cp caldo');
        const feedNet = finite(x.feedOutflowSensibleKW ?? 0, 'Qfeeds/outflows');
        const dTdt = ((generated - removed + feedNet) * 1000) / (mass * cp * 1000);
        out.energy = { dTdtKPerS: dTdt, dTdtKPerH: dTdt * 3600, generatedKW: generated, removedKW: removed,
          equation: 'M Cp dT/dt = Qgenerated − Qremoved + Qfeeds/outflows' };
      }
      const duty = positive(generated, 'Qtotal');
      if (x.coolantCpKJPerKgK != null && x.coolantDeltaTK != null) {
        const cp = positive(x.coolantCpKJPerKgK, 'Cp refrigerante');
        const dT = positive(x.coolantDeltaTK, 'ΔT refrigerante');
        const massFlow = duty / (cp * dT);
        const density = optional(x.coolantDensityKgM3, 'ρ refrigerante');
        out.coolant = { massFlowKgS: massFlow, volumetricFlowM3S: density == null ? null : massFlow / density,
          volumetricFlowM3H: density == null ? null : massFlow / density * 3600 };
      }
    }

    for (const [key, value] of Object.entries(x.provenance || {})) {
      if (!value || !['manual', 'experimental', 'literature', 'estimated'].includes(value.status))
        throw new RangeError(`Indica el origen trazable de ${key} (manual, experimental, literature o estimated).`);
      if (value.status === 'literature' && !value.reference) throw new RangeError(`Falta la referencia bibliográfica de ${key}.`);
    }
    return out;
  }

  const api = Object.freeze({ analyzeProcess });
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else root.ThermaBioBioprocess = api;
})(typeof globalThis !== 'undefined' ? globalThis : this);
