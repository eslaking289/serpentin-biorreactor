const assert = require('node:assert/strict');
const { analyzeProcess } = require('../bioprocess-core');

const biology = analyzeProcess({
  volumeM3: 0.02, heatMethod: 'oxygen', agitationKW: 0.1,
  muMaxPerH: 0.5, substrateGPerL: 2, ksGPerL: 1, biomassGPerL: 5,
  yieldXPerS: 0.5, maintenanceGPerGXH: 0.1, qO2MolPerGXH: 0.002,
  kLaPerH: 10, cStarMolPerL: 0.00025, cLMolPerL: 0.0001,
  heatKJPerMolO2: 460, coolantCpKJPerKgK: 4.18, coolantDeltaTK: 5,
  coolantDensityKgM3: 998
});
assert.ok(Math.abs(biology.kinetics.muPerH - (1 / 3)) < 1e-12);
assert.ok(Math.abs(biology.oxygen.OURMolPerLH - 0.01) < 1e-12);
assert.ok(Math.abs(biology.oxygen.OTRMolPerLH - 0.0015) < 1e-12);
assert.equal(biology.oxygen.oxygenSufficient, false);
assert.ok(Math.abs(biology.heat.metabolicKW - (460 * 0.01 * 20 / 3600)) < 1e-12);
assert.ok(biology.coolant.massFlowKgS > 0);

const stirred = analyzeProcess({
  volumeM3: 1, manualMetabolicKW: 4, agitationKW: 1, heatMethod: 'manual',
  impellerSpeedPerS: 2, impellerDiameterM: 0.3, powerNumber: 5,
  brothDensityKgM3: 1000, brothViscosityPaS: 0.001
});
assert.ok(Math.abs(stirred.agitation.reynolds - 180000) < 1e-9);
assert.ok(Math.abs(stirred.heat.totalGeneratedKW - (stirred.agitation.powerKW + 4)) < 1e-9);

assert.throws(() => analyzeProcess({ volumeM3: 1, heatMethod: 'manual',
  provenance: { muMaxPerH: { status: 'literature' } } }), /referencia bibliográfica/);
assert.throws(() => analyzeProcess({ volumeM3: 1, heatMethod: 'oxygen',
  qO2MolPerGXH: 1, biomassGPerL: 1 }), /YQ\/O2/);
assert.deepEqual(analyzeProcess({ volumeM3: 1, muMaxPerH: 0.3 }).kinetics.missing,
  ['substrateGPerL', 'ksGPerL', 'biomassGPerL', 'yieldXPerS']);

const oxygenLimited = analyzeProcess({
  volumeM3: 1, muMaxPerH: 0.5, substrateGPerL: 2, ksGPerL: 1,
  biomassGPerL: 4, yieldXPerS: 0.5, ko2MolPerL: 1e-4,
  cLMolPerL: 1e-4, kdPerH: 0.1
});
assert.ok(Math.abs(oxygenLimited.kinetics.muPerH - (1 / 6)) < 1e-12);
assert.ok(Math.abs(oxygenLimited.kinetics.biomassRateGPerLH - (1 / 6 - 0.1) * 4) < 1e-12);

const reaction = analyzeProcess({
  volumeM3: 1, heatMethod: 'reaction', reactionEnthalpyKJPerMol: -100,
  substrateConsumptionMolPerH: 36, agitationKW: 2, agitationHeatFraction: 0.25
});
assert.ok(Math.abs(reaction.heat.metabolicKW - 1) < 1e-12);
assert.ok(Math.abs(reaction.heat.agitationKW - 0.5) < 1e-12);
assert.ok(Math.abs(reaction.heat.totalGeneratedKW - 1.5) < 1e-12);

const coolantOutlet = analyzeProcess({ volumeM3: 1, manualMetabolicKW: 10,
  coolantMassFlowKgS: 1, coolantCpKJPerKgK: 4, coolantInletTempC: 10 });
assert.ok(Math.abs(coolantOutlet.coolantOutletTempC - 12.5) < 1e-12);
console.log('Bioprocess calculations verified.');
