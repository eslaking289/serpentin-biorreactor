# Módulo interactivo de biorreactor Android

En la pantalla **Resultados** también está disponible **Calcula cada variable**. El módulo `despejes.html`, incluido en los assets del APK, permite resolver carga térmica (con la base volumétrica de la APK o la base molar de la web), caudal másico requerido, ΔT logarítmica, U requerido, área exterior, longitud, vueltas, altura y caída de presión por separado. Cada opción muestra su ecuación, unidades y validaciones; los datos se ingresan en la unidad que muestra cada campo. La caída de presión se identifica como estimación de Darcy–Weisbach para tubo recto equivalente.

El módulo se despliega dentro de la tarjeta **Biorreactor en corte** en la pantalla **Resultados**; no abre otra pantalla. Está incluido en `android/app/src/main/assets/bioreactor/` y se carga en una `WebView` local expandida a la altura del contenido, con las solicitudes de red bloqueadas. Su interfaz usa los colores y la tipografía de ThermaBio. No depende del sitio GitHub Pages, servicios web, bibliotecas descargadas ni permiso de Internet. Al abrirlo, recibe los valores actualmente editados en la calculadora.

## Funciones

- Esquema conceptual seleccionable de recipiente, motor, eje/agitador, serpentín, sondas y conexiones.
- Animaciones con controles separados para etiquetas, refrigerante, mezcla, pausa, zoom y restablecimiento; los controles responden a toque y teclado.
- Entradas editables en unidades SI para carga, temperaturas, propiedades y geometría. El diámetro interior se deriva de `Di = Do − 2e`; el nivel de líquido puede ingresarse o estimarse con volumen y diámetro. La representación paramétrica actualiza nivel, diámetro, altura y vueltas.
- Dos modelos de carga: fermentación estacionaria (`q_met V + P_agit`) y enfriamiento por lote, que requiere masa, calor específico del caldo, temperaturas inicial/final y duración.
- Dos criterios de serpentin: dimensionar longitud/área/vueltas requeridas o evaluar longitud/vueltas/área declaradas de una instalación. En evaluación se compara longitud geométrica contra vueltas/paso, y capacidad estimada contra carga. `U` manual es opcional; si se deja vacío se usa el U calculado.
- Refrigerante y material se pueden etiquetar desde controles; propiedades como densidad, calor específico, viscosidad, conductividad y conductividad de pared siempre se editan explícitamente. El selector no cambia propiedades ni consulta fuentes externas.

## Ecuaciones de la estimación

En fermentación estacionaria:

`Q̇ = q_met V + P_agit` (kW)

En enfriamiento por lote:

`E = m_b Cp_b (T_i − T_f)` (kJ)

`Q̇_media = E / (3600 t_h) + P_agit` (kW)

Para ambos modos, con caldo isotermo:

`ΔT1 = T_caldo − T_out`, `ΔT2 = T_caldo − T_in`

`ΔT_lm = (ΔT2 − ΔT1) / ln(ΔT2 / ΔT1)`; si son iguales, `ΔT_lm = ΔT1`.

`ṁ_req = Q̇ / [Cp_r (T_out − T_in)]`, usando kW = kJ/s y `Cp_r` en kJ/(kg·K).

`Re = 4 ṁ / (π Di μ)`, `Pr = μ Cp_r / k_f`, convirtiendo μ de mPa·s a Pa·s y `Cp_r` a J/(kg·K) para Pr.

`Nu = 3.66` para Re < 2300; transición lineal hacia Gnielinski en 2300–3000; Gnielinski desde Re ≥ 3000.

`h_i = (Nu k_f / Di) [1 + w 3.5 Di / D_c]`, `w = clamp((Re−2300)/700, 0, 1)`.

`1/U_o = Do/(Di h_i) + Do ln(Do/Di)/(2 k_pared) + 1/h_o + R_f`

`A_o = 1000 Q̇ / (U_o ΔT_lm)`, `L = A_o/(π Do)`

`A_i = π Di L`; para evaluar un serpentín instalado `Q̇_transferida = U A_o ΔT_lm / 1000` en kW. Si se ingresa un área externa declarada, se contrasta con `π Do L` y se muestra un aviso si difiere más de 5 %.

