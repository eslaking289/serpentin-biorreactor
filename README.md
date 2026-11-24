# ThermaBio Android

Aplicación Android que empaqueta la interfaz interactiva del biorreactor en un APK instalable. La interfaz se ejecuta desde archivos locales.

**Descarga:** [ThermaBio Android 1.7.3](https://github.com/eslaking289/serpentin-biorreactor/releases/download/apk-v1.7.3/ThermaBio-Android.apk).

La APK une las pantallas de ThermaBio (calculadora metabólica con despejes, resultados y metodología) con el modelo 3D interactivo. Resultados y Modelo 3D comparten la misma vista y los resultados nativos actualizan su geometría y condiciones térmicas. La pantalla Modelo 3D incluye accesos directos a Modelo, Componentes, Parámetros y Cálculos, además de zoom con pellizco y botones. La vista se carga cuando se abre por primera vez; la app funciona desde recursos locales, sin navegador externo ni permiso de Internet.

## Instalar

1. Descarga el APK en tu teléfono Android.
2. Ábrelo y autoriza la instalación si Android lo solicita.
3. La aplicación calcula localmente; no necesita conexión después de instalarse.

## Compilar desde el código

Requisitos: JDK 17, Android SDK 35 y Gradle 8.9.

```powershell
cd android
gradle --offline --no-daemon assembleDebug
```

El resultado se crea en `android/app/build/outputs/apk/debug/app-debug.apk`. La carpeta `android/dist/` contiene el APK instalable de esta versión.

## Alcance y supuestos

La calculadora principal estima el enfriamiento sensible con las entradas térmicas. El botón **Análisis biológico y de energía** abre el módulo opcional para cinética Monod batch inicial, OUR/OTR, calor metabólico por el método seleccionado, agitación, transferencia del lado del caldo, balance transitorio y caudal requerido de refrigerante. El análisis es complementario y no introduce parámetros biológicos por especie.

El módulo reutilizable `bioprocess-core.js` calcula Monod con limitación opcional por oxígeno y muerte celular, consumo de sustrato, OUR/OTR, calor con ΔH firmado o equivalente de oxígeno, fracción de potencia de agitación convertida en calor, balance transitorio y refrigerante. No incluye una base de datos biológica ni valores ficticios: los datos de literatura requieren referencia y condiciones experimentales. Es un módulo JavaScript sin DOM; sus unidades de entrada están indicadas en sus claves y se verifica con `node test/bioprocess-verify.js`.

## Verificación matemática

Los módulos numéricos de referencia que conserva el repositorio se pueden comprobar con Node.js:

```powershell
node test/verify.js
```

El código de Android está en `android/app/`. La calculadora usa vistas nativas Java; el modelo conserva el `index.html` original de la versión 1.6.1 y se adapta al contenedor Android con estilos inyectados desde Java, sin cambiar ese archivo.
