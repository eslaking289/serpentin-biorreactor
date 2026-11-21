# Memoria de desarrollo — ThermaBio

## 1. Planteamiento y alcance

El encargo académico solicita una herramienta reutilizable para calcular la generación de calor de una fermentación aeróbica, sumar aportes relevantes como la agitación, estimar resistencias de transferencia y dimensionar un serpentín bajo condiciones modificables. La escala valorativa asigna peso al balance energético, la transferencia y geometría del serpentín, el funcionamiento, la verificación, la memoria y la defensa.

ThermaBio se planteó como aplicación web estática para escritorio y móvil. Los parámetros son editables, el cálculo ocurre en el navegador y el informe se descarga localmente como CSV. No utiliza APIs, paquetes de terceros, base de datos ni red. Esta decisión facilita la ejecución en el aula y la inspección del código desde Visual Studio Code.

## 2. Organización del trabajo y decisiones de alcance

1. Revisar el enunciado del proyecto y la escala valorativa proporcionados.
2. Traducir los requisitos a entradas, ecuaciones, resultados y límites visibles.
3. Construir una interfaz web estática y separar presentación, estructura y cálculos.
4. Documentar ecuaciones, unidades, supuestos y elementos que requieren respaldo bibliográfico del equipo.
5. Completar pruebas con casos independientes y registrar resultados antes de la entrega académica.

Se aceptó que el usuario introduzca la entalpía molar, la tasa de consumo y coeficientes convectivos estimados. No existe una única reacción global ni una correlación válida para todo caldo y geometría; presentar esos datos como entradas evita ocultar supuestos. La interfaz etiqueta el resultado como estimación y advierte que no es una especificación de fabricación.

## 3. Fundamentación implementada

La potencia térmica metabólica se estima como el producto de magnitud de entalpía molar, consumo molar y fracción de calor liberado al caldo, con conversión de horas a segundos. Se suma la potencia de agitación disipada.

El refrigerante circula por el interior del tubo y el caldo baña el exterior. El caudal requerido se calcula con el balance sensible para el salto de temperatura especificado. El flujo interior se clasifica como laminar (Re < 2300), transición (2300 ≤ Re < 4000) o turbulento (Re ≥ 4000); los umbrales son referencias de tubo recto y no predicen por sí solos la transición en una hélice. Gnielinski se usa desde Re = 3000, de modo que parte del intervalo de transición tiene aviso explícito. Para Re < 2300 se mantiene Nu = 3.66 como referencia de tubo recto sin corrección Dean; la app advierte que hᵢ no representa de manera validada el efecto de la curvatura. No se incorporó una correlación helicoidal laminar única porque depende de Dean, Pr, geometría y condición térmica de pared. En transición se interpolan Nu y factor de fricción, señalados como aproximaciones. El coeficiente global referido al área exterior suma las resistencias de película interior, conducción cilíndrica de pared, película exterior y ensuciamiento. El área usa la diferencia media logarítmica entre el caldo uniforme y las temperaturas de entrada/salida del refrigerante. Las vueltas y altura son una aproximación geométrica helicoidal.

El balance sensible usa propiedades editables del refrigerante; los valores por defecto aproximan agua líquida. El usuario debe justificar los datos elegidos. La tasa molar y la entalpía deben corresponder a la misma especie de referencia; los valores por defecto usan oxígeno. Las ecuaciones y unidades están en `README.md` y en la metodología de la interfaz.

## 4. Registro de apoyo con IA

### Instrucción recibida

El usuario pidió desarrollar en VS Code una solución profesional, publicarla en GitHub y hacerla sin APIs externas y utilizable sin conexión. Indicó distinguir las instrucciones dentro de los documentos de su propia solicitud.

### Apoyo de IA aplicado

- Lectura de los dos PDF suministrados para extraer el objetivo, entregables, criterios de evaluación y niveles de desempeño.
- Propuesta de una aplicación web local de archivos estáticos para cubrir el software funcional, los cálculos, las condiciones de operación editables y la exportación de resultados.
- Redacción de la implementación inicial de interfaz, cálculos y documentación técnica.

