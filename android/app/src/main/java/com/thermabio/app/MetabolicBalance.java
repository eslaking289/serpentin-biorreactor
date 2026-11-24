package com.thermabio.app;

final class MetabolicBalance {
    private MetabolicBalance() {}

    static double solve(String target, double qTotal, double qMet, double volume, double agitation, double phiD, double qOther) {
        requireFraction(phiD);
        requireNonnegative(agitation, "La potencia de agitación");
        finite(qTotal);
        finite(qMet);
        finite(qOther);
        switch (target) {
            case "qTotal":
                requirePositive(volume, "El volumen de caldo");
                return finite(qMet * volume + phiD * agitation + qOther);
            case "qMet":
                requirePositive(volume, "El volumen de caldo");
                return finite((qTotal - phiD * agitation - qOther) / volume);
            case "volume":
                double heatPerVolume = qTotal - phiD * agitation - qOther;
                if (qMet == 0) throw new IllegalArgumentException("q_met debe ser distinto de cero para despejar el volumen.");
                double calculatedVolume = finite(heatPerVolume / qMet);
                requirePositive(calculatedVolume, "El volumen calculado");
                return calculatedVolume;
            case "agitation":
                requirePositive(volume, "El volumen de caldo");
                if (phiD == 0) throw new IllegalArgumentException("φd debe ser mayor que cero para despejar la potencia de agitación.");
                double power = finite((qTotal - qMet * volume - qOther) / phiD);
                requireNonnegative(power, "La potencia calculada");
                return power;
            default:
                throw new IllegalArgumentException("Selecciona una de las tres entradas que quieres despejar.");
        }
    }

    private static void requirePositive(double value, String label) {
        if (!Double.isFinite(value) || !(value > 0)) throw new IllegalArgumentException(label + " debe ser mayor que cero.");
    }

    private static void requireNonnegative(double value, String label) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException(label + " debe ser cero o mayor.");
    }

    private static void requireFraction(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1) throw new IllegalArgumentException("φd debe estar entre 0 y 1.");
    }

    private static double finite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("El resultado no es finito; revisa los valores y las unidades.");
        return value;
    }
}
