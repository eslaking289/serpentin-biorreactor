# Modelo y cálculos del index integrado

La interfaz activa de la APK es el `index.html` original, conservado sin cambios en `app/src/main/assets/index.html`. La pantalla mantiene sus controles: seleccionar componentes del modelo, alternar corte/transparente/cerrado, mostrar u ocultar etiquetas, pausar la animación, cambiar la temperatura visualizada, editar parámetros, revisar cálculos y restaurar valores ilustrativos.

## Ecuaciones del cálculo

La longitud helicoidal usa el número de vueltas `N`, diámetro central `Dc` y paso `p`:

`L = N · √[(π · Dc)² + p²]`

El área exterior de tubo se estima como `A = π · De · L`. El nivel ideal del caldo cilíndrico se calcula con `Hl = 4 · Vw / (π · Di²)`. El cálculo sensible usa `m = ρ · Vw` y `E = m · Cp · (T0 − Tobj)`.

Para la temperatura de cultivo seleccionada, la página calcula las diferencias terminales y la media logarítmica `ΔTlm`. La potencia transferible se obtiene con `Q̇ = U · A · ΔTlm`. La potencia absorbida por el refrigerante se estima con `q̇ref = ṁ · Cp,ref · (Tout − Tin)`, donde `ṁ` procede del caudal volumétrico y la densidad asumida. La página compara ambos balances y estima el área necesaria con `Areq = q̇ref / (U · ΔTlm)`.

## Supuestos y límites

- El medio y el refrigerante se aproximan como agua, con propiedades constantes.
- El coeficiente global `U` lo introduce el usuario; esta página no calcula correlaciones internas de transferencia.
- No incluye calor metabólico, potencia de agitación, pérdidas al ambiente ni un balance transitorio en el tiempo.
- La geometría, incluidos impulsor y deflectores, es conceptual. No representa un plano de fabricación ni valida resistencia mecánica, soldaduras, soportes o bomba.
- Los valores ilustrativos iniciales solo sirven para mostrar la figura y no son datos experimentales.

Los controles y resultados son los implementados por el `index.html` suministrado. La estimación debe validarse para el proceso y equipo reales antes de usarla como base de fabricación u operación.
