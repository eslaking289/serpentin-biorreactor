# ThermaBio Android

Aplicación Android para el balance térmico de fermentación y el dimensionamiento preliminar de un serpentín de enfriamiento. Este repositorio mantiene la versión APK; la antigua aplicación web independiente se retiró.

**Descarga:** [ThermaBio Android 1.5.8](https://github.com/eslaking289/serpentin-biorreactor/raw/refs/heads/main/android/dist/ThermaBio-Android-1.5.8.apk)

La APK calcula el balance metabólico (`Q̇ = q̇_met · V_caldo + P_agitación`), el deber de enfriamiento, la transferencia térmica, la geometría helicoidal y una estimación de caída de presión. En Resultados muestra el modelo 3D local del biorreactor, basado en el archivo HTML incluido en los recursos de la APK, con dimensiones sincronizadas con el cálculo nativo. La aplicación no solicita permiso de Internet.

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

El resultado se crea en `android/app/build/outputs/apk/debug/app-debug.apk`. El APK firmado de la carpeta `android/dist/` permite instalación directa; usa una clave de firma propia para publicar en Google Play.

## Alcance y supuestos

El modelo es educativo y preliminar, no es un plano de fabricación ni una herramienta de selección de bomba. Supone caldo isotermo, propiedades constantes y pérdidas al ambiente nulas. La transferencia interna usa Gnielinski con corrección helicoidal simplificada; en régimen laminar se conserva Nu = 3.66 de tubo recto y se advierte que no corrige el efecto Dean. La caída de presión es Darcy–Weisbach de tubo recto equivalente y no incluye curvatura ni accesorios.

Consulta [`android/BIORREACTOR.md`](android/BIORREACTOR.md) para los detalles del diagrama. Las correlaciones y propiedades deben validarse para el equipo y proceso reales.

## Verificación matemática

El motor numérico de referencia se puede comprobar con Node.js:

```powershell
node test/verify.js
```

El código de la aplicación está en `android/app/`. La interfaz, los despejes metabólicos y el diagrama paramétrico están implementados nativamente en Android; el repositorio ya no contiene páginas HTML ni vistas WebView.
