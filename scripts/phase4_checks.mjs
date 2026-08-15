import { chromium } from 'playwright';
import { mkdir, readdir, readFile, writeFile } from 'node:fs/promises';
import { extname, join, relative } from 'node:path';

const baseURL = process.env.SMOKE_BASE_URL || 'http://127.0.0.1:4173/';
const baseOrigin = new URL(baseURL).origin;
const artifactsDir = 'artifacts/phase4';
const failures = [];
const report = { security: [], performance: null, resilience: [] };

const budgets = {
  localBytes: 3_000_000,
  localRequests: 100,
  domNodes: 4_000,
  domContentLoadedMs: 3_000,
  loadMs: 5_000
};

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

async function ensureArtifacts() {
  await mkdir(artifactsDir, { recursive: true });
}

async function saveDiagnostic(label, details, page) {
  await ensureArtifacts();
  if (page) await page.screenshot({ path: `${artifactsDir}/${label}.png`, fullPage: true }).catch(() => {});
  await writeFile(`${artifactsDir}/${label}.json`, JSON.stringify(details, null, 2), 'utf8').catch(() => {});
}

function addFailure(label, message) {
  failures.push({ label, message });
  console.error(`✗ ${label}: ${message}`);
}

function lineNumber(source, index) {
  return source.slice(0, index).split('\n').length;
}

async function walk(dir = '.') {
  const ignored = new Set(['.git', 'node_modules', 'artifacts', 'build', 'dist', '.gradle', '.idea', '.kotlin']);
  const entries = await readdir(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    if (ignored.has(entry.name)) continue;
    const path = join(dir, entry.name);
    if (entry.isDirectory()) files.push(...await walk(path));
    else files.push(path);
  }
  return files;
}

