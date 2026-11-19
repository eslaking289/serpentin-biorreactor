# ThermaBio

Aplicación local para estimar la carga térmica de una fermentación aeróbica y dimensionar preliminarmente un serpentín de enfriamiento. Está hecha con HTML, CSS y JavaScript sin dependencias, servidor, telemetría ni APIs. Funciona sin conexión al abrir `index.html` en un navegador moderno.

**Aplicación web:** [Abrir ThermaBio](https://eslaking289.github.io/serpentin-biorreactor/)

## Inicio rápido

1. Descarga o clona este repositorio.
2. Abre `index.html` directamente en tu navegador, o abre la carpeta en VS Code y usa una extensión de servidor local si quieres recarga automática.
3. Introduce tus datos. Los resultados se recalculan en vivo y puedes descargar el informe como CSV.

ThermaBio puede instalarse como aplicación desde un navegador compatible. Después de abrirla por primera vez con conexión, sus archivos quedan guardados en el dispositivo para usarla sin conexión. No necesita cuentas, APIs ni servicios externos.

## Instalar como aplicación

1. Abre [ThermaBio](https://eslaking289.github.io/serpentin-biorreactor/) en Chrome o Edge.
2. Pulsa **Instalar aplicación** si aparece el botón; también puedes usar **Instalar ThermaBio** desde el menú del navegador.
3. Una vez instalada y abierta al menos una vez, ThermaBio puede iniciarse y calcular sin conexión.

La instalación inicial necesita internet para cargar la aplicación y guardar sus archivos. Luego los cálculos, la metodología y la exportación CSV funcionan localmente. También puedes abrir `index.html` directamente desde una carpeta descargada; el modo instalable sin conexión se activa al entrar por primera vez desde el sitio HTTPS.

## Alcance funcional

- Balance de calor metabólico basado en entalpía de reacción y consumo molar de sustrato.
- Inclusión de la disipación de potencia de agitación.
- Temperaturas configurables del caldo y del refrigerante.
- Coeficiente global estimado con resistencias en serie: película interior, conducción cilíndrica, película exterior y ensuciamiento.
- Diferencia media logarítmica para un caldo mezclado a temperatura constante.
- Área y longitud requeridas y estimación de vueltas y altura helicoidal.
- Comparación del deber térmico con la capacidad sensible del caudal de refrigerante.
- Exportación CSV del informe y entradas configurables, sin persistencia de datos en la nube.

## Ecuaciones y base de cálculo

El modelo considera el refrigerante dentro del tubo y el caldo por fuera. Calcula el caudal necesario para absorber la carga con el salto de temperatura seleccionado; lo compara con el caudal disponible y usa el caudal requerido para estimar el flujo interior. La entalpía molar y la tasa de consumo deben corresponder a la misma especie química. Los valores iniciales usan una base de oxígeno: 460 kJ/mol O₂ y 50 mol O₂/h.

```text
Q = |ΔH_r| · n_r · f_calor / 3600 + P_agitación           [kW]
m_req = 1000 · Q / (Cp · (T_salida − T_entrada))         [kg/s]
v = 4 m_req / (ρ π Di²)     Re = 4 m_req / (π Di μ)     Pr = μ Cp / k_f
Nu = 3.66 (Re < 2300) | interpolación 2300–3000 | Gnielinski (Re ≥ 3000)
h_i = Nu k_f / Di · (1 + 3.5 Di / D_hélice)   (corrección helicoidal si Re ≥ 2300)
1/Uo = 1/ho + Rf + Do ln(Do/Di)/(2 k_pared) + Do/(Di h_i)
ΔTlm = (ΔT_fría − ΔT_caliente) / ln(ΔT_fría / ΔT_caliente)
A = 1000 Q / (Uo ΔTlm)     L = A / (π Do)
D_hélice = D_tanque − 2 · holgura − Do
f_D = 64/Re (Re < 2300) | [0.79 ln(Re) − 1.64]⁻² (Re ≥ 2300; aproximación)
ΔP ≈ f_D (L/Di) ρ v² / 2   (tubo recto equivalente, orientativo)
```

Se usa Cp en J/(kg·K), viscosidad μ en Pa·s dentro de la ecuación (la entrada en mPa·s se convierte en el código), conductividad del refrigerante en W/(m·K), densidad en kg/m³ y diámetros en m. El factor 1000 convierte kW a W al determinar el caudal y el área. La correlación de Gnielinski se reporta como orientativa en 3000 ≤ Re ≤ 5×10⁶ y 0.5 ≤ Pr ≤ 2000; la transición y el multiplicador helicoidal son aproximaciones. La caída de presión aplica una aproximación de tubo recto a la longitud calculada y no incorpora curvatura, accesorios ni pérdidas locales. `core.js` contiene los cálculos sin DOM y `app.js` la interfaz y exportación.

## Supuestos y límites

Es una herramienta educativa de dimensionamiento preliminar, no una especificación para fabricar u operar un equipo. Supone estado estacionario, temperatura uniforme del caldo, propiedades constantes, h_o ingresado, h_i estimado con una correlación simplificada, entalpía y tasa molar con una base consistente y ensuciamiento total referido al área exterior. El perfil térmico del refrigerante no se resuelve de forma acoplada.

El programa no calcula la entalpía desde la estequiometría, no calcula h_o desde el movimiento del caldo, y no resuelve caída de presión helicoidal, pérdidas locales, hidráulica del circuito, transferencia de masa/oxígeno, control dinámico, selección mecánica, corrosión ni restricciones sanitarias. Justifica con bibliografía cada propiedad y coeficiente, y compara los resultados con balances independientes antes de tomar decisiones físicas.

## Referencias técnicas del modelo

- Gnielinski, V. (1975). “New equations for heat and mass transfer in turbulent pipe and channel flow”. *Forschung im Ingenieurwesen*, 41, 8–16. [Registro bibliográfico](https://ui.adsabs.harvard.edu/abs/1975STIA...7522028G/abstract).
- Ali, S. (2001). “Pressure drop correlations for flow through regular helical coil tubes”. *Fluid Dynamics Research*, 28(4), 295–310. [Artículo](https://www.nagare.or.jp/download/noauth.html?d=p295_310.pdf&dir=16). Su trabajo muestra que las pérdidas en hélices dependen de Reynolds y geometría; por eso la caída de presión aquí se marca como aproximación de tubo recto.

Las referencias respaldan correlaciones generales, no validan automáticamente cada geometría ni las entradas de esta herramienta. El rango indicado para Gnielinski y la corrección helicoidal requieren comprobarse contra la edición de bibliografía y las condiciones del caso real.

## Estructura

- `index.html`: estructura de la interfaz.
- `styles.css`: presentación adaptable a escritorio y móvil.
- `core.js`: motor de cálculo.
- `app.js`: validación, interfaz y exportación CSV.
- `MEMORIA.md`: alcance, decisiones, registro de apoyo con IA y pendientes de verificación del equipo.
- `manifest.webmanifest`: configuración de instalación como aplicación.
- `service-worker.js`: almacenamiento local de los archivos para su uso sin conexión.
- `icons/icon.svg`: icono de la aplicación.

## Compatibilidad

Requiere un navegador actualizado con soporte para JavaScript ES2021, `Blob` y descarga de archivos. No hace solicitudes de red. El CSV puede abrirse en hojas de cálculo comunes.
