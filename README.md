# ThermaBio

Aplicación local para estimar la carga térmica de una fermentación aeróbica y dimensionar preliminarmente un serpentín de enfriamiento. Está hecha con HTML, CSS y JavaScript sin dependencias, servidor, telemetría ni APIs. Funciona sin conexión al abrir `index.html` en un navegador moderno.

**Aplicación web:** [Abrir ThermaBio](https://eslaking289.github.io/serpentin-biorreactor/)

**APK nativa para Android:** descarga [ThermaBio para Android 1.5.4](https://github.com/eslaking289/serpentin-biorreactor/raw/refs/heads/main/android/dist/ThermaBio-Android-1.5.4.apk). La aplicación no solicita permiso de Internet.

La pantalla **Resultados** contiene una tarjeta que despliega el modelo interactivo del biorreactor dentro de la misma pantalla. El módulo usa recursos incluidos en el APK, y el modelo, los supuestos y sus límites están documentados en [`android/BIORREACTOR.md`](android/BIORREACTOR.md).

En **Condiciones de operación** está la calculadora del balance metabólico para despejar carga térmica total, calor metabólico específico, volumen de caldo o potencia de agitación. En Android aparece dentro de la pantalla **Calculadora** y puedes aplicar q̇_met, volumen o agitación a los campos principales. En **Resultados**, el diagrama paramétrico del biorreactor se muestra expandido al inicio y refleja los valores calculados.

## Inicio rápido

1. Descarga o clona este repositorio.
2. Abre `index.html` directamente en tu navegador, o abre la carpeta en VS Code y usa una extensión de servidor local si quieres recarga automática.
3. Introduce tus datos. Los resultados se recalculan en vivo y puedes descargar el informe como CSV.

ThermaBio puede instalarse como aplicación desde un navegador compatible. Después de abrirla por primera vez con conexión, sus archivos quedan guardados en el dispositivo para usarla sin conexión. No necesita cuentas, APIs ni servicios externos.

## Instalar en un teléfono y usar sin datos

### APK Android independiente

Descarga el APK desde el enlace de arriba en tu teléfono Android, ábrelo y acepta la instalación si Android lo solicita. Una vez instalada, no necesita Wi-Fi ni datos móviles. El APK se firma automáticamente para instalación directa; Google Play requiere una firma de publicación propia.

### Aplicación web para Android o iPhone

**Android:** abre [ThermaBio](https://eslaking289.github.io/serpentin-biorreactor/) en Chrome, toca **Instalar aplicación** o selecciona **Instalar aplicación** en el menú del navegador.

**iPhone:** abre el enlace en Safari, toca **Compartir** y luego **Añadir a pantalla de inicio**.

La primera descarga e instalación sí necesita internet. Después abre ThermaBio una vez mientras tienes conexión para que el teléfono guarde sus archivos; a partir de ahí puedes activar modo avión y seguir calculando sin Wi-Fi ni datos móviles. Los cálculos y la exportación CSV se hacen en el teléfono. No necesita cuenta ni conexión a servidores externos.

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
v = 4 m_req / (ρ π Di²)     Re = 4 m_req / (π Di μ)     Pr = μ Cp / k_f     De = Re √(Di / D_hélice)
Nu = 3.66 (Re < 2300; referencia de tubo recto) | interpolación 2300–3000 | Gnielinski (Re ≥ 3000)
h_i = Nu k_f / Di · [1 + w · 3.5 Di / D_hélice], w = clamp((Re − 2300)/700, 0, 1)
1/Uo = 1/ho + Rf + Do ln(Do/Di)/(2 k_pared) + Do/(Di h_i)
ΔTlm = (ΔT_fría − ΔT_caliente) / ln(ΔT_fría / ΔT_caliente)
A = 1000 Q / (Uo ΔTlm)     L = A / (π Do)
D_hélice = D_tanque − 2 · holgura − Do
f_D = 64/Re (Re ≤ 2300) | interpolación lineal a f_Gn (2300 < Re < 4000) | f_Gn (Re ≥ 4000)
f_Gn = [0.79 ln(Re) − 1.64]⁻²
ΔP ≈ f_D (L/Di) ρ v² / 2   (Darcy–Weisbach de tubo recto equivalente)
```

Se usa Cp en J/(kg·K), viscosidad μ en Pa·s dentro de las ecuaciones (la entrada en mPa·s se convierte en el código), conductividad en W/(m·K), densidad en kg/m³ y diámetros en m. El factor 1000 convierte kW a W. El programa clasifica Re < 2300 como laminar, 2300 ≤ Re < 4000 como transición y Re ≥ 4000 como turbulento; esos límites son referencias de tubo recto y no predicen por sí solos la transición de una hélice. Gnielinski se usa desde Re = 3000, dentro de su intervalo orientativo 3000 ≤ Re ≤ 5×10⁶ y 0.5 ≤ Pr ≤ 2000. Por ello, 3000 ≤ Re < 4000 recibe aviso de flujo transicional aunque el número esté en el intervalo de la correlación.

Para Re < 2300, Nu = 3.66 se conserva solo como referencia de tubo recto completamente desarrollado con pared a temperatura constante; no incorpora el efecto Dean de una hélice. No se extrapola una correlación laminar helicoidal única porque los resultados dependen de Dean, Pr, curvatura, desarrollo térmico y condición de pared. El resultado hᵢ se marca incierto en ese régimen. La corrección helicoidal simplificada se interpola entre Re 2300–3000 y no reemplaza una correlación validada para la geometría real.

ΔP es solo Darcy–Weisbach de tubo recto equivalente. No incluye aumento de fricción por curvatura, accesorios ni pérdidas locales y no debe usarse para seleccionar la bomba. Los experimentos y correlaciones de hélices disponibles dependen de Reynolds y geometría. `core.js` contiene los cálculos sin DOM y `app.js` la interfaz y exportación.

## Supuestos y límites

Es una herramienta educativa de dimensionamiento preliminar, no una especificación para fabricar u operar un equipo. Supone estado estacionario, temperatura uniforme del caldo, propiedades constantes, h_o ingresado, h_i estimado con una correlación simplificada, entalpía y tasa molar con una base consistente y ensuciamiento total referido al área exterior. El perfil térmico del refrigerante no se resuelve de forma acoplada.

El programa no calcula la entalpía desde la estequiometría, no calcula h_o desde el movimiento del caldo, y no resuelve caída de presión helicoidal, pérdidas locales, hidráulica del circuito, transferencia de masa/oxígeno, control dinámico, selección mecánica, corrosión ni restricciones sanitarias. Justifica con bibliografía cada propiedad y coeficiente, y compara los resultados con balances independientes antes de tomar decisiones físicas.

## Referencias técnicas del modelo

- Gnielinski, V. (1976). “New Equations for Heat and Mass Transfer in Turbulent Pipe and Channel Flow”. *International Chemical Engineering*, 16(2), 359–368. [Registro de la Biblioteca Nacional de Japón](https://ndlsearch.ndl.go.jp/books/R100000136-I1570854174985140864).
- Patankar, S. V., Pratap, V. S. & Spalding, D. B. (1974). “Prediction of Laminar Flow and Heat Transfer in Helically Coiled Pipes”. *Journal of Fluid Mechanics*, 62(3), 539–551. [Artículo](https://electronicsandbooks.com/edt/manual/Magazine/J/Journal%20of%20Fluid%20Mechanics/1974%20Volume%2062/S0022112074000796.pdf). Describe la dependencia de flujo secundario, transferencia y fricción con Dean y la curvatura; no se adopta como correlación general para todas las hélices.
- Ali, S. (2001). “Pressure drop correlations for flow through regular helical coil tubes”. *Fluid Dynamics Research*, 28(4), 295–310. [Artículo](https://www.nagare.or.jp/download/noauth.html?d=p295_310.pdf&dir=16). Su trabajo muestra que las pérdidas en hélices dependen de Reynolds y geometría; por eso la caída de presión aquí se marca como aproximación de tubo recto.

Las referencias respaldan correlaciones generales, no validan automáticamente cada geometría ni las entradas de esta herramienta. El rango indicado para Gnielinski y la corrección helicoidal requieren comprobarse contra la edición de bibliografía y las condiciones del caso real.

## Estructura

- `index.html`: estructura de la interfaz.
- `styles.css`: presentación adaptable a escritorio y móvil.
- `core.js`: motor de cálculo.
- `balance-metabolico.html` y `balance-metabolico-core.js`: despejes del balance metabólico, también integrados en Resultados Android.
- `app.js`: validación, interfaz y exportación CSV.
- `MEMORIA.md`: alcance, decisiones, registro de apoyo con IA y pendientes de verificación del equipo.
- `manifest.webmanifest`: configuración de instalación como aplicación.
- `service-worker.js`: almacenamiento local de los archivos para su uso sin conexión.
- `icons/icon.svg`, `icons/icon-192.png`, `icons/icon-512.png`: iconos de la aplicación web instalable.
- `test/verify.js`: verificación independiente y casos límite; ejecutar con `node test/verify.js`.
- `android/`: proyecto Android nativo independiente del sitio web; no incluye permiso de internet.

### Base térmica de la APK

La APK Android usa la entrada de calor volumétrico de la maqueta Stitch: Q_met = q_met · V_caldo, con q_met en kW/m³ y volumen en m³. El PWA mantiene la base molar Q_rxn = |ΔH_r| · OUR · f_calor / 3600; ambas bases son compatibles si q_met = |ΔH_r| · OUR · f_calor / (3600 · V_caldo). La APK supone explícitamente Q_pérdidas = 0, presenta las pérdidas hidráulicas como tubo recto equivalente y advierte que no sirven para seleccionar una bomba.

## Modelo geométrico paramétrico

El esquema frontal de la APK se actualiza con el diámetro y altura del tanque, el volumen de caldo, diámetro y paso de hélice, vueltas calculadas y longitud de tubo. El nivel de líquido se estima como cilindro ideal, `h = V/(πD²/4)`; el cálculo avisa cuando la hélice queda parcialmente fuera del caldo. Esa representación no es una vista de fabricación ni modela internos, boquillas, soportes o elipses del fondo.

El archivo [CAD paramétrico](cad/thermabio_serpentin_parametrico.scad) abre con OpenSCAD y permite editar esas dimensiones fuera de la APK. OpenSCAD genera la hélice por extrusión lineal con giro; las ecuaciones de altura y capacidad, y las verificaciones geométricas están incluidas en el archivo. La pieza es conceptual y no está validada para fabricación mecánica. Ver [instrucciones del modelo CAD](cad/README.md).

## Compatibilidad

Requiere un navegador actualizado con soporte para JavaScript ES2021, `Blob` y descarga de archivos. No hace solicitudes de red. El CSV puede abrirse en hojas de cálculo comunes.
