# Brailux Aprende

## Configuracion actual del proyecto

- `applicationId`: `com.brailuxaprende`.
- `namespace`: `com.brailuxaprende`.
- Version minima de Android: `minSdk = 24`.
- Proyecto Android con Kotlin: existen fuentes `.kt` en `app/src/main/java` y `app/src/test/java`.
- Jetpack Compose: `buildFeatures { compose = true }` y dependencias Compose configuradas en `app/build.gradle.kts`.
- Kotlin DSL: configuracion Gradle en `settings.gradle.kts`, `build.gradle.kts` y `app/build.gradle.kts`.
- Una sola Activity declarada actualmente: `.MainActivity`.
- Comandos disponibles para verificacion:
  - `gradlew.bat test`
  - `gradlew.bat assembleDebug`

## Proposito

Aplicacion Android educativa y accesible para aprender el sistema Braille mediante lecciones, practicas y juegos.

## Principios

- Funcionar completamente sin conexion.
- No requerir cuentas de usuario.
- No utilizar Firebase, servidores ni servicios remotos.
- Guardar el progreso localmente.
- Mantener compatibilidad con TalkBack.
- No depender unicamente del color para comunicar estados.

## Tecnologia

- Kotlin.
- Jetpack Compose.
- Kotlin DSL.
- Una sola Activity.
- Arquitectura sencilla y facil de mantener.
- Logica Braille separada de la interfaz.
- DataStore para el progreso y las preferencias cuando se implemente.

## Reglas de desarrollo

- Mantener el applicationId y namespace actuales.
- No actualizar Gradle, Kotlin, Compose ni el SDK sin autorizacion.
- No agregar dependencias sin justificar su necesidad.
- No utilizar Hilt, Room, Firebase ni servicios remotos en la primera version.
- Realizar cambios pequenos y verificables.
- No modificar archivos que no correspondan a la tarea.
- Crear componentes reutilizables.
- Agregar pruebas unitarias para la logica Braille.
- Mantener los textos visibles en recursos `strings.xml` cuando corresponda.

## Accesibilidad

- Proporcionar semantica para TalkBack.
- Identificar cada punto Braille mediante su numero y estado.
- Mantener areas tactiles adecuadas.
- Ofrecer retroalimentacion visual y textual.
- Evitar que elementos decorativos se confundan con los seis puntos.

## Verificacion

Despues de modificar codigo, ejecutar:

```powershell
gradlew.bat test
gradlew.bat assembleDebug
```

Si algun comando falla, informar el error y no afirmar que la tarea quedo completada.

## Informe final

Indicar unicamente:

- archivos modificados;
- funcion realizada;
- pruebas ejecutadas;
- resultado de compilacion;
- problemas pendientes.
