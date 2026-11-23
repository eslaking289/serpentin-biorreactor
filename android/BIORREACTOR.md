# Modelo y cálculos del index integrado

La app parte de la interfaz estable de la versión 1.6.1. La calculadora conserva el balance metabólico, los despejes de entradas, los resultados y la metodología. El modelo carga el `index.html` original, sin editar su contenido, dentro de una sola WebView reutilizada en Resultados y Modelo 3D. Android adapta el tamaño, fondo y distribución con estilos añadidos al cargar la página; los parámetros calculados se sincronizan mediante `BiorreactorApp.setParametros`. Se conservan controles de componentes, modos de vista, etiquetas, animación, temperatura, parámetros y cálculos del modelo.

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

Los resultados térmicos y geométricos se calculan en la app nativa y se pasan a la página original del modelo. Las ecuaciones y supuestos descritos arriba documentan el modelo de referencia del proyecto. La estimación debe validarse para el proceso y equipo reales antes de usarse como base de fabricación u operación.
