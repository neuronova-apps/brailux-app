(() => {
  if (!document.querySelector('link[data-neuronova-a11y]')) {
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'https://neuronova-apps.github.io/assets/accessibility/accessibility.css';
    link.dataset.neuronovaA11y = 'true';
    document.head.appendChild(link);
  }

  if (!window.NeuronovaA11y && !document.querySelector('script[data-neuronova-a11y]')) {
    const script = document.createElement('script');
    script.src = 'https://neuronova-apps.github.io/assets/accessibility/accessibility.js';
    script.dataset.neuronovaA11y = 'true';
    document.head.appendChild(script);
  }
})();

const menuButton = document.querySelector('.menu-button');
const mainNav = document.querySelector('.main-nav');
const year = document.querySelector('#year');
const tabs = [...document.querySelectorAll('.rank-tab')];
const rows = [...document.querySelectorAll('.leader-row')];
const status = document.querySelector('#rankingStatus');

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

function renderRanking(view) {
  const key = view === 'total' ? 'total' : 'week';
  const ordered = [...rows].sort((a, b) => Number(b.dataset[key]) - Number(a.dataset[key]));

  ordered.forEach((row, index) => {
    row.querySelector('.place').textContent = String(index + 1);
    row.querySelector('.xp').textContent = `${Number(row.dataset[key]).toLocaleString('es-PE')} XP`;
    row.classList.remove('first', 'second', 'third');

    if (index === 0) row.classList.add('first');
    if (index === 1) row.classList.add('second');
    if (index === 2) row.classList.add('third');

    row.parentElement.appendChild(row);
  });

  if (status) {
    status.textContent = view === 'total'
      ? 'Mostrando XP total de demostración.'
      : 'Mostrando ranking semanal de demostración.';
  }
}

tabs.forEach(tab => {
  tab.addEventListener('click', () => {
    tabs.forEach(item => {
      const active = item === tab;
      item.classList.toggle('active', active);
      item.setAttribute('aria-pressed', String(active));
    });

    renderRanking(tab.dataset.view);
  });
});