### Decisiones que el equipo debe revisar

- La aplicación no calcula la entalpía desde el balance estequiométrico. El equipo debe formular la reacción global elegida y determinar/calcular su entalpía con fuentes fiables y una base de cálculo consistente.
- Los coeficientes de película `hi` y `ho` se ingresan, no se derivan de correlaciones de momento/transferencia. Deben justificarse para las condiciones y la geometría reales.
- La potencia de agitación se agrega como aporte íntegro al caldo.
- Se adoptan propiedades constantes, estado estacionario y temperatura uniforme del caldo.
- La construcción inicial no añade referencias externas específicas que no estén verificadas por el equipo. Incorporar bibliografía consultada y sus decisiones a la versión final.

## 5. Evolución, dificultades y soluciones

La primera versión define tres archivos separados: `index.html` para la interfaz, `styles.css` para el diseño adaptable y `app.js` para validación, cálculos y exportación local. Se añadieron mensajes cuando las temperaturas no permiten el perfil asumido y cuando la capacidad sensible calculada del refrigerante es menor que la carga térmica.

Un requisito central era operar sin conexión. Por ese motivo se retiró la importación de fuentes web y la interfaz usa fuentes del sistema; no carga imágenes, librerías ni recursos remotos.

## 6. Verificación independiente y casos límite

El caso predeterminado se calculó de forma independiente sustituyendo los datos en las ecuaciones del apartado 3 y se comparó con `require('./core.js').design(valores)`. Se usaron `ΔH = 460 kJ/mol`, `OUR = 50 mol/h`, `f_calor = 1`, `P_ag = 1.2 kW`, `T_caldo = 30 °C`, `T_in = 10 °C`, `T_out = 18 °C`, `Cp = 4185 J/(kg·K)`, `ρ = 999 kg/m³`, `μ = 1.17 mPa·s`, `k_f = 0.589 W/(m·K)`, `D_o = 25 mm`, `e = 2 mm`, `k_p = 16 W/(m·K)`, `h_o = 1200 W/(m²·K)`, `R_f = 0.0002 m²·K/W`, tanque de 1.2 m, holgura radial 100 mm y paso 75 mm.

Pasos intermedios de comprobación: `D_i = 0.021 m`, `D_h = 0.975 m`; `Q_rxn = 460·50/3600 = 6.388889 kW`; `Q_total = 7.588889 kW`; `ṁ = 7588.889/[4185·(18−10)] = 0.226669 kg/s`; `ΔT₁ = 12 K`, `ΔT₂ = 20 K`, `ΔT_lm = 15.660922 K`; `v = 0.655087 m/s`; `Re = 11746.21`; `Pr = 8.31316`; `De = Re·√(D_i/D_h) = 1723.87`.

| Resultado | Cálculo independiente | Node (`core.js`) | Diferencia |
|---|---:|---:|---:|
| Q metabólico (kW) | 6.388889 | 6.388889 | < 1e-9 relativo |
| Q total (kW) | 7.588889 | 7.588889 | < 1e-9 relativo |
| Caudal requerido (kg/s) | 0.226669 | 0.226669 | < 1e-9 relativo |
| Re / Pr | 11746.21 / 8.31316 | 11746.21 / 8.31316 | < 1e-9 relativo |
| Nu / hᵢ (W/m²·K) | 98.35714 / 2966.646 | 98.35714 / 2966.646 | < 1e-9 relativo |
| U exterior (W/m²·K) | 636.605 | 636.605 | < 1e-9 relativo |
| ΔTlm (K) | 15.660922 | 15.660922 | < 1e-9 relativo |
| Área / longitud (m² / m) | 0.761187 / 9.691729 | 0.761187 / 9.691729 | < 1e-9 relativo |
| Vueltas / altura (m) | 3.163127 / 0.237235 | 3.163127 / 0.237235 | < 1e-9 relativo |
| ΔP (Pa, tubo recto equivalente) | 2978.312 | 2978.312 | < 1e-9 relativo |

