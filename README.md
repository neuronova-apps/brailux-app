# Brailux

Brailux es una iniciativa personal orientada a acercar el sistema Braille a más personas mediante una experiencia web accesible, interactiva y progresiva.

El proyecto busca ofrecer una alternativa digital para conocer qué es el Braille, comprender la lógica de su estructura, explorar sus signos básicos, practicar contenidos introductorios y contribuir a su difusión como sistema de lectoescritura utilizado por personas ciegas, sordociegas y con discapacidad visual grave.

Brailux forma parte del ecosistema [Neuronova Apps](https://neuronova-apps.github.io/) y se desarrolla como un proyecto personal e independiente de Gabriel Berrospi.

## Propósito

Brailux pretende facilitar un primer acercamiento al Braille mediante recursos digitales que combinen información, representación visual e interacción.

La aplicación está pensada para personas que desean conocer el sistema por primera vez, docentes, estudiantes, familiares, voluntarios y otros usuarios interesados en comprender sus fundamentos.

No sustituye procesos formales de alfabetización Braille, capacitación especializada, materiales táctiles ni la intervención de profesionales del área. Se plantea como un recurso complementario de aprendizaje, sensibilización y difusión.

## Base teórica

El sistema Braille fue desarrollado por Louis Braille en 1825. Su estructura se basa en una celda de seis puntos distribuidos en dos columnas verticales de tres puntos cada una.

En lectura, los puntos se numeran de la siguiente manera:

```text
1 4
2 5
3 6
```

La presencia o ausencia de puntos permite construir los distintos caracteres. Matemáticamente existen 64 configuraciones posibles si se incluye la celda vacía; 63 configuraciones contienen al menos un punto.

El sistema permite representar letras, números, signos de puntuación y expresión, contenidos matemáticos y científicos, notación musical y otros códigos especializados.

## Lógica de formación del alfabeto

Una de las características más interesantes del Braille es que sus signos no fueron organizados de manera aleatoria. Louis Braille estructuró el sistema mediante series que reutilizan patrones y añaden puntos de forma progresiva.

### Primera serie

Las letras de la A a la J se construyen combinando únicamente los cuatro puntos superiores de la celda: 1, 2, 4 y 5.

```text
A B C D E F G H I J
```

Esta primera serie constituye la base para varias de las series siguientes.

### Segunda serie

Las letras de la K a la T mantienen las mismas configuraciones de la primera serie y añaden el punto 3, situado en la parte inferior izquierda.

```text
Primera serie + punto 3 = K a T
```

Por ejemplo:

```text
A = punto 1
K = puntos 1-3

B = puntos 1-2
L = puntos 1-2-3
```

### Tercera serie

La tercera serie parte de la segunda y añade el punto 6, situado en la parte inferior derecha.

De esta serie surgen, entre otros signos, las letras U, V, X, Y y Z, así como otras configuraciones utilizadas en diferentes lenguas y signos del Braille español.

```text
Segunda serie + punto 6 = tercera serie
```

### Otras series

La organización original del sistema comprende siete series. Las series posteriores permiten obtener otras letras, signos de puntuación, signos de expresión, símbolos matemáticos y configuraciones utilizadas en diferentes códigos.

Brailux prioriza inicialmente la comprensión de las primeras tres series por su relación directa con la formación del alfabeto y posteriormente incorpora signos necesarios para el Braille integral en español.

## Braille integral en español

Además del alfabeto básico, Brailux contempla progresivamente los grafemas y signos necesarios para el español, entre ellos:

- letra Ñ;
- vocales acentuadas Á, É, Í, Ó y Ú;
- Ü;
- signo de mayúscula;
- signo de número;
- signos de puntuación y expresión.

En Braille las vocales acentuadas poseen configuraciones propias. La mayúscula se representa mediante un signo diferenciador colocado antes de la letra. Los números utilizan las configuraciones de las letras A-J precedidas por el signo de número.

## Lectura y escritura

La lectura Braille se realiza de izquierda a derecha.

Cuando se utiliza una pauta o regleta convencional negativa junto con un punzón, la escritura se realiza sobre el reverso del papel. Por ello se escribe de derecha a izquierda y con la disposición de puntos en espejo. Después se gira la hoja para realizar la lectura de izquierda a derecha.

En este modo de escritura el punto 1 se encuentra a la derecha desde la perspectiva de quien está escribiendo sobre el reverso.

Las máquinas Braille, líneas Braille y regletas positivas permiten escribir en el mismo sentido de la lectura, de izquierda a derecha.

## Componentes de aprendizaje

La aplicación está organizada para incorporar progresivamente cinco áreas principales:

### Conoce

Introducción al sistema Braille, historia, función, importancia y usos actuales.

### Comprende

Explicación del signo generador, numeración de puntos, lógica de las series y construcción de los caracteres.

### Explora

Visualización interactiva del alfabeto, números, signos y configuraciones Braille.

### Practica

Ejercicios para reconocer signos, identificar puntos, construir caracteres y relacionar representaciones visuales con su estructura Braille.

### Tecnología

Introducción al Braille digital y a herramientas como líneas Braille, teclados estilo Perkins, impresoras Braille y teclados virtuales.

## Habilidades vinculadas al aprendizaje táctil

Brailux reconoce que aprender el código visualmente no equivale al proceso de alfabetización táctil de una persona usuaria de Braille.

Por ello, el contenido también contempla orientaciones sobre aspectos relacionados con la adquisición del sistema, como discriminación táctil, coordinación bimanual, orientación espacial, lateralidad, postura y ergonomía.

Estas dimensiones se presentan con finalidad informativa y no pretenden ser sustitutos de una metodología especializada de enseñanza.

## Funciones interactivas

La versión inicial contempla:

- constructor interactivo del signo generador;
- activación y desactivación de los seis puntos;
- identificación de combinaciones conocidas;
- exploración de las series del alfabeto;
- visualización de cada letra con sus puntos correspondientes;
- comparación entre lectura y escritura con pauta y punzón;
- representación de números y signos diferenciadores;
- ejercicios de reconocimiento de letras;
- conversor didáctico básico de texto a representación Braille;
- contenidos sobre tecnologías relacionadas con Braille.

Las funciones serán ampliadas y revisadas conforme avance el desarrollo.

## Accesibilidad

La accesibilidad constituye uno de los ejes centrales de Brailux. La aplicación busca incorporar progresivamente:

- estructura semántica clara;
- navegación mediante teclado;
- indicadores visibles de foco;
- contraste suficiente;
- opción de alto contraste;
- compatibilidad con lectores de pantalla;
- controles con estados accesibles mediante ARIA;
- diseño adaptable a dispositivos móviles;
- respeto a la preferencia de movimiento reducido del sistema operativo;
- textos alternativos y descripciones para elementos visuales relevantes.

La accesibilidad será revisada de manera continua durante el desarrollo.

## Alcance

Brailux es una aplicación introductoria y educativa. Su desarrollo se concentra inicialmente en el Braille integral, los fundamentos de lectoescritura y la difusión del sistema.

Códigos especializados como matemáticas avanzadas, ciencias, música, fonética o estenografía requieren normas específicas y solo serán incorporados cuando puedan desarrollarse con el rigor técnico correspondiente.

## Referencias técnicas y didácticas

Los contenidos introductorios del proyecto toman como referencia documentación especializada del sistema Braille, principalmente:

- Consejo Iberoamericano del Braille. "Enseñanza del sistema Braille a personas con visión". Agosto de 2024.
- Consejo Iberoamericano del Braille. Publicaciones técnicas y criterios pedagógicos para la enseñanza del Braille.
- Comisión Braille Española. Documentos técnicos vigentes relacionados con Braille.
- Museo Tiflológico de la ONCE. Material histórico y documental sobre el sistema Braille y sus herramientas de escritura.

Referencias:

- https://www.once.es/servicios-sociales/braille/consejo-iberoamericano/publicaciones-tecnicas-del-consejo-iberoamericano-del-braille-cib
- https://www.once.es/servicios-sociales/braille/comision-braille-espanola/documentos-tecnicos
- https://museo.once.es/exposiciones/exposiciones-temporales-pasadas/2025/250-anos-del-sistema-braille

## Desarrollo

Brailux se encuentra en desarrollo activo. Los contenidos, funciones y componentes de interfaz pueden modificarse conforme se realicen pruebas de funcionamiento, accesibilidad, precisión de los contenidos y experiencia de usuario.

## Ecosistema

Brailux forma parte de [Neuronova Apps](https://neuronova-apps.github.io/), plataforma matriz que reúne diferentes aplicaciones web desarrolladas como proyectos independientes.

## Autoría

Proyecto personal desarrollado por Gabriel Berrospi.

## Estado

En desarrollo.
