# Biorreactor con serpentín helicoidal

App Android 2.0.0 basada en el archivo `BiorreactorSerpentinHelicoidal_v6.html`. La app abre esa interfaz localmente, conserva sus componentes, parámetros, cálculos y animación del modelo, y funciona sin conexión a internet. Se puede ampliar el modelo con el gesto de pellizco.

**Descargar APK:** [Biorreactor Android 2.0.0](https://github.com/eslaking289/serpentin-biorreactor/releases/download/apk-v2.0.0/ThermaBio-Android.apk).

La interfaz se encuentra en `android/app/src/main/assets/biorreactor-v6.html`; el `index.html` existente se conserva intacto. `MainActivity.java` aloja el HTML en una WebView y adapta el contenido a las barras del teléfono.

## Instalar

1. Descarga el APK en el teléfono Android.
2. Ábrelo y autoriza la instalación si Android lo solicita.

## Compilar

Requisitos: JDK 17, Android SDK 35 y Gradle 8.9.

```powershell
cd android
gradle --no-daemon assembleDebug
```

El APK se genera en `android/app/build/outputs/apk/debug/app-debug.apk`.
