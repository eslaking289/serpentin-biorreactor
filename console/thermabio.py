#!/usr/bin/env python3
"""ThermaBio CLI para Ubuntu 26.04 en WSL. Solo usa la biblioteca estándar."""
import argparse
import csv
import math
from pathlib import Path

DEFAULTS = {
    "dh": (460.0, "Entalpía metabólica, kJ/mol base (O2)"),
    "substrate_rate": (50.0, "Consumo molar de base, mol/h"),
    "heat_factor": (1.0, "Fracción del calor liberado (0–1)"),
    "agitation": (1.2, "Potencia de agitación al caldo, kW"),
    "t_broth": (30.0, "Temperatura uniforme del caldo, °C"),
    "t_in": (20.0, "Entrada del refrigerante, °C"),
    "t_out": (25.0, "Salida objetivo del refrigerante, °C"),
    "cp": (4180.0, "Calor específico del refrigerante, J/(kg·K)"),
    "rho": (997.0, "Densidad del refrigerante, kg/m³"),
    "mu": (0.89, "Viscosidad dinámica, mPa·s"),
    "kf": (0.60, "Conductividad del refrigerante, W/(m·K)"),
    "coolant_flow": (0.25, "Caudal másico disponible, kg/s"),
    "tube_d": (25.4, "Diámetro exterior del tubo, mm"),
    "wall_t": (1.65, "Espesor de pared, mm"),
    "wall_k": (16.0, "Conductividad de pared, W/(m·K)"),
    "pitch": (50.8, "Paso entre vueltas, mm"),
    "tank_d": (1.0, "Diámetro interno del tanque, m"),
    "clearance": (25.0, "Holgura radial a la pared, mm"),
    "h_broth": (1200.0, "Coeficiente externo caldo-tubo, W/(m²·K)"),
    "fouling": (0.0002, "Resistencia de ensuciamiento exterior, m²·K/W"),
}


def ask_number(label, default):
    while True:
        raw = input(f"  {label} [{default:g}]: ").strip()
        if not raw:
            return default
        try:
            number = float(raw.replace(",", "."))
            if math.isfinite(number):
                return number
        except ValueError:
            pass
        print("  Ingresa un número finito válido.")


def nusselt(re, pr):
    def gnielinski(r):
        f = (0.79 * math.log(r) - 1.64) ** -2
        return (f / 8) * (r - 1000) * pr / (
            1 + 12.7 * math.sqrt(f / 8) * (pr ** (2 / 3) - 1)
        )

    if re < 2300:
        return 3.66
    if re >= 3000:
        return gnielinski(re)
    weight = (re - 2300) / 700
    return 3.66 * (1 - weight) + gnielinski(3000) * weight


def lmtd(d1, d2):
    if abs(d1 - d2) < 1e-9 * max(d1, d2):
        return (d1 + d2) / 2
    return (d2 - d1) / math.log(d2 / d1)


