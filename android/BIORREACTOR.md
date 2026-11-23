# Modelo y cálculos del index integrado

La app parte de la interfaz nativa estable de la versión 1.6.1. La calculadora conserva el balance metabólico, los despejes de entradas, los resultados y la metodología. El Modelo 3D usa una sola página (`app/src/main/assets/index.html`) tanto para la vista previa de Resultados como para la sección interactiva completa. Los parámetros de geometría y operación calculados por la app nativa se sincronizan con esa página mediante `BiorreactorApp.setParametros`; se mantienen la selección de componentes, modos de vista, etiquetas, animación, temperatura, cálculos y edición/restauración de parámetros.

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

Los cálculos propios del modelo siguen usando las ecuaciones y supuestos descritos arriba, separados del balance metabólico y del dimensionamiento nativo. La geometría y condiciones que se muestran parten de los resultados nativos sincronizados; ambas estimaciones deben validarse para el proceso y equipo reales antes de usarse como base de fabricación u operación.
