# Desarrollo de Brailux

## Enfoque de desarrollo

Brailux se desarrolla de forma modular y progresiva. La prioridad es mantener precisión en los contenidos, accesibilidad, estabilidad funcional y una experiencia comprensible antes de ampliar funciones.

## Estructura actual

La web se organiza en:

- `index.html`: portal principal de Brailux;
- `aprende.html`: contenido teórico e interactivo;
- hojas de estilo propias para cada experiencia;
- JavaScript para navegación, ejercicios, ranking de exhibición e interacción;
- módulo de accesibilidad consumido desde el repositorio raíz de Neuronova Apps.

## Accesibilidad compartida

Brailux consume el núcleo central de accesibilidad de Neuronova Apps. Esto permite mantener criterios comunes entre proyectos y evita duplicar implementaciones.

Entre las funciones compartidas se contemplan:

- tres niveles de tamaño de texto;
- alto contraste;
- espaciado de letras y palabras;
- interlineado amplio;
- lectura amigable para dislexia;
- guía de lectura;
- reducción de movimiento;
- resaltado de enlaces;
- foco de teclado reforzado.

## Criterios técnicos

Cada componente debe procurar:

- HTML semántico;
- navegación mediante teclado;
- controles nativos cuando sea posible;
- estados ARIA cuando correspondan;
- indicadores visibles de foco;
- diseño adaptable;
- compatibilidad con preferencias de movimiento reducido;
- separación entre contenido, presentación y lógica.

## Contenidos

Los contenidos Braille deben revisarse antes de incorporarse a producción. Las fuentes de referencia incluyen documentación del Consejo Iberoamericano del Braille, Comisión Braille Española y ONCE.

## Pruebas

Las pruebas deben considerar:

1. funcionamiento general;
2. precisión de signos y combinaciones;
3. navegación con teclado;
4. comprensión sin depender exclusivamente de recursos visuales;
5. comportamiento en móvil y escritorio;
6. alto contraste y ampliación de texto;
7. interacción con tecnologías de apoyo cuando sea posible.

## Estado

Desarrollo activo. La arquitectura puede evolucionar conforme se incorporen nuevas actividades y se realicen pruebas de accesibilidad y experiencia de usuario.