La verificación automatizada sin framework se ejecuta con `node test/verify.js`. Incluye el caso de referencia, Re = 2300/3000/4000, continuidad de Nu y factor Darcy, LMTD igual y casi igual, diámetro interior inválido, paso menor que Do, hélice inviable, carga cero, entrada incompleta/no finita, decimal 30.3 °C, invalidación de indicadores en modo quiet, ciclo de vida de URL para CSV y fallo de icono opcional durante instalación del Service Worker.

## 7. Limitaciones conocidas y trabajo futuro

No se incluye un balance de materia/estequiometría integrado, cálculo de h_o desde el movimiento del caldo, perfil acoplado del refrigerante, pérdidas de presión helicoidales y locales, diseño hidráulico completo, transferencia de oxígeno, dinámica de control ni diseño mecánico. Nu = 3.66 laminar no corrige Dean; las interpolaciones de transición son aproximadas; ΔP es tubo recto equivalente. hᵢ y la caída de presión no sirven por sí solos para dimensionamiento final. El caso de referencia numérico coincide con cálculo independiente; aún debe verificarse el modelo contra datos experimentales/una correlación helicoidal validada para la geometría real.

## 8. Guion breve para la demostración

1. Mostrar que la interfaz abre localmente y explicar las entradas.
2. Presentar la base de entalpía y el balance de potencia, incluyendo agitación.
3. Modificar una variable y describir cómo cambia la carga, el área y la geometría propuesta.
4. Explicar qué representan las resistencias térmicas y cómo se estimaron los coeficientes introducidos.
5. Exportar el CSV y exponer las hipótesis, pruebas independientes y límites de uso.

## 9. Correcciones de la segunda versión

Se invirtieron los lados de transferencia: ahora el refrigerante va por dentro del tubo y el caldo por fuera, como en un serpentín real. El caudal de refrigerante pasó de dato supuesto a resultado (m = Q / (Cp ΔT)), porque antes la ΔTlm y la capacidad sensible podían ser incompatibles. La hélice deja holgura a la pared y se valida que el paso no sea menor que el diámetro del tubo. Se quitó el `step` rígido de los campos, que invalidaba valores como 30.3 °C. Los valores por defecto se ajustaron a una carga de unos 7.6 kW. La verificación independiente y sus límites están documentados en la sección 6.

## 10. Revisión de robustez del modelo web

Se unificaron los avisos con los dominios de cada correlación: Re < 2300 laminar; 2300–4000 transicional; Re ≥ 4000 turbulento, y Gnielinski se considera desde Re = 3000 con su propio aviso de rango. `Nu = 3.66` se declara referencia de tubo recto, no correlación laminar de hélice; la interfaz muestra Dean y advierte que no se modela el efecto Dean en laminar. El factor de curvatura se mezcla suavemente entre Re 2300–3000. El factor Darcy también se interpola en transición y la caída de presión queda expresamente limitada a tubo recto equivalente.

Se añadieron validaciones de objeto, campos finitos, propiedades y dimensiones al motor independiente del DOM. El cálculo LMTD usa `log1p` cerca de diferencias iguales y rechaza entradas no positivas. La aplicación limpia los indicadores cuando una entrada en modo quiet es inválida. El enlace del CSV se agrega al DOM y su URL se revoca un segundo después del click. El Service Worker usa `thermabio-v3`; los archivos esenciales siguen siendo obligatorios y los iconos se intentan guardar por separado para que un icono ausente no aborte la instalación. La estrategia network-first ahora recurre a caché también ante respuestas HTTP no exitosas.

## 11. Revisión de la APK nativa inspirada en Stitch

Se conservaron la interfaz de tres pantallas, navegación inferior, papel crema, bordes negros marcados y acentos turquesa/amarillo/magenta. Se reemplazó el cálculo demostrativo del HTML de Stitch: no se fija U arbitrariamente ni se presentan como calculadas cifras de presión incompatibles con Darcy–Weisbach. La APK usa Q_met = q_met · V_caldo, Q_diseño = Q_met + P_agitación con pérdidas al ambiente omitidas (Q_pérdidas = 0); el caudal másico se calcula como ṁ = Q_diseño/[Cp(T_s−T_e)] usando Q en kW y Cp en kJ/(kg·K). Después calcula Reynolds, Prandtl, Nusselt, resistencias en serie, área exterior, longitud, vueltas, altura y una presión recta equivalente. Se agregaron verificaciones de ajuste radial/vertical y avisos por velocidad alta, transición, laminar helicoidal no corregido por Dean y pérdidas helicoidales omitidas.

