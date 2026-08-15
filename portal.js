const alignmentStylesheet = document.createElement('link');
alignmentStylesheet.rel = 'stylesheet';
alignmentStylesheet.href = 'ecosystem-alignment.css';
document.head.appendChild(alignmentStylesheet);

const menuButton = document.querySelector('.menu-button');
const mainNav = document.querySelector('.main-nav');

const innerOrbitLabels = {
  '.chip-a b': 'CELDA 2×3',
  '.chip-b b': 'LECTURA',
  '.chip-c b': 'ESCRITURA'
};

Object.entries(innerOrbitLabels).forEach(([selector, label]) => {
  const element = document.querySelector(selector);
  if (element) element.textContent = label;
});

/* Footer definitivo del ecosistema Neuronova, adaptado a Brailux. */
const footerMain = document.querySelector('.footer-main');
const footerBottom = document.querySelector('.footer-bottom');

if (footerMain) {
  footerMain.innerHTML = `
    <div>
      <a class="brand" href="#inicio" aria-label="Brailux, volver al inicio">
        <span class="brand-cell" aria-hidden="true"><i></i><i></i><i></i><i></i><i></i><i></i></span>
        <span><strong>Brailux</strong><small>by Neuronova Apps</small></span>
      </a>
      <p>Aplicación educativa orientada a conocer, aprender y practicar el sistema Braille mediante una experiencia web accesible.</p>
    </div>
    <div>
      <h2>Explorar</h2>
      <a href="aprende.html">Aprende</a>
      <a href="aprende.html#practica">Practica</a>
      <a href="#recursos">Recursos</a>
      <a href="aprende.html#ruta-aprendizaje">Progreso</a>
      <a href="https://github.com/neuronova-apps/brailux-app" target="_blank" rel="noopener noreferrer">GitHub</a>
    </div>
    <div>
      <h2>Contacto</h2>
      <a href="mailto:berm_km@hotmail.com">berm_km@hotmail.com</a>
      <span>Pucallpa, Ucayali · Perú</span>
      <span>Proyecto independiente</span>
    </div>`;
}

if (footerBottom) {
  footerBottom.innerHTML = `
    <p>© 2026 Brailux · Neuronova Apps</p>
    <p><a href="privacy/">Política de privacidad</a></p>`;
}

if (menuButton && mainNav) {
  menuButton.addEventListener('click', () => {
    const open = mainNav.classList.toggle('open');
    menuButton.setAttribute('aria-expanded', String(open));
  });

  mainNav.querySelectorAll('a').forEach(link => {
    link.addEventListener('click', () => {
      mainNav.classList.remove('open');
      menuButton.setAttribute('aria-expanded', 'false');
    });
  });
}
