# Brailux Aprende - Roadmap

## 0. Proyecto base y compilacion

- Objetivo: mantener el proyecto Android base compilable con Kotlin, Jetpack Compose, una sola Activity y la configuracion actual.
- Resultado verificable: `gradlew.bat test` y `gradlew.bat assembleDebug` se ejecutan sin errores.
- Condicion para avanzar: el proyecto compila sin modificar `applicationId`, `namespace`, versiones de Gradle, Kotlin, Compose ni SDK.

## 1. Documentacion y arquitectura

- Objetivo: definir el producto, la arquitectura inicial y el plan de trabajo por etapas.
- Resultado verificable: existen `docs/PRODUCT_SPEC.md`, `docs/ARCHITECTURE.md` y `docs/ROADMAP.md`.
- Condicion para avanzar: la documentacion es coherente con el proyecto actual y con las reglas de desarrollo.

## 2. Motor Braille

- Objetivo: implementar la logica Braille independiente de la interfaz.
- Resultado verificable: existen modelos o funciones para representar la celda Braille, numerar sus seis puntos y validar letras iniciales.
- Condicion para avanzar: la logica Braille tiene pruebas unitarias y no depende de Compose.

## 3. Componente visual de seis puntos

- Objetivo: crear un componente Compose reutilizable para mostrar e interactuar con la celda Braille.
- Resultado verificable: el componente muestra seis puntos, comunica estado seleccionado o no seleccionado y ofrece semantica para TalkBack.
- Condicion para avanzar: el componente puede usarse en una pantalla de practica sin depender de datos remotos.

## 4. Primer recorrido funcional

- Objetivo: crear un flujo minimo para aprender la celda, identificar puntos y formar la letra A.
- Resultado verificable: una persona puede completar el ejercicio de la letra A y recibir retroalimentacion.
- Condicion para avanzar: el recorrido funciona sin conexion y conserva criterios basicos de accesibilidad.

## 5. Navegacion principal

- Objetivo: incorporar navegacion entre las secciones previstas de la aplicacion.
- Resultado verificable: Inicio, Aprende, Practica, Juega, Mi progreso y Configuracion son rutas navegables.
- Condicion para avanzar: la navegacion usa Navigation Compose y mantiene una sola Activity.

## 6. Progreso local

- Objetivo: guardar progreso y preferencias basicas en el dispositivo.
- Resultado verificable: el avance del primer recorrido se conserva al cerrar y abrir la aplicacion.
- Condicion para avanzar: la persistencia usa DataStore y no requiere cuentas ni conexion.

## 7. Contenido educativo

- Objetivo: ampliar lecciones y practicas de Braille inicial, empezando por vocales y fundamentos de la celda.
- Resultado verificable: existen contenidos educativos organizados y accesibles desde las secciones de aprendizaje.
- Condicion para avanzar: el contenido funciona offline y no rompe el recorrido minimo.

## 8. Juegos iniciales

- Objetivo: agregar juegos simples que refuercen reconocimiento de puntos y letras iniciales.
- Resultado verificable: al menos un juego educativo puede completarse y entrega retroalimentacion.
- Condicion para avanzar: el juego no requiere cuentas, clasificaciones en linea ni multijugador.

## 9. Integracion del personaje Brailux

- Objetivo: incorporar al personaje Brailux como guia pedagogico sin afectar la accesibilidad.
- Resultado verificable: Brailux aparece en momentos clave con mensajes breves y utiles.
- Condicion para avanzar: el personaje no sustituye instrucciones accesibles ni se confunde con elementos interactivos.

## 10. Accesibilidad y pruebas

- Objetivo: reforzar compatibilidad con TalkBack, pruebas unitarias y revision de flujos principales.
- Resultado verificable: los puntos Braille tienen descripcion por numero y estado, y la logica Braille esta cubierta por pruebas.
- Condicion para avanzar: los recorridos principales se pueden usar sin depender unicamente del color.

## 11. Preparacion para Google Play

- Objetivo: preparar la aplicacion para distribucion inicial.
- Resultado verificable: build de lanzamiento revisada, textos principales completos, iconografia y metadatos listos para revision.
- Condicion para avanzar: no hay funciones remotas obligatorias, anuncios, compras ni cuentas pendientes para la primera version.
