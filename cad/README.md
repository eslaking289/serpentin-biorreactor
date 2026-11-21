# Modelo CAD de ThermaBio

Abre thermabio_serpentin_parametrico.scad con OpenSCAD. El archivo usa solo primitivas y funciones incorporadas; no requiere complementos ni conexión a internet.

Las cotas están en milímetros y el volumen de caldo en litros. Para ajustar el esquema cambia los parámetros al inicio del archivo: diámetro interior y altura útil del tanque, volumen, diámetro central de la hélice, diámetro exterior y espesor del tubo, paso, número de vueltas y holguras. El modelo muestra un corte del recipiente, el nivel de líquido y el tubo con diámetro interior.

OpenSCAD permite previsualizar el modelo con F5 y generar la geometría final con F6. Para enviarlo para revisión, comparte el archivo SCAD; opcionalmente exporta también un STL desde File → Export. El SCAD conserva las cotas editables; el STL es solo la geometría resultante.

El modelo es geométrico y de visualización. No representa boquillas normalizadas, soportes, soldaduras, deflectores, contracción térmica ni esfuerzos mecánicos; no es un plano de fabricación.
