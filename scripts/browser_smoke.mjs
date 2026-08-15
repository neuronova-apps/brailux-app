import { chromium } from 'playwright';
import { mkdir } from 'node:fs/promises';

const baseURL = process.env.SMOKE_BASE_URL || 'http://127.0.0.1:4173/';
const baseOrigin = new URL(baseURL).origin;
const artifactsDir = 'artifacts/smoke';

const viewports = [
  { name: 'desktop-1440', width: 1440, height: 900 },
  { name: 'tablet-1024', width: 1024, height: 768 },
  { name: 'tablet-768', width: 768, height: 1024 },
  { name: 'mobile-390', width: 390, height: 844 },
  { name: 'mobile-360', width: 360, height: 800 }
];

const isLocal = (value) => {
  try {
    return new URL(value).origin === baseOrigin;
  } catch {
    return false;
  }
};

const browser = await chromium.launch({ headless: true });
const failures = [];

for (const viewport of viewports) {
  const context = await browser.newContext({
    viewport: { width: viewport.width, height: viewport.height },
    reducedMotion: 'reduce'
  });
  const page = await context.newPage();
  const errors = [];

  page.on('pageerror', (error) => {
    errors.push(`JavaScript runtime error: ${error.message}`);
  });

  page.on('console', (message) => {
    if (message.type() !== 'error') return;
    const location = message.location();
    if (!location.url || isLocal(location.url)) {
      errors.push(`Console error: ${message.text()}`);
    }
  });

  page.on('requestfailed', (request) => {
    if (isLocal(request.url())) {
      errors.push(`Local request failed: ${request.url()} (${request.failure()?.errorText || 'unknown error'})`);
    }
  });

  page.on('response', (response) => {
    if (isLocal(response.url()) && response.status() >= 400) {
      errors.push(`Local response ${response.status()}: ${response.url()}`);
    }
  });

  try {
    const response = await page.goto(baseURL, { waitUntil: 'domcontentloaded', timeout: 30_000 });
    if (!response || response.status() >= 400) {
      errors.push(`Homepage failed to load: HTTP ${response?.status() ?? 'no response'}`);
    }

    await page.waitForTimeout(700);

    const title = (await page.title()).trim();
    if (!title) errors.push('Document title is empty.');

    for (const selector of ['header', 'main', 'h1', 'footer']) {
      const locator = page.locator(selector).first();
      if ((await locator.count()) === 0) {
        errors.push(`Missing essential element: ${selector}`);
      } else if (!(await locator.isVisible())) {
        errors.push(`Essential element is not visible: ${selector}`);
      }
    }

    const layout = await page.evaluate(() => ({
      viewportWidth: window.innerWidth,
      documentWidth: document.documentElement.scrollWidth,
      bodyWidth: document.body.scrollWidth
    }));
    const widest = Math.max(layout.documentWidth, layout.bodyWidth);
    if (widest > layout.viewportWidth + 2) {
      errors.push(`Horizontal overflow: document ${widest}px > viewport ${layout.viewportWidth}px.`);
    }

    if (viewport.width <= 900) {
      const menuButton = page.locator('header .menu-button, header .menu').first();
      const nav = page.locator('header .main-nav').first();
      if ((await menuButton.count()) > 0 && (await menuButton.isVisible()) && (await nav.count()) > 0) {
        await menuButton.click();
        const expanded = await menuButton.getAttribute('aria-expanded');
        if (expanded !== 'true') errors.push('Mobile menu button did not set aria-expanded="true".');
        if (!(await nav.isVisible())) errors.push('Mobile navigation did not become visible after opening the menu.');
      }
    }
  } catch (error) {
    errors.push(`Smoke test exception: ${error.message}`);
  }

  if (errors.length) {
    await mkdir(artifactsDir, { recursive: true });
    const screenshot = `${artifactsDir}/${viewport.name}.png`;
    await page.screenshot({ path: screenshot, fullPage: true }).catch(() => {});
    failures.push({ viewport: viewport.name, errors });
    console.error(`\n✗ ${viewport.name}`);
    errors.forEach((error) => console.error(`  - ${error}`));
  } else {
    console.log(`✓ ${viewport.name} (${viewport.width}×${viewport.height})`);
  }

  await context.close();
}

await browser.close();

if (failures.length) {
  console.error(`\n${failures.length} viewport(s) failed responsive smoke checks.`);
  process.exit(1);
}

console.log(`\nAll ${viewports.length} responsive smoke checks passed.`);
