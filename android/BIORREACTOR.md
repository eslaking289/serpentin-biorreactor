# Modelo del biorreactor en Android

El diagrama técnico parametrizado aparece al inicio de **Resultados** y recibe los valores actuales de la calculadora Android. En la APK se muestra una vista frontal esquemática sin animaciones ni controles secundarios.

La figura se dibuja con Canvas en una vista Android nativa de altura fija. Se redibuja al actualizar un cálculo válido y se limpia si las entradas dejan de ser válidas. No hay WebView, animaciones ni medición dinámica del contenido; eso elimina los reajustes que causaban el parpadeo y el espacio vacío.

## Balance térmico y dimensionamiento

La carga se calcula con el balance metabólico:

`Q̇ = q̇_met · V_caldo + P_agitación`

La calculadora permite despejar cualquiera de esas entradas con las otras variables y la carga total conocida. El refrigerante circula por el interior del tubo; el caldo se considera perfectamente mezclado y a temperatura constante.

`ΔT₁ = T_caldo − T_salida`  ·  `ΔT₂ = T_caldo − T_entrada`

`ΔT_lm = (ΔT₂ − ΔT₁) / ln(ΔT₂ / ΔT₁)`

`ṁ_req = Q̇ / [Cp · (T_salida − T_entrada)]`

`A_o = 1000 Q̇ / (U_o ΔT_lm)`  ·  `L = A_o / (π D_o)`

El número de vueltas y la altura se estiman con el diámetro de hélice y el paso ingresados. Los resultados se actualizan al cambiar datos y se muestran en la tarjeta siguiente al diagrama.

## Límites del modelo

- Es una estimación académica preliminar, no un plano de fabricación ni una selección de bomba.
- Supone propiedades constantes, caldo isotermo y pérdidas de calor al ambiente iguales a cero.
- La corrección helicoidal del coeficiente interior es simplificada. En flujo laminar, `Nu = 3.66` es la referencia de tubo recto y no corrige el efecto Dean.
- La caída de presión usa Darcy–Weisbach de tubo recto equivalente y omite curvatura y pérdidas locales.
- La figura es conceptual; no representa cabezales, boquillas normalizadas, soldaduras, soportes ni validación estructural.

Verifica las propiedades, el comportamiento hidráulico, los materiales, la geometría y las condiciones de proceso con referencias apropiadas antes de usar los resultados en un equipo real.
