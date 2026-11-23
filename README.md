# ThermaBio Android

Aplicación Android que empaqueta la interfaz interactiva del biorreactor en un APK instalable. La interfaz se ejecuta desde archivos locales.

**Descarga:** [ThermaBio Android 1.6.0](https://github.com/eslaking289/serpentin-biorreactor/raw/refs/heads/main/android/dist/ThermaBio-Android-1.6.0.apk)

La APK muestra el archivo `index.html` completo como interfaz principal: modelo 3D interactivo, selección de componentes, parámetros editables, modos de vista, control de animación y cálculos propios del HTML. El archivo original se conserva dentro del APK y se carga en una vista Android local; no se abre un navegador externo ni se necesitan recursos de Internet. La app no solicita permiso de Internet.

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

El cálculo incluido en `index.html` estima enfriamiento sensible: trata cultivo y refrigerante como agua, usa un coeficiente global `U` ingresado por el usuario y compara la potencia transferida con la absorbida por el refrigerante. No calcula generación metabólica ni calor de agitación. Es una herramienta académica preliminar, no un plano de fabricación ni una selección de bomba. Consulta [`android/BIORREACTOR.md`](android/BIORREACTOR.md) para las ecuaciones y límites.

## Verificación matemática

Los módulos numéricos de referencia que conserva el repositorio se pueden comprobar con Node.js:

```powershell
node test/verify.js
```

El código de la aplicación está en `android/app/`. La interfaz se conserva en `android/app/src/main/assets/index.html`; `MainActivity.java` la abre dentro de una vista WebView local con la carga de red bloqueada.
