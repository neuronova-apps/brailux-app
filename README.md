# Brailux

Brailux es una aplicación educativa de Neuronova Apps orientada a conocer, comprender y practicar los fundamentos del sistema Braille mediante una experiencia web accesible e interactiva.

## Estado del proyecto

- **Web:** MVP funcional en desarrollo activo.
- **Publicación:** disponible mediante GitHub Pages.
- **Android:** rama `android` separada en trabajo en progreso; no es una versión estable ni publicada.

## Alcance actual

Brailux funciona como recurso introductorio y complementario para comprender la celda Braille, la formación de caracteres y algunos principios de lectura y escritura. No sustituye alfabetización Braille formal, materiales táctiles ni enseñanza especializada.

## Funciones disponibles

- introducción al sistema Braille y al signo generador de seis puntos;
- explicación de numeración y lógica de formación de caracteres;
- exploración del alfabeto y signos básicos;
- práctica de reconocimiento y construcción;
- comparación didáctica entre lectura y escritura con pauta y punzón;
- representación de números y signos diferenciadores;
- conversor didáctico básico de texto a representación Braille;
- contenidos sobre tecnologías relacionadas con Braille;
- diseño responsive e integración con la accesibilidad compartida de Neuronova Apps.

## Tecnología

La versión web utiliza HTML5, CSS3, JavaScript en el navegador, GitHub Pages, recursos SVG/PNG y el módulo compartido de accesibilidad de Neuronova Apps. La arquitectura es estática y no requiere un proceso de compilación obligatorio.

## Accesibilidad

La interfaz contempla estructura semántica, navegación por teclado, foco visible, contraste, diseño adaptable, controles con estados accesibles, compatibilidad progresiva con lectores de pantalla y respeto por `prefers-reduced-motion`.

La superficie pública forma parte de la auditoría automática central del ecosistema. Estas medidas no constituyen una certificación WCAG y continúan sujetas a pruebas manuales con tecnologías de asistencia.

## Privacidad

La política pública está disponible en https://neuronova-apps.github.io/brailux-app/privacy/.

La política debe mantenerse alineada con cualquier cambio futuro que incorpore cuentas, servicios externos, analítica, sincronización u otro tratamiento de datos.

## Limitaciones conocidas

La experiencia web no reemplaza el aprendizaje táctil real del Braille. Las rutas de práctica y el sistema de progreso todavía pueden ampliarse. La revisión manual completa de accesibilidad permanece pendiente y la rama Android no constituye una aplicación publicada.

## Roadmap

Las prioridades son ampliar las rutas de práctica, consolidar un sistema de progreso más completo, reforzar los contenidos pedagógicos y completar validaciones manuales con teclado, lectores de pantalla, zoom y dispositivos.

## Desarrollo local

```bash
git clone https://github.com/neuronova-apps/brailux-app.git
cd brailux-app
python3 -m http.server 8000
```

Después abre `http://localhost:8000`. La rama `main` corresponde a la web pública y `android` mantiene el desarrollo móvil separado.

## Estructura principal

- `index.html`: portada y estructura principal;
- `aprende.html`: contenido y práctica educativa;
- `portal.css` y hojas complementarias: presentación;
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

Brailux forma parte de Neuronova Apps y comparte una base común de identidad, accesibilidad, privacidad, documentación y publicación, conservando su repositorio y evolución independientes.

## Autoría

Proyecto personal e independiente desarrollado por Gabriel Berrospi dentro del ecosistema Neuronova Apps.

## Última revisión

2026-08-15
