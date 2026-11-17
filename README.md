# ThermaBio

Aplicación local para estimar la carga térmica de una fermentación aeróbica y dimensionar preliminarmente un serpentín de enfriamiento. Está hecha con HTML, CSS y JavaScript sin dependencias, servidor, telemetría ni APIs. Funciona sin conexión al abrir `index.html` en un navegador moderno.

**Aplicación web:** [Abrir ThermaBio](https://eslaking289.github.io/serpentin-biorreactor/)

## Inicio rápido

1. Descarga o clona este repositorio.
2. Abre `index.html` directamente en tu navegador, o abre la carpeta en VS Code y usa una extensión de servidor local si quieres recarga automática.
3. Introduce tus datos y selecciona **Actualizar diseño**. El informe se descarga como CSV.

No es necesario instalar paquetes ni compilar el proyecto. La extensión de servidor local es opcional y solo sirve los archivos de esta carpeta.

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

El usuario aporta el calor de reacción por mol de sustrato y la tasa de consumo. La fracción de calor que llega al caldo se configura explícitamente:

```text
Q_reacción = |ΔH_r| · tasa_sustrato · fracción_calor / 3600
Q_total = Q_reacción + P_agitación
Q_refrigerante = m_dot · Cp · (T_salida - T_entrada)
```

`Q` se expresa en kW si `ΔH_r` está en kJ/mol, el consumo en mol/h y la agitación en kW. Se usa `Cp = 4.18 kJ/(kg·K)` como valor aproximado para el refrigerante acuoso; cámbialo en el código si tu fluido requiere otra propiedad.

Para el área exterior del tubo, el coeficiente global se estima con:

```text
1/Uo = Do/(Di·hi) + Do·ln(Do/Di)/(2·kpared) + 1/ho + Rf
ΔTlm = (ΔTmayor - ΔTmenor) / ln(ΔTmayor / ΔTmenor)
A = Qtotal / (Uo · ΔTlm)
L = A / (π · Do)
```

La geometría idealiza el tubo como una hélice de diámetro `Dtanque - Do` y paso vertical indicado por el usuario. La longitud por vuelta es `sqrt((π·Dhélice)^2 + paso^2)`.

## Supuestos y límites

Es una herramienta educativa de dimensionamiento preliminar, no una especificación para fabricar o operar un equipo. Supone estado estacionario, temperatura uniforme del caldo, propiedades constantes, régimen térmico representado por coeficientes aportados, calor de reacción definido en una base molar consistente y resistencia de ensuciamiento total referida al área exterior. El cálculo de capacidad del refrigerante es sensible y no resuelve un balance acoplado del perfil de temperatura.

El programa no estima la entalpía de fermentación a partir de la estequiometría, ni calcula coeficientes convectivos mediante correlaciones, caída de presión, hidráulica, transferencia de masa/oxígeno, control dinámico, selección mecánica, corrosión ni restricciones sanitarias. Justifica y documenta con bibliografía el valor de cada propiedad, entalpía, coeficiente y factor elegido. Compara el diseño con balances independientes antes de usarlo en una decisión física.

## Estructura

- `index.html`: estructura de la interfaz.
- `styles.css`: presentación adaptable a escritorio y móvil.
- `app.js`: validación, cálculos y exportación CSV.
- `MEMORIA.md`: alcance, decisiones, registro de apoyo con IA y pendientes de verificación del equipo.

## Compatibilidad

Requiere un navegador actualizado con soporte para JavaScript ES2021, `Blob` y descarga de archivos. No hace solicitudes de red. El CSV puede abrirse en hojas de cálculo comunes.