`L_vuelta = √[(π D_c)² + p²]`, `N = L/L_vuelta`, `H = N p`.

`ΔP ≈ f_D (L/Di) ρ v²/2` como aproximación Darcy–Weisbach de tubo recto.

### Comprobación numérica del modo fermentación

Con los valores de inicio de la app (`q_met=38 kW/m³`, `V=1.2 m³`, `P_agit=2.9 kW`, `T_caldo=37 °C`, refrigerante 10→15 °C, `Cp_r=4.18 kJ/(kg·K)`, `ρ=998 kg/m³`, `μ=1 mPa·s`, `k_f=0.6 W/(m·K)`, `Do=25.4 mm`, pared 1.65 mm, `D_c=550 mm`, `p=50 mm` y los coeficientes editables iniciales), la sustitución independiente produce:

| Magnitud | Resultado aproximado |
|---|---:|
| `Q̇ = 38·1.2 + 2.9` | 48.50 kW |
| `ΔT_lm` | 24.4147 K |
| `ṁ_req` | 2.32057 kg/s |
| `Re / Pr` | 133,694 / 6.9667 |
| `Nu / h_i` | 768.258 / 23,791 W/(m²·K) |
| `U_o` | 815.455 W/(m²·K) |
| `A_o / L` | 2.43607 m² / 30.5285 m |
| `N / H` | 17.6609 vueltas / 0.88304 m |
| velocidad del refrigerante | 6.06 m/s |
| `ΔP` recto equivalente | 428.895 kPa |

La pérdida calculada es elevada porque el caso da una velocidad interna aproximada de 6.06 m/s. Se conserva la advertencia del modelo: esta cifra omite curvatura y accesorios y no debe usarse para seleccionar una bomba.

### Verificaciones funcionales

- Modo instalado: con `L=31 m`, `N=18`, `A_o=2.45 m²` y `U=800 W/(m²·K)`, el módulo estima `Q_transferida=47.85 kW` y avisa que no alcanza los 48.5 kW de carga del caso base.
- Enfriamiento por lote: con `m=1200 kg`, `Cp_b=4 kJ/(kg·K)`, 37→20 °C, `t=2 h` y `P_agit=2.9 kW`, obtiene `Q̇_media=14.23 kW`; marca que el LMTD a temperatura media es una aproximación transitoria.
- Entrada inválida `e=13 mm`, `Do=25.4 mm`: invalida `Di`, borra los indicadores numéricos y muestra la corrección requerida.
- `core.js` del sitio permanece sin cambios; el módulo Android implementa en JavaScript la misma base de Gnielinski, resistencias y Darcy del proyecto Android actual.

## Supuestos y límites

- En el modo por lote, el cálculo de área usa una temperatura de caldo igual a `(T_i + T_f)/2` para aproximar LMTD. El proceso real es transitorio y requiere integrar la temperatura y el flujo de calor con el tiempo; la potencia calculada es un promedio y no representa el pico inicial.
- El régimen laminar conserva `Nu = 3.66` de tubo recto, sin corrección Dean. La corrección helicoidal aplicada fuera de esa referencia es simplificada; no valida `h_i` para cualquier hélice.
- `h_o` es una entrada. No se modelan potencia de agitación hidrodinámica, reología, mezcla, ensuciamiento variable, presión, esfuerzos mecánicos, soportes, soldaduras, expansión, accesibilidad de limpieza, esterilización ni conexiones de seguridad.
- La caída de presión omite curvatura, accesorios y pérdidas locales, y no sirve para dimensionar bomba.
- La figura es una visualización conceptual parametrizada. Dibuja un máximo de 34 vueltas para conservar legibilidad; el resultado numérico completo de vueltas se presenta aparte. No es un plano de fabricación ni un modelo CAD.
- Los valores iniciales heredados del proyecto son ilustrativos y deben reemplazarse por datos justificados del proceso y del proveedor. No interpretar una advertencia vacía como certificación de diseño.

Este módulo sirve para explorar conceptos y preparar un cálculo preliminar. La configuración final requiere revisión de ingeniería y validación contra datos, normas aplicables y condiciones reales.
