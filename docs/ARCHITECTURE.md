# Brailux Aprende - Arquitectura

## Enfoque general

Brailux Aprende usara una arquitectura sencilla, local y facil de mantener. La aplicacion debe conservar una sola Activity, usar Kotlin y Jetpack Compose, y separar la logica Braille de la interfaz para que pueda probarse con pruebas unitarias.

## Base tecnica actual

- Android con Kotlin.
- Jetpack Compose habilitado.
- Una sola Activity: `MainActivity`.
- `applicationId` y `namespace`: `com.brailuxaprende`.
- Version minima de Android: `minSdk = 24`.

## Capas previstas

### ui

Contendra pantallas, componentes reutilizables, navegacion y tema visual. Las pantallas Compose deben incluir semantica accesible para TalkBack, areas tactiles adecuadas y retroalimentacion textual cuando el estado cambie.

### data

Contendra el almacenamiento local del progreso y las preferencias. Cuando se implemente persistencia, se usara DataStore para guardar datos simples del usuario en el dispositivo.

### braille

Paquete independiente para la logica Braille. Debe contener reglas, modelos y validaciones relacionadas con la celda Braille, letras y ejercicios. Esta logica no debe depender de Compose ni de Android UI para permitir pruebas unitarias directas.

## Estado de pantalla

Los ViewModel se usaran unicamente cuando exista estado de pantalla que deba sobrevivir recomposiciones o coordinar interacciones de una pantalla. No deben agregarse por defecto si una funcion o componente Compose simple es suficiente.

## Navegacion

La navegacion principal se implementara con Navigation Compose cuando el proyecto necesite cambiar entre secciones como Inicio, Aprende, Practica, Juega, Mi progreso y Configuracion.

## Identidad institucional

`InstitutionalIdentity` es la fuente unica para el nombre de NeuroNova Apps, el sitio de la matriz, el sitio de Brailux Aprende y el credito de Gabriel Berrospi. La politica de privacidad permanece sin URL hasta que exista una direccion oficial.

## Persistencia

El progreso y las preferencias se guardaran localmente con DataStore cuando se implemente esa etapa. No habra cuentas, sincronizacion remota ni dependencias de servidor.

## Dependencias excluidas en la primera version

La primera version no usara:

- Hilt.
- Room.
- Firebase.
- Servicios remotos.
- Una capa de dominio separada.

## Pruebas

La logica Braille debe tener pruebas unitarias. Las pruebas iniciales deben cubrir, como minimo, la numeracion de puntos, la representacion de la celda Braille y las letras o ejercicios que se incorporen.

## Accesibilidad

La interfaz debe incluir semantica para TalkBack. Cada punto Braille debe poder identificarse por numero y estado, y los ejercicios deben entregar retroalimentacion visual y textual.

## Estructura futura propuesta

Esta estructura se propone para etapas posteriores. No es necesario crear estas carpetas hasta que exista codigo para ellas.

```text
app/src/main/java/com/brailuxaprende/
- braille/
- data/
- ui/components/
- ui/navigation/
- ui/screens/
- ui/theme/
```