Comprobación aritmética del juego visible en Stitch (q_met=38 kW/m³, V=1.2 m³, P_ag=2.9 kW, T_caldo=37 °C, T_entrada=10 °C, T_salida=15 °C, agua aproximada a 20 °C, tubo D_o=25.4 mm, pared 1.65 mm, hélice 550 mm, paso 50 mm, h_o=1150 W/(m²·K), k_p=16.3 W/(m·K), R_f=0.0002 m²·K/W):

| Magnitud | Cálculo con las ecuaciones de la APK |
|---|---:|
| Q_met / Q_diseño | 45.6 / 48.5 kW |
| ΔT₁ / ΔT₂ / ΔT_lm | 22 / 27 / 24.414729 K |
| Caudal requerido | 2.320574 kg/s (8.37 m³/h) |
| v / Re / Pr / Nu / De | 6.061645 m/s / 133,694 / 6.9667 / 768.258 / 26,800 |
| h_i / U_o | 23,791 / 815.46 W/(m²·K) |
| Área / longitud | 2.4361 m² / 30.5285 m |
| Vueltas / altura centro a centro | 17.6609 / 0.8830 m |
| ΔP Darcy de tubo recto | 428.9 kPa |

El valor de ΔT_lm en la pantalla de Stitch no corresponde a sus temperaturas de entrada (la diferencia correcta es 27 K, no 28 K). Su h_i=3200 W/(m²·K) no sale de Gnielinski con el caudal/diámetro indicados, y ΔP=18.6 kPa no coincide con Darcy recto (el cálculo de cribado supera 400 kPa antes de añadir la penalización de curvatura). Esto confirma que no debe mostrarse el resultado original como verificado. La tabla verifica la aritmética de la aproximación implementada; no es validación experimental ni una correlación hidráulica helicoidal apta para diseño final.
En la APK 1.4.1 las ecuaciones de metodología se componen con vistas nativas: fracciones apiladas con barra, subíndices y exponentes tipográficos, y líneas separadas para evitar notación con guiones bajos como si fuera código. Se incorporaron a la pantalla las expresiones efectivamente usadas por el motor para Pr (Cp de entrada en kJ/(kg·K), convertido a J/(kg·K)), Gnielinski, el factor de curvatura mezclado, la resistencia global, el ajuste geométrico y Darcy recto.

## 12. Esquema geométrico paramétrico de la APK

En la APK 1.4.2 el dibujo conceptual sinusoidal se sustituyó por una elevación frontal escalada del tanque, nivel de caldo estimado a partir del volumen y diámetro interior, hélice con número y paso de vueltas calculados, y cotas de diámetro, altura y longitud de tubo. Se añadió una comprobación de capacidad cilíndrica ideal y otra de inmersión completa considerando holgura axial de un diámetro exterior en cada extremo. El diámetro interior predeterminado del tanque cambió a 1100 mm: con 1200 L y 1400 mm de altura, un tanque de 900 mm resultaba volumétricamente insuficiente.

Se agregó `cad/thermabio_serpentin_parametrico.scad`, un modelo OpenSCAD editable que muestra el recipiente en corte, el líquido y el tubo hueco helicoidal. Usa `linear_extrude` con giro para construir la geometría espiral; esta operación está descrita en el [manual oficial de OpenSCAD](https://files.openscad.org/documentation/manual/The_OpenSCAD_Language.html). El SCAD permite revisar forma y cotas con software CAD local. Tanto el diagrama Android como el modelo SCAD aproximan el tanque como cilindro recto: no incluyen cabezales, conexiones normalizadas, soportes, soldadura ni validación estructural. No deben tomarse como planos de fabricación.