async function runSecurityChecks() {
  const files = (await walk()).filter(path => {
    if (path.startsWith('scripts/')) return false;
    return ['.html', '.js', '.css'].includes(extname(path));
  });
  const findings = [];

  for (const path of files) {
    const source = await readFile(path, 'utf8');
    const rel = relative('.', path);

    const secretPatterns = [
      { name: 'private-key', regex: /-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----/g },
      { name: 'github-token', regex: /\bgh[pousr]_[A-Za-z0-9]{20,}\b/g }
    ];
    for (const pattern of secretPatterns) {
      for (const match of source.matchAll(pattern.regex)) {
        findings.push(`${rel}:${lineNumber(source, match.index)} posible ${pattern.name}`);
      }
    }

    if (extname(path) === '.html') {
      const mixed = /\b(?:src|href)\s*=\s*["']http:\/\/(?!127\.0\.0\.1|localhost)[^"']+["']/gi;
      for (const match of source.matchAll(mixed)) {
        findings.push(`${rel}:${lineNumber(source, match.index)} recurso HTTP inseguro`);
      }

      const inlineHandler = /\son[a-z]+\s*=\s*["']/gi;
      for (const match of source.matchAll(inlineHandler)) {
        findings.push(`${rel}:${lineNumber(source, match.index)} handler inline ${match[0].trim()}`);
      }

      const blankLinks = source.match(/<a\b[^>]*target\s*=\s*["']_blank["'][^>]*>/gi) || [];
      for (const tag of blankLinks) {
        const relMatch = tag.match(/\brel\s*=\s*["']([^"']*)["']/i);
        const relTokens = new Set((relMatch?.[1] || '').toLowerCase().split(/\s+/).filter(Boolean));
        if (!relTokens.has('noopener') || !relTokens.has('noreferrer')) {
          const index = source.indexOf(tag);
          findings.push(`${rel}:${lineNumber(source, index)} target="_blank" sin noopener noreferrer`);
        }
      }
    }

    if (extname(path) === '.js') {
      const dangerous = [
        { name: 'eval()', regex: /\beval\s*\(/g },
        { name: 'new Function()', regex: /\bnew\s+Function\s*\(/g },
        { name: 'document.write()', regex: /\bdocument\.write\s*\(/g }
      ];
      for (const pattern of dangerous) {
        for (const match of source.matchAll(pattern.regex)) {
          findings.push(`${rel}:${lineNumber(source, match.index)} uso de ${pattern.name}`);
        }
      }
    }
  }

  report.security = findings;
  if (findings.length) {
    await saveDiagnostic('security-findings', { findings });
    findings.forEach(item => console.error(`  - ${item}`));
    addFailure('seguridad', `${findings.length} hallazgo(s) bloqueante(s)`);
  } else {
    console.log(`✓ seguridad: ${files.length} archivos revisados sin patrones bloqueantes`);
  }
}

async function blockRemoteRequests(page) {
  await page.route('**/*', route => {
    const url = route.request().url();
    if (url.startsWith('data:') || url.startsWith('blob:')) return route.continue();
    try {
      if (new URL(url).origin === baseOrigin) return route.continue();
    } catch {
      return route.continue();
    }
    return route.abort('failed');
  });
}

async function runPerformanceChecks(browser) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, reducedMotion: 'reduce' });
  const page = await context.newPage();
  const responses = [];
  const pageErrors = [];
  page.on('pageerror', error => pageErrors.push(error.message));
  page.on('response', response => {
    try {
      if (new URL(response.url()).origin !== baseOrigin) return;
      const length = Number(response.headers()['content-length'] || 0);
      responses.push({ url: response.url(), status: response.status(), bytes: Number.isFinite(length) ? length : 0 });
    } catch {}
  });

  try {
    await blockRemoteRequests(page);
    const response = await page.goto(baseURL, { waitUntil: 'load', timeout: 30_000 });
    assert(response && response.status() < 400, 'La portada no cargó correctamente.');
    await page.waitForTimeout(250);

    const metrics = await page.evaluate(() => {
      const nav = performance.getEntriesByType('navigation')[0];
      return {
        domContentLoadedMs: Math.round(nav?.domContentLoadedEventEnd || 0),
        loadMs: Math.round(nav?.loadEventEnd || 0),
        domNodes: document.getElementsByTagName('*').length,
        h1: document.querySelector('h1')?.textContent?.trim() || '',
        mainVisible: Boolean(document.querySelector('main'))
      };
    });
    const localBytes = responses.reduce((sum, item) => sum + item.bytes, 0);
    const localRequests = responses.length;
    report.performance = { ...metrics, localBytes, localRequests, responses, pageErrors, budgets };

    assert(metrics.mainVisible && metrics.h1, 'No se encontró contenido principal usable.');
    assert(pageErrors.length === 0, `Errores JavaScript con dependencias remotas bloqueadas: ${pageErrors.join(' | ')}`);
    assert(localBytes <= budgets.localBytes, `Payload local ${localBytes} B supera presupuesto ${budgets.localBytes} B.`);
    assert(localRequests <= budgets.localRequests, `${localRequests} requests locales superan presupuesto ${budgets.localRequests}.`);
    assert(metrics.domNodes <= budgets.domNodes, `${metrics.domNodes} nodos DOM superan presupuesto ${budgets.domNodes}.`);
    assert(metrics.domContentLoadedMs <= budgets.domContentLoadedMs, `DOMContentLoaded ${metrics.domContentLoadedMs} ms supera ${budgets.domContentLoadedMs} ms.`);
    assert(metrics.loadMs <= budgets.loadMs, `load ${metrics.loadMs} ms supera ${budgets.loadMs} ms.`);

    console.log(`✓ rendimiento: ${(localBytes / 1024).toFixed(1)} KiB, ${localRequests} requests, ${metrics.domNodes} nodos, DCL ${metrics.domContentLoadedMs} ms, load ${metrics.loadMs} ms`);
    report.resilience.push({ scenario: 'remote-dependencies-blocked', success: true, pageErrors: [] });
    console.log('✓ resiliencia: la portada funciona con dependencias remotas bloqueadas');
  } catch (error) {
    await saveDiagnostic('performance-or-remote-resilience', { error: error.message, report: report.performance }, page);
    addFailure('rendimiento/resiliencia-remota', error.message);
  } finally {
    await context.close();
  }
}

async function runStorageResilience(browser) {
  const context = await browser.newContext({ viewport: { width: 1280, height: 800 }, reducedMotion: 'reduce' });
  await context.addInitScript(() => {
    const fail = () => { throw new DOMException('Storage unavailable for resilience test', 'SecurityError'); };
    for (const method of ['getItem', 'setItem', 'removeItem', 'clear']) {
      try { Storage.prototype[method] = fail; } catch {}
    }
  });
  const page = await context.newPage();
  const pageErrors = [];
  page.on('pageerror', error => pageErrors.push(error.message));

  try {
    const response = await page.goto(baseURL, { waitUntil: 'domcontentloaded', timeout: 30_000 });
    assert(response && response.status() < 400, 'La portada no cargó con almacenamiento indisponible.');
    await page.waitForTimeout(400);
    const usable = await page.evaluate(() => Boolean(document.querySelector('main') && document.querySelector('h1')?.textContent?.trim()));
    assert(usable, 'El contenido principal dejó de estar disponible sin localStorage.');
    assert(pageErrors.length === 0, `Errores JavaScript sin localStorage: ${pageErrors.join(' | ')}`);
    report.resilience.push({ scenario: 'storage-unavailable', success: true, pageErrors: [] });
    console.log('✓ resiliencia: la portada funciona aunque localStorage no esté disponible');
  } catch (error) {
    report.resilience.push({ scenario: 'storage-unavailable', success: false, pageErrors, error: error.message });
    await saveDiagnostic('storage-resilience', { error: error.message, pageErrors }, page);
    addFailure('resiliencia-storage', error.message);
  } finally {
    await context.close();
  }
}

await runSecurityChecks();
const browser = await chromium.launch({ headless: true });
await runPerformanceChecks(browser);
await runStorageResilience(browser);
await browser.close();

await ensureArtifacts();
await writeFile(`${artifactsDir}/phase4-report.json`, JSON.stringify({ budgets, ...report, failures }, null, 2), 'utf8');

if (failures.length) {
  console.error(`\nFase 4 falló en ${failures.length} comprobación(es).`);
  process.exit(1);
}
console.log('\nFase 4 superada: rendimiento, seguridad y resiliencia verificados.');
