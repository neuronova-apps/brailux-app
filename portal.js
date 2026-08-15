const alignmentStylesheet = document.createElement('link');
alignmentStylesheet.rel = 'stylesheet';
alignmentStylesheet.href = 'ecosystem-alignment.css';
document.head.appendChild(alignmentStylesheet);

const menuButton = document.querySelector('.menu-button');
const mainNav = document.querySelector('.main-nav');
const year = document.querySelector('#year');

const innerOrbitLabels = {
  '.chip-a b': 'CELDA 2×3',
  '.chip-b b': 'LECTURA',
  '.chip-c b': 'ESCRITURA'
};

Object.entries(innerOrbitLabels).forEach(([selector, label]) => {
  const element = document.querySelector(selector);
  if (element) element.textContent = label;
});

if (year) {
  year.textContent = new Date().getFullYear();
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
