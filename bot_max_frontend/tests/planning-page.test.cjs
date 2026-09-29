const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');
const React = require('react');
const { create, act } = require('react-test-renderer');
const { MemoryRouter } = require('react-router-dom');

global.IS_REACT_ACT_ENVIRONMENT = true;

function pageLoader() {
  const cache = new Map();
  function load(file) {
    file = path.resolve(file);
    if (cache.has(file)) return cache.get(file).exports;
    const module = { exports: {} };
    cache.set(file, module);
    const source = fs.readFileSync(file, 'utf8').replace(/import\.meta\.env/g,
      JSON.stringify({ VITE_API_MODE: 'real', VITE_API_URL: 'https://example.invalid/v1' }));
    const js = ts.transpileModule(source, { compilerOptions: {
      module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, jsx: ts.JsxEmit.ReactJSX,
    } }).outputText;
    function localRequire(name) {
      if (name.endsWith('.css')) return { default: {} };
      if (name === '@/store/hooks') return { useAppSelector: (select) => select({ session: { sessionId: 'session' } }) };
      if (name === '@/components/AppLayout/AppLayout') return { AppLayout: ({ children }) => children };
      if (name.startsWith('@/') || name.startsWith('.')) {
        const base = name.startsWith('@/') ? path.resolve('src', name.slice(2)) : path.resolve(path.dirname(file), name);
        return load(['.ts', '.tsx'].map((ext) => base + ext).find(fs.existsSync));
      }
      return require(name);
    }
    vm.runInThisContext(`(function(require,module,exports){${js}\n})`, { filename: file })(localRequire, module, module.exports);
    return module.exports;
  }
  return load;
}

const option = {
  programId: 'p', programName: 'Программа', universityId: 'u', universityName: 'Вуз',
  direction: { id: 'ivt', code: '09.03.01', name: 'Информатика и вычислительная техника' },
  city: 'Казань', campus: null, campaignYear: 2026, funding: 'budget', studyForm: 'full_time',
  competitionType: 'general', eligibility: 'eligible', bviAvailable: false, totalScore: 250,
  passingScorePreviousYear: null, previousYear: null, scoreDifference: null,
  comparison: 'insufficient_data', dataSource: 'verified', breakdown: [], reasons: [],
};
const emptyPlan = { composition: { universities: [], bviProgramId: null }, options: [], version: 7,
  savedAt: null, warnings: [], explanations: [], calculatedAt: '2026-09-29T10:00:00Z', nearPreviousThreshold: 5 };

async function mountPage() {
  global.sessionStorage = { getItem: () => null, setItem: () => {}, removeItem: () => {} };
  global.window = { addEventListener: () => {}, removeEventListener: () => {} };
  const Page = pageLoader()('src/pages/PlanningPage/PlanningPage.tsx').PlanningPage;
  let renderer;
  await act(async () => { renderer = create(React.createElement(MemoryRouter, { initialEntries: ['/universities'] }, React.createElement(Page))); });
  return renderer;
}

test('results request every selected direction even when the plan returns 404; no catalogue dependency', async () => {
  const calls = [];
  global.fetch = async (url) => {
    const u = new URL(url); calls.push(u);
    if (u.pathname.endsWith('/application-plan')) return Response.json({ message: 'Not implemented' }, { status: 404 });
    if (u.pathname.endsWith('/sessions/session/directions')) return Response.json({ directionIds: ['ivt', 'se'] });
    if (u.pathname.endsWith('/recommendations')) return Response.json({ items: [option], total: 1 });
    throw new Error(`Unexpected dependency: ${url}`);
  };
  const renderer = await mountPage();
  try {
    assert.deepEqual(calls.filter((u) => u.pathname.endsWith('/recommendations')).map((u) => u.searchParams.get('directionId')).sort(), ['ivt', 'se']);
    assert.equal(calls.some((u) => u.pathname === '/v1/directions'), false);
    const addButtons = renderer.root.findAllByType('button').filter((b) => b.children.includes('Добавить в план'));
    assert.equal(addButtons.length, 2);
    assert.ok(addButtons.every((b) => b.props.disabled));
    assert.match(JSON.stringify(renderer.toJSON()), /Информатика и вычислительная техника/);
  } finally { await act(async () => renderer.unmount()); }
});

test('pending plan does not delay recommendations; after loading its actual version is used on save', async () => {
  let resolvePlan;
  const pending = new Promise((resolve) => { resolvePlan = resolve; });
  const calls = [];
  global.fetch = async (url, init) => {
    const u = new URL(url); calls.push({ u, init });
    if (u.pathname.endsWith('/application-plan')) {
      if (init.method === 'PUT') return Response.json({ ...emptyPlan, composition: JSON.parse(init.body), version: 8, options: [option] });
      return pending;
    }
    if (u.pathname.endsWith('/sessions/session/directions')) return Response.json({ directionIds: ['ivt'] });
    if (u.pathname.endsWith('/recommendations')) return Response.json({ items: [option], total: 1 });
    throw new Error(`Unexpected URL: ${url}`);
  };
  const renderer = await mountPage();
  const button = (label) => renderer.root.findAllByType('button').find((b) => b.children.includes(label));
  try {
    assert.ok(calls.some(({ u }) => u.pathname.endsWith('/recommendations')));
    assert.equal(button('Добавить в план').props.disabled, true);
    await act(async () => resolvePlan(Response.json(emptyPlan)));
    assert.equal(button('Добавить в план').props.disabled, false);
    await act(async () => button('Добавить в план').props.onClick());
    await act(async () => button('К итоговой расстановке').props.onClick());
    await act(async () => button('Сохранить план').props.onClick());
    const put = calls.find(({ init }) => init.method === 'PUT');
    assert.equal(JSON.parse(put.init.body).expectedVersion, 7);
    assert.deepEqual(JSON.parse(put.init.body).universities, [{ universityId: 'u', programIds: ['p'] }]);
  } finally { await act(async () => renderer.unmount()); }
});

test('empty direction selection makes no recommendation requests and offers selection', async () => {
  global.fetch = async (url) => {
    if (url.includes('/application-plan')) return Response.json(emptyPlan);
    if (url.includes('/sessions/session/directions')) return Response.json({ directionIds: [] });
    throw new Error(`Unexpected request: ${url}`);
  };
  const renderer = await mountPage();
  try { assert.match(JSON.stringify(renderer.toJSON()), /Вы ещё не выбрали направления/); }
  finally { await act(async () => renderer.unmount()); }
});