def calculate(v):
    errors = []
    if not (v["t_broth"] > v["t_out"] and v["t_broth"] > v["t_in"]):
        errors.append("El caldo debe estar a mayor temperatura que ambas corrientes de refrigerante.")
    if not v["t_out"] > v["t_in"]:
        errors.append("La salida del refrigerante debe ser mayor que su entrada.")
    if v["dh"] <= 0 or v["substrate_rate"] < 0 or not 0 <= v["heat_factor"] <= 1 or v["agitation"] < 0:
        errors.append("Entalpía > 0, consumo y agitación ≥ 0, y fracción de calor entre 0 y 1.")
    if any(v[k] <= 0 for k in ("cp", "rho", "mu", "kf")) or v["coolant_flow"] < 0:
        errors.append("Las propiedades del refrigerante deben ser positivas y el caudal no negativo.")
    if any(v[k] <= 0 for k in ("tube_d", "wall_t", "wall_k", "pitch", "tank_d", "h_broth")) or v["clearance"] < 0 or v["fouling"] < 0:
        errors.append("Geometría y h externo deben ser positivos; holgura y ensuciamiento, no negativos.")

    do, wall = v["tube_d"] / 1000, v["wall_t"] / 1000
    di = do - 2 * wall
    if di <= 0:
        errors.append("El espesor de pared deja un diámetro interior nulo o negativo.")
    if v["pitch"] / 1000 < do:
        errors.append("El paso entre vueltas no puede ser menor que el diámetro exterior del tubo.")
    dh = v["tank_d"] - 2 * v["clearance"] / 1000 - do
    if dh <= do:
        errors.append("El diámetro de hélice debe superar el diámetro exterior del tubo.")
    if errors:
        raise ValueError("\n".join(errors))

    q_reaction = v["dh"] * v["substrate_rate"] * v["heat_factor"] / 3600
    q_total = q_reaction + v["agitation"]
    if q_total <= 0:
        raise ValueError("La carga térmica total debe ser mayor que cero.")
    delta_lm = lmtd(v["t_broth"] - v["t_out"], v["t_broth"] - v["t_in"])
    m_req = q_total * 1000 / (v["cp"] * (v["t_out"] - v["t_in"]))
    mu = v["mu"] / 1000
    velocity = m_req / (v["rho"] * math.pi * di**2 / 4)
    re = 4 * m_req / (math.pi * di * mu)
    pr = mu * v["cp"] / v["kf"]
    h_straight = nusselt(re, pr) * v["kf"] / di
    hi = h_straight * (1 + 3.5 * di / dh) if re >= 2300 else h_straight
    u = 1 / (do / (di * hi) + do * math.log(do / di) / (2 * v["wall_k"]) + 1 / v["h_broth"] + v["fouling"])
    area = q_total * 1000 / (u * delta_lm)
    length = area / (math.pi * do)
    pitch = v["pitch"] / 1000
    turn_length = math.sqrt((math.pi * dh) ** 2 + pitch**2)
    turns = length / turn_length
    height = turns * pitch
    darcy = 64 / re if re < 2300 else (0.79 * math.log(re) - 1.64) ** -2
    pressure = darcy * (length / di) * v["rho"] * velocity**2 / 2
    ratio = v["coolant_flow"] / m_req
    return {
        "Carga metabólica": (q_reaction, "kW"),
        "Carga térmica total": (q_total, "kW"), "LMTD": (delta_lm, "K"),
        "Caudal másico requerido": (m_req, "kg/s"), "Caudal disponible": (v["coolant_flow"], "kg/s"),
        "Velocidad interna": (velocity, "m/s"), "Reynolds": (re, "—"), "Prandtl": (pr, "—"),
        "Coeficiente interno": (hi, "W/(m²·K)"),
        "Coeficiente externo ingresado": (v["h_broth"], "W/(m²·K)"),
        "Coeficiente global exterior": (u, "W/(m²·K)"), "Área exterior requerida": (area, "m²"),
        "Longitud de tubo": (length, "m"), "Vueltas teóricas": (turns, "—"),
        "Altura centro a centro": (height, "m"),
        "Caída de presión (tubo recto eq.)": (pressure, "Pa"),
        "Caudal disponible suficiente": (ratio >= 1, "sí/no"),
        "Régimen": ("Turbulento" if re >= 4000 else "Transición" if re >= 2300 else "Laminar", ""),
        "Gnielinski en rango indicado": (re >= 3000 and re <= 5e6 and 0.5 <= pr <= 2000, "sí/no"),
    }


def main():
    parser = argparse.ArgumentParser(description="ThermaBio local para Ubuntu 26.04 en WSL")
    parser.add_argument("--no-prompt", action="store_true", help="usar los valores predeterminados")
    parser.add_argument("--csv", type=Path, default=Path(__file__).with_name("resultado-thermabio.csv"), help="ruta del informe CSV")
    args = parser.parse_args()

    print("\n  THERMABIO  /  UBUNTU 26.04 WSL")
    print("  Dimensionamiento preliminar de serpentín — local y sin red")
    print("  Unidades SI en cada entrada. Enter conserva el valor sugerido.\n")
    values = {key: default for key, (default, _) in DEFAULTS.items()}
    if not args.no_prompt:
        for key, (default, label) in DEFAULTS.items():
            values[key] = ask_number(label, default)
    try:
        results = calculate(values)
    except ValueError as exc:
        print("\n  ENTRADAS NO VÁLIDAS")
        for error in str(exc).splitlines():
            print(f"  • {error}")
        return 2

    print("  RESULTADOS")
    print("  " + "─" * 74)
    for label, (value, unit) in results.items():
        if isinstance(value, bool):
            rendered = "Sí" if value else "No"
            if label == "Gnielinski en rango indicado" and not value:
                rendered = "No; interpretar con cautela"
        elif isinstance(value, float):
            rendered = f"{value:,.0f}" if label == "Reynolds" else f"{value:,.3f}"
        else:
            rendered = str(value)
        print(f"  {label:<39} {rendered:>22} {unit}")
    print("\n  Alcance educativo preliminar. h externo es un dato de entrada; la caída de presión")
    print("  usa tubo recto equivalente y no incluye accesorios ni curvatura.")
    print("  Valida propiedades y resultados antes de fabricar.")

    args.csv.parent.mkdir(parents=True, exist_ok=True)
    with args.csv.open("w", newline="", encoding="utf-8-sig") as report:
        writer = csv.writer(report)
        writer.writerow(("Parámetro", "Valor", "Unidad"))
        writer.writerows((label, value, unit) for label, (value, unit) in results.items())
    print(f"\n  Informe CSV guardado en: {args.csv.resolve()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
