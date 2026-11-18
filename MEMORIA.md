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

El refrigerante circula por el interior del tubo y el caldo baña el exterior. El caudal requerido se calcula con el balance sensible para el salto de temperatura especificado. Ese caudal permite estimar velocidad, Reynolds, Prandtl y el coeficiente interior con Gnielinski y una corrección helicoidal aproximada. El coeficiente global referido al área exterior suma las resistencias de película interior, conducción cilíndrica de pared, película exterior y ensuciamiento. El área usa la diferencia media logarítmica entre el caldo uniforme y las temperaturas de entrada/salida del refrigerante. Las vueltas y altura son una aproximación geométrica helicoidal.

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

## 6. Verificación pendiente para el equipo

La verificación académica aún debe completarse antes de presentar resultados como confiables. Registrar aquí versiones, casos, resultados esperados independientes, resultados observados, diferencias y correcciones. Casos mínimos recomendados:

1. Caso de referencia con valores calculados a mano en una hoja independiente.
2. Cambiar el caudal de refrigerante y comprobar la variación de capacidad sensible.
3. Cambiar `|ΔHr|` o consumo de sustrato y comprobar la proporcionalidad de la carga.
4. Cambiar `hi`, `ho` o ensuciamiento y comprobar el sentido del cambio de `U` y área.
5. Confirmar rechazo de perfiles de temperatura físicamente incompatibles y de dimensiones fuera de rango.
6. Abrir la aplicación sin conexión y generar/abrir el CSV en el equipo de presentación.

| Fecha / versión | Caso y método independiente | Resultado esperado | Resultado observado | Conclusión / responsable |
|---|---|---|---|---|
| Pendiente | Completar por el equipo | Pendiente | Pendiente | Pendiente |

## 7. Limitaciones conocidas y trabajo futuro

No se incluye un balance de materia/estequiometría integrado, cálculo de h_o desde el movimiento del caldo, perfil acoplado del refrigerante, pérdidas de presión helicoidales y locales, diseño hidráulico completo, transferencia de oxígeno, dinámica de control ni diseño mecánico. h_i y la caída de presión son estimaciones correlacionales con alcance limitado. Como trabajo futuro, se recomienda validar el caso de referencia con un cálculo independiente, ampliar la correlación hidráulica para hélices y documentar bibliografía aplicable.

## 8. Guion breve para la demostración

1. Mostrar que la interfaz abre localmente y explicar las entradas.
2. Presentar la base de entalpía y el balance de potencia, incluyendo agitación.
3. Modificar una variable y describir cómo cambia la carga, el área y la geometría propuesta.
4. Explicar qué representan las resistencias térmicas y cómo se estimaron los coeficientes introducidos.
5. Exportar el CSV y exponer las hipótesis, pruebas independientes y límites de uso.

## 9. Correcciones de la segunda versión

Se invirtieron los lados de transferencia: ahora el refrigerante va por dentro del tubo y el caldo por fuera, como en un serpentín real. El caudal de refrigerante pasó de dato supuesto a resultado (m = Q / (Cp ΔT)), porque antes la ΔTlm y la capacidad sensible podían ser incompatibles. El coeficiente interior se calcula con Gnielinski y corrección helicoidal. La hélice deja holgura a la pared y se valida que el paso no sea menor que el diámetro del tubo. Se quitó el `step` rígido de los campos, que invalidaba valores como 30.3 °C. Los valores por defecto se ajustaron a una carga de unos 7.6 kW. Pendiente: contrastar el caso de referencia con un cálculo manual independiente.
