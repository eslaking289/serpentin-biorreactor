# ThermaBio Android

Aplicación Android que empaqueta la interfaz interactiva del biorreactor en un APK instalable. La interfaz se ejecuta desde archivos locales.

**Descarga:** [ThermaBio Android 1.6.1](https://github.com/eslaking289/serpentin-biorreactor/releases/download/apk-v1.6.1/ThermaBio-Android.apk)

La APK une las pantallas nativas de ThermaBio (calculadora metabólica con despejes, resultados y metodología) con el `index.html` original como sección **Modelo 3D**. En Resultados se conserva el diagrama sincronizado con las entradas nativas; en Modelo 3D están disponibles también los controles, parámetros y cálculos propios del HTML. Ambas partes se cargan desde recursos locales dentro de la misma app, sin navegador externo ni permiso de Internet.

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

El código de la aplicación está en `android/app/`. La interfaz HTML se conserva en `android/app/src/main/assets/index.html`; `MainActivity.java` la abre en la sección Modelo 3D con la carga de red bloqueada.
