import { chromium } from 'playwright';
import AxeBuilder from '@axe-core/playwright';
import { mkdir, writeFile } from 'node:fs/promises';

const baseURL = process.env.SMOKE_BASE_URL || 'http://127.0.0.1:4173/';
const artifactsDir = 'artifacts/phase3';
const blockingImpacts = new Set(['critical', 'serious']);
const browser = await chromium.launch({ headless: true });
const failures = [];

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

async function saveFailure(page, label, details) {
  await mkdir(artifactsDir, { recursive: true });
  await page.screenshot({ path: `${artifactsDir}/${label}.png`, fullPage: true }).catch(() => {});
  await writeFile(`${artifactsDir}/${label}.json`, JSON.stringify(details, null, 2), 'utf8').catch(() => {});
}

function summarizeViolation(violation) {
  return {
    id: violation.id,
    impact: violation.impact,
    help: violation.help,
    helpUrl: violation.helpUrl,
    nodes: violation.nodes.map(node => ({ target: node.target, failureSummary: node.failureSummary }))
  };
}

async function runAxe(label, url, viewport, setup) {
  const context = await browser.newContext({ viewport, reducedMotion: 'reduce' });
  const page = await context.newPage();
  try {
    const response = await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 30_000 });
    assert(response && response.status() < 400, `${label}: la página no cargó correctamente.`);
    await page.waitForTimeout(500);
    if (setup) await setup(page);

    const results = await new AxeBuilder({ page }).analyze();
    const blocking = results.violations.filter(item => blockingImpacts.has(item.impact));
    const advisory = results.violations.filter(item => !blockingImpacts.has(item.impact));

    console.log(`✓ axe ${label}: ${blocking.length} bloqueantes, ${advisory.length} informativas`);
    advisory.forEach(item => console.log(`  · ${item.impact || 'sin impacto'} ${item.id}: ${item.help}`));

    if (blocking.length) {
      const details = blocking.map(summarizeViolation);
      await saveFailure(page, `axe-${label}`, details);
      failures.push({ label: `axe-${label}`, errors: details.map(item => `${item.impact} ${item.id}: ${item.help}`) });
      console.error(`✗ axe ${label}`);
      details.forEach(item => console.error(`  - ${item.impact} ${item.id}: ${item.help}`));
    }
  } catch (error) {
    await saveFailure(page, `axe-${label}-exception`, { error: error.message });
    failures.push({ label: `axe-${label}`, errors: [error.message] });
    console.error(`✗ axe ${label}: ${error.message}`);
  } finally {
    await context.close();
  }
}

async function runFunctionalFlow() {
  const context = await browser.newContext({ viewport: { width: 1280, height: 800 }, reducedMotion: 'reduce' });
  const page = await context.newPage();
  try {
    await page.goto(baseURL, { waitUntil: 'domcontentloaded', timeout: 30_000 });
    await page.getByRole('link', { name: /Comenzar a aprender/i }).click();
    await page.waitForURL(url => url.pathname.endsWith('/aprende.html'));

    const dot1 = page.locator('#brailleBuilder [data-dot="1"]');
    const dot2 = page.locator('#brailleBuilder [data-dot="2"]');
    await dot1.click();
    await dot2.click();
    assert(await dot1.getAttribute('aria-pressed') === 'true', 'El punto Braille 1 no quedó activo.');
    assert(await dot2.getAttribute('aria-pressed') === 'true', 'El punto Braille 2 no quedó activo.');
    const pointsText = (await page.locator('#builderPoints').textContent()) || '';
    assert(pointsText.includes('1') && pointsText.includes('2'), 'El constructor no reflejó los puntos seleccionados.');

    await page.locator('#clearCell').click();
    assert(await dot1.getAttribute('aria-pressed') === 'false', 'Limpiar no desactivó el punto 1.');
    assert(((await page.locator('#builderPoints').textContent()) || '').includes('Sin puntos'), 'Limpiar no restauró el estado vacío.');

    const seriesTitle = page.locator('#seriesTitle');
    const beforeSeries = (await seriesTitle.textContent()) || '';
    const secondSeries = page.locator('.series-tab[data-series="2"]');
    await secondSeries.click();
    assert(await secondSeries.getAttribute('aria-selected') === 'true', 'La segunda serie no quedó seleccionada.');
    assert(((await seriesTitle.textContent()) || '') !== beforeSeries, 'El contenido de la serie no cambió.');

    console.log('✓ funcional Brailux: navegación, constructor Braille, limpieza y cambio de serie');
  } catch (error) {
    await saveFailure(page, 'functional-brailux', { error: error.message });
    failures.push({ label: 'functional-brailux', errors: [error.message] });
    console.error(`✗ funcional Brailux: ${error.message}`);
  } finally {
    await context.close();
  }
}

await runAxe('home-desktop', baseURL, { width: 1440, height: 900 });
await runAxe('home-mobile-menu', baseURL, { width: 390, height: 844 }, async page => {
  const menu = page.locator('header .menu-button, header .menu').first();
  if ((await menu.count()) && (await menu.isVisible())) await menu.click();
});
await runAxe('aprende-desktop', new URL('aprende.html', baseURL).href, { width: 1280, height: 800 });
await runFunctionalFlow();

await browser.close();
if (failures.length) {
  console.error(`\nFase 3 falló en ${failures.length} comprobación(es).`);
  process.exit(1);
}
console.log('\nFase 3 superada: accesibilidad automática y flujo funcional principal verificados.');
