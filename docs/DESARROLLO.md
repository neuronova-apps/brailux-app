# Desarrollo de Brailux

## Enfoque de desarrollo

Brailux se desarrolla de forma modular y progresiva. La prioridad es mantener precisión en los contenidos, accesibilidad, estabilidad funcional y una experiencia comprensible antes de ampliar funciones.

## Estructura actual

La web se organiza en:

- `index.html`: portal principal y accesos a las rutas de Brailux;
- `portal.css`: estilos exclusivos del portal principal;
- `portal.js`: navegación móvil, ranking de exhibición y carga del módulo compartido de accesibilidad en el portal;
- `aprende.html`: contenido teórico e interactivo de aprendizaje;
- `styles.css`: estilos de la experiencia Aprende;
- `script.js`: constructor Braille, series, prácticas y demás interacciones didácticas;
- `privacy/index.html`: política de privacidad;
- `privacy/styles.css`: estilos exclusivos de privacidad;
- `sitemap.xml`: URLs públicas indexables;
- `favicon.svg`: identidad gráfica del sitio;
- `docs/`: documentación de proyecto, alcance, desarrollo y roadmap;
- `.nojekyll`: publicación estática directa mediante GitHub Pages.

Esta separación evita mezclar la portada de navegación con la experiencia didáctica y con la política de privacidad.

## Accesibilidad compartida

Brailux consume el núcleo central de accesibilidad de Neuronova Apps. Esto permite mantener criterios comunes entre proyectos y evita duplicar implementaciones generales.

`aprende.html` y la política de privacidad declaran directamente los recursos compartidos. El portal principal los carga actualmente desde `portal.js`; esta diferencia puede simplificarse en una revisión posterior cuando el cambio pueda verificarse sin alterar el funcionamiento publicado.

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

La experiencia Aprende conserva además un control local de alto contraste. Su eventual eliminación debe realizarse únicamente después de comprobar que el módulo compartido cubre todos los estados visuales específicos de Brailux.

## Criterios técnicos

Cada componente debe procurar:

- HTML semántico;
- navegación mediante teclado;
- controles nativos cuando sea posible;
- estados ARIA cuando correspondan;
- indicadores visibles de foco;
- diseño adaptable;
- compatibilidad con preferencias de movimiento reducido;
- separación entre contenido, presentación y lógica;
- evitar estilos o scripts duplicados cuando ya exista una responsabilidad claramente asignada a otro archivo.

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
