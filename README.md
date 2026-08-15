# Brailux

Brailux es una aplicación educativa de Neuronova Apps orientada a conocer, comprender y practicar los fundamentos del sistema Braille mediante una experiencia web accesible e interactiva.

## Estado del proyecto

- **Web:** MVP funcional en desarrollo activo.
- **Publicación:** disponible mediante GitHub Pages.
- **Android:** existe una rama `android` separada para el desarrollo móvil. Se considera trabajo en progreso y no una versión estable o publicada.

## Funciones disponibles

- introducción al sistema Braille y al signo generador de seis puntos;
- explicación de la numeración y lógica de formación de caracteres;
- exploración del alfabeto y signos básicos;
- práctica de reconocimiento y construcción de caracteres;
- comparación didáctica entre lectura y escritura con pauta y punzón;
- representación de números y signos diferenciadores;
- conversor didáctico básico de texto a representación Braille;
- contenidos sobre tecnologías relacionadas con Braille;
- diseño responsive e integración con la accesibilidad compartida de Neuronova Apps.

Brailux es un recurso introductorio y complementario. No sustituye alfabetización Braille formal, materiales táctiles ni enseñanza especializada.

## Tecnología

La versión web utiliza una arquitectura estática y sin proceso de compilación obligatorio:

- HTML5;
- CSS3;
- JavaScript en el navegador;
- GitHub Pages;
- recursos SVG/PNG para identidad y metadatos sociales;
- módulo de accesibilidad compartido de Neuronova Apps.

## Accesibilidad

La accesibilidad es un eje central del proyecto. La interfaz contempla estructura semántica, navegación por teclado, foco visible, contraste, diseño adaptable, controles con estados accesibles, compatibilidad progresiva con lectores de pantalla y respeto por `prefers-reduced-motion`.

Estas medidas no constituyen una certificación WCAG. La aplicación continúa sujeta a revisión manual con tecnologías de asistencia.

## Privacidad

La política pública está disponible en:

https://neuronova-apps.github.io/brailux-app/privacy/

La política debe mantenerse alineada con cualquier cambio futuro que incorpore cuentas, servicios externos, analítica, sincronización u otro tratamiento de datos.

## Desarrollo local

La versión web puede ejecutarse con cualquier servidor HTTP estático. Por ejemplo:

```bash
git clone https://github.com/neuronova-apps/brailux-app.git
cd brailux-app
python3 -m http.server 8000
```

Después abre `http://localhost:8000` en el navegador.

La rama `main` corresponde a la versión web pública. El trabajo móvil se mantiene separado en la rama `android`.

## Estructura principal

- `index.html`: portada y estructura principal;
- `aprende.html`: contenido y práctica educativa;
- `portal.css` y hojas de estilo complementarias: presentación;
- `portal.js` y scripts de aprendizaje: interacción;
- `assets/`: recursos gráficos y sociales;
- `privacy/`: política de privacidad pública;
- `.nojekyll`: publicación estática mediante GitHub Pages.

## Enlaces

- **Web:** https://neuronova-apps.github.io/brailux-app/
- **Privacidad:** https://neuronova-apps.github.io/brailux-app/privacy/
- **Repositorio:** https://github.com/neuronova-apps/brailux-app
- **Ecosistema:** https://neuronova-apps.github.io/

## Neuronova Apps

Brailux forma parte de **Neuronova Apps**, ecosistema que reúne proyectos web independientes relacionados con aprendizaje, accesibilidad, bienestar, espiritualidad y entretenimiento bajo una identidad visual y técnica común.

## Autoría

Proyecto personal e independiente desarrollado por Gabriel Berrospi dentro del ecosistema Neuronova Apps.
