package com.thermabio.app;

final class MetabolicBalance {
    private MetabolicBalance() {}

    static double solve(String target, double qTotal, double qMet, double volume, double agitation) {
        switch (target) {
            case "qTotal":
                requireNonnegative(qMet, "El calor metabólico específico");
                requirePositive(volume, "El volumen de caldo");
                requireNonnegative(agitation, "La potencia de agitación");
                return finite(qMet * volume + agitation);
            case "qMet":
                requireNonnegative(qTotal, "La carga térmica total");
                requireNonnegative(agitation, "La potencia de agitación");
                requirePositive(volume, "El volumen de caldo");
                if (qTotal < agitation) throw new IllegalArgumentException("La carga total no puede ser menor que la potencia de agitación.");
                return finite((qTotal - agitation) / volume);
            case "volume":
                requireNonnegative(qTotal, "La carga térmica total");
                requireNonnegative(agitation, "La potencia de agitación");
                requirePositive(qMet, "El calor metabólico específico");
                if (qTotal < agitation) throw new IllegalArgumentException("La carga total no puede ser menor que la potencia de agitación.");
                return finite((qTotal - agitation) / qMet);
            case "agitation":
                requireNonnegative(qTotal, "La carga térmica total");
                requireNonnegative(qMet, "El calor metabólico específico");
                requirePositive(volume, "El volumen de caldo");
                double metabolic = qMet * volume;
                if (qTotal < metabolic) throw new IllegalArgumentException("La carga total no puede ser menor que q̇_met · V_caldo; resultaría una potencia de agitación negativa.");
                return finite(qTotal - metabolic);
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

    private static double finite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("El resultado no es finito; revisa los valores y las unidades.");
        return value;
    }
}
