const { test, beforeEach } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const ts = require("typescript");

// Execute the actual TS modules in Node without a browser or extra test dependencies.
function createLoader() {
  const cache = new Map();
  function load(file) {
    file = path.resolve(file);
    if (cache.has(file)) return cache.get(file).exports;
    const module = { exports: {} };
    cache.set(file, module);
    const source = fs.readFileSync(file, "utf8").replace(/import\.meta\.env/g,
      JSON.stringify({ VITE_API_MODE: "mock", VITE_MAX_MODE: "mock", VITE_API_URL: "https://example.invalid/v1" }));
    const js = ts.transpileModule(source, {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
    }).outputText;
    const localRequire = (name) => name.startsWith("@/")
      ? load(`src/${name.slice(2)}.ts`)
      : name.startsWith(".") ? load(path.resolve(path.dirname(file), `${name}.ts`)) : require(name);
    vm.runInThisContext(`(function(require,module,exports){${js}\n})`, { filename: file })(localRequire, module, module.exports);
    return module.exports;
  }
  return load;
}

let load;
beforeEach(() => {
  load = createLoader();
  const values = new Map();
  global.localStorage = {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
  };
});

test("planning API uses authenticated real endpoints, exact IDs and atomic versioned PUT", async () => {
  const api = load("src/api/planningApi.ts");
  const client = load("src/api/client.ts");
  client.setAccessToken("test-token");
  const calls = [];
  global.fetch = async (url, init) => {
    calls.push({ url: new URL(url), init });
    assert.equal(init.headers.get("Authorization"), "Bearer test-token");
    return Response.json({ items: [], total: 0 });
  };
  await api.getDirections("прог инж", 20, "it&science");
  assert.equal(calls[0].url.pathname, "/v1/directions");
  assert.equal(calls[0].url.searchParams.get("interestCategoryId"), "it&science");
  assert.equal(calls[0].url.searchParams.get("offset"), "20");
  await api.getSelectedDirections("session");
  const ids = Array.from({ length: 7 }, (_, i) => `d${i}`);
  await api.saveSelectedDirections("session", ids);
  assert.deepEqual(JSON.parse(calls[2].init.body), { directionIds: ids });
  await api.saveSelectedDirections("session", []);
  assert.deepEqual(JSON.parse(calls[3].init.body), { directionIds: [] });
  await api.getRecommendations("session", "09.03.04&x", 40);
  assert.equal(calls[4].url.searchParams.get("directionId"), "09.03.04&x");
  await api.getApplicationPlan("session");
  const composition = { universities: [{ universityId: "u", programIds: ["p2", "p1"] }], bviProgramId: "p2" };
  await api.saveApplicationPlan("session", composition, 3);
  assert.equal(calls[6].url.pathname, "/v1/sessions/session/application-plan");
  assert.equal(calls[6].init.method, "PUT");
  assert.deepEqual(JSON.parse(calls[6].init.body), { ...composition, expectedVersion: 3 });
  await api.saveDiplomas("session", [{ profileId: "olymp-2025", degree: "prize", year: 2025, olympiadName: "Extra response field" }]);
  assert.deepEqual(JSON.parse(calls[7].init.body), { diplomas: [{ profileId: "olymp-2025", degree: "prize" }] });
  for (const status of [401, 409]) {
    global.fetch = async () => Response.json({ code: "conflict", message: "Cannot save" }, { status });
    await assert.rejects(api.saveApplicationPlan("session", composition, 3), (error) => error.status === status);
    assert.deepEqual(composition.universities[0].programIds, ["p2", "p1"]);
  }
});

test("autoplan preview posts mode, deficit and base plan without saving", async () => {
  const api = load("src/api/planningApi.ts");
  const calls = [];
  global.fetch = async (url, init) => {
    calls.push({ url: new URL(url), init });
    return Response.json({ composition: { universities: [], bviProgramId: null }, options: [], explanations: [],
      universityRanking: [], warnings: [], calculatedAt: "2026-09-30T00:00:00Z", nearPreviousThreshold: 5 });
  };
  await api.previewApplicationPlan("session", { mode: "generate", allowedDeficit: 10 });
  assert.equal(calls[0].url.pathname, "/v1/sessions/session/application-plan/preview");
  assert.equal(calls[0].init.method, "POST");
  assert.deepEqual(JSON.parse(calls[0].init.body), { mode: "generate", allowedDeficit: 10 });
  const basePlan = { universities: [{ universityId: "u", programIds: ["p"] }], bviProgramId: null };
  await api.previewApplicationPlan("session", { mode: "fill", basePlan });
  assert.deepEqual(JSON.parse(calls[1].init.body), { mode: "fill", basePlan });
  assert.equal(calls.some((call) => call.init.method === "PUT"), false);
});

test("plan enforces 5x5, direction uniqueness, campaign and a single valid BVI place", () => {
  const { emptyComposition, addProgram, removeProgram, moveItem, validateComposition } = load("src/utils/applicationPlan.ts");
  const options = Array.from({ length: 6 }, (_, u) => Array.from({ length: 6 }, (_, d) => ({
    universityId: `u${u}`, programId: `u${u}p${d}`, direction: { id: `d${d}` }, campaignYear: 2026, bviAvailable: d === 0,
  }))).flat();
  let plan = emptyComposition();
  for (const option of options.filter((p) => p.universityId !== "u5" && p.direction.id !== "d5")) plan = addProgram(plan, option, options);
  assert.equal(plan.universities.length, 5);
  assert.ok(plan.universities.every((u) => u.programIds.length === 5));
  assert.throws(() => addProgram(plan, options.find((p) => p.universityId === "u5"), options), /5 вузов/);
  assert.throws(() => addProgram(plan, options.find((p) => p.programId === "u0p5"), options), /1 до 5/);
  let small = addProgram(emptyComposition(), options[0], options);
  assert.equal(addProgram(small, options[0], options), small);
  assert.throws(() => addProgram(small, { ...options[0], programId: "alternative" }, options), /этого направления/);
  assert.throws(() => addProgram(small, { ...options[1], campaignYear: 2027 }, options), /кампании/);
  assert.equal(validateComposition({ ...small, bviProgramId: "u0p0" }, options), null);
  assert.match(validateComposition({ ...small, bviProgramId: "u1p0" }, options), /БВИ/);
  small = removeProgram({ ...small, bviProgramId: "u0p0" }, "u0p0");
  assert.deepEqual(small, emptyComposition());
  const order = ["p1", "p2", "p3"];
  assert.deepEqual(moveItem(order, 0, 1), ["p2", "p1", "p3"]);
  assert.deepEqual(order, ["p1", "p2", "p3"]);
  assert.deepEqual(moveItem(order, 0, -1), order);
});

test("plan draft survives navigation, isolates sessions and preserves the stale version for conflict detection", () => {
  global.sessionStorage = global.localStorage;
  const { writePlanDraft, readPlanDraft, clearPlanDraft } = load("src/utils/planDraft.ts");
  const draft = { composition: { universities: [{ universityId: "u", programIds: ["p"] }], bviProgramId: null },
    options: [{ universityId: "u", programId: "p", direction: { id: "d" }, campaignYear: 2026 }], expectedVersion: 2 };
  assert.equal(writePlanDraft("one", draft), true);
  assert.deepEqual(createLoader()("src/utils/planDraft.ts").readPlanDraft("one"), draft);
  assert.equal(readPlanDraft("two"), null);
  assert.equal(readPlanDraft("one").expectedVersion, 2);
  clearPlanDraft("one");
  assert.equal(readPlanDraft("one"), null);
  sessionStorage.setItem("application-plan-draft:v1:one", "bad json");
  assert.equal(readPlanDraft("one"), null);
  sessionStorage.setItem = () => { throw new Error("Quota exceeded"); };
  assert.equal(writePlanDraft("one", draft), false);
});

test("HTTP client accepts empty 200 and 204, parses JSON and preserves errors", async () => {
  const client = load("src/api/client.ts");
  for (const status of [200, 204]) {
    global.fetch = async () => new Response(null, { status });
    assert.equal(await client.request("/test"), undefined);
  }
  global.fetch = async () => Response.json({ ok: true });
  assert.deepEqual(await client.request("/test"), { ok: true });
  global.fetch = async () => Response.json({ message: "Invalid selection" }, { status: 400 });
  await assert.rejects(client.request("/test"), /Invalid selection/);
  global.fetch = async () => new Response("broken JSON", { status: 200 });
  await assert.rejects(client.request("/test"), SyntaxError);
});

test("session exchange is shared and can be retried after a failure", async () => {
  const auth = load("src/api/authApi.ts");
  const sessionApi = load("src/api/sessionApi.ts");
  const session = load("src/mocks/session.ts").mockSession;
  let exchanges = 0;
  auth.exchangeAuth = async () => {
    exchanges++;
    return { accessToken: "test-token", session: { id: session.id } };
  };
  sessionApi.getSession = async () => { throw new Error("Offline"); };
  const { initializeSession } = load("src/app/initializeSession.ts");
  const first = initializeSession();
  assert.equal(first, initializeSession());
  await assert.rejects(first, /Offline/);
  global.fetch = async (_url, options) => {
    assert.equal(options.headers.has("Authorization"), false);
    return Response.json({});
  };
  await load("src/api/client.ts").request("/test");
  sessionApi.getSession = async () => ({ ...session, isCompleteFromBot: false });
  const result = await initializeSession();
  assert.equal(exchanges, 2);
  assert.equal(result.session.isCompleteFromBot, false);
  assert.equal(result.accessToken, "test-token");
});

test("ошибки API сохраняют HTTP-статус и код для экрана ошибки", async () => {
  const client = load("src/api/client.ts");
  const { getErrorDetails } = load("src/utils/appError.ts");
  for (const status of [400, 401, 404, 500]) {
    global.fetch = async () => Response.json({ code: "invalid_init_data", message: "Ошибка входа" }, { status });
    await assert.rejects(client.request("/auth/exchange"), (error) => {
      assert.deepEqual(getErrorDetails(error), { status, code: "invalid_init_data" });
      return true;
    });
  }
  for (const body of ["<html>Bad Gateway</html>", "null", "{}"] ) {
    global.fetch = async () => new Response(body, { status: 502 });
    await assert.rejects(client.request("/test"), (error) => {
      assert.deepEqual(getErrorDetails(error), { status: 502, code: "HTTP_ERROR" });
      return true;
    });
  }
  global.fetch = async () => { throw new TypeError("Failed to fetch"); };
  await assert.rejects(client.request("/test"), (error) => {
    assert.deepEqual(getErrorDetails(error), { status: null, code: "NETWORK_ERROR" });
    return true;
  });
  assert.deepEqual(getErrorDetails(new Error("private details")), { status: null, code: "UNKNOWN_ERROR" });
});

test("повторный запуск очищает подробности ошибки", () => {
  const slice = load("src/store/slices/sessionSlice.ts");
  const details = { status: 401, code: "invalid_init_data" };
  let state = slice.default(undefined, slice.initializationFailed(details));
  assert.deepEqual(state.initializationErrorDetails, details);
  state = slice.default(state, slice.clearSession());
  assert.equal(state.initializationErrorDetails, null);
  assert.equal(state.initializationError, false);
});

test("session state exposes exam scores, incomplete state and initialization failure", () => {
  const slice = load("src/store/slices/sessionSlice.ts");
  const session = load("src/mocks/session.ts").mockSession;
  let state = slice.default(undefined, { type: "init" });
  state = slice.default(state, slice.setSession({ sessionId: session.id, accessToken: "token", language: "ru", egeScores: session.egeScores, isCompleteFromBot: false }));
  assert.equal(slice.selectEgeTotal({ session: state }), 275);
  assert.equal(state.isCompleteFromBot, false);
  state = slice.default(state, slice.initializationFailed());
  assert.equal(state.initializationError, true);
  assert.equal(state.isInitialized, false);
  state = slice.default(state, slice.clearSession());
  assert.equal(state.sessionId, null);
  assert.equal(state.initializationError, false);
});

test("direction priorities survive a fresh read and change the top direction", async () => {
  const api = load("src/mocks/mockApi.ts");
  const before = await api.getMockUniversityDetail("itmo");
  const ids = before.directions.map((item) => item.id).reverse();
  await api.saveMockDirectionPriorities("itmo", ids);
  const after = await api.getMockUniversityDetail("itmo");
  assert.deepEqual(after.directions.map((item) => item.id), ids);
  assert.deepEqual(after.directions.map((item) => item.priority), [1, 2, 3]);
  const summary = (await api.getMockUniversities()).find((item) => item.id === "itmo");
  assert.equal(summary.passingScorePreviousYear, after.directions[0].passingScorePreviousYear);
  await assert.rejects(api.saveMockDirectionPriorities("itmo", [ids[0], ids[0], ids[0]]));
});

test("selected achievements drive university totals and profile counts", async () => {
  const api = load("src/mocks/mockApi.ts");
  await api.saveMockAchievements([]);
  assert.equal((await api.getMockUniversityDetail("itmo")).idScoreTotal, 0);
  assert.ok((await api.getMockUniversities()).every((item) => item.myScore === 275));
  await api.saveMockAchievements(["gto_gold", "school_medal"]);
  const universities = await api.getMockUniversities();
  assert.deepEqual(universities.map((item) => item.myScore), [285, 283, 281, 282]);
  const priorities = await api.getMockPriorities();
  assert.deepEqual(priorities.map((item) => item.myScore), universities.map((item) => item.myScore));
  const profile = await api.getMockProfile();
  const { programs } = profile;
  assert.equal(programs.abovePrevious + programs.nearPrevious + programs.belowPrevious, universities.length);
  assert.equal(programs.total, universities.length);
  assert.equal(profile.achievements.length, 2);
  for (const item of universities) await api.removeMockUniversity(item.id);
  const empty = await api.getMockProfile();
  assert.equal(empty.programs.total, 0);
  assert.match(empty.advice, /подходящих программ нет/);
});

test("mock EGE scores can be edited and feed the profile", async () => {
  const api = load("src/mocks/mockApi.ts");
  const saved = await api.saveMockEgeScores([
    { subjectId: "russian", score: 90 },
    { subjectId: "informatics", score: 30 },
  ]);
  assert.equal(saved.allPassed, false);
  assert.deepEqual((await api.getMockEgeScores()).scores.map((item) => item.score), [90, 30]);
  assert.equal((await api.getMockProfile()).egeTotal, 120);
});

test("remove, re-add and reorder keep consecutive priorities without duplicates", async () => {
  const api = load("src/mocks/mockApi.ts");
  await api.removeMockUniversity("hse");
  assert.deepEqual((await api.getMockUniversities()).map((item) => item.priority), [1, 2, 3]);
  assert.equal((await api.addMockUniversity("hse")).priority, 4);
  await api.addMockUniversity("hse");
  assert.equal((await api.getMockUniversities()).length, 4);
  await assert.rejects(api.saveMockPriorities(["itmo", "itmo"]));
  const ids = ["mipt", "hse", "itmo", "msu"];
  await api.saveMockPriorities(ids);
  assert.deepEqual((await api.getMockUniversities()).map((item) => item.id), ids);
});

test("strategy report is a saved snapshot, not a live list", async () => {
  const api = load("src/mocks/mockApi.ts");
  await assert.rejects(api.getMockStrategyReport());
  const report = await api.finalizeMockStrategy();
  await api.removeMockUniversity("itmo");
  assert.deepEqual(await api.getMockStrategyReport(), report);
});

test("all locales have matching translation keys", () => {
  const keys = (obj) => Object.entries(obj).flatMap(([key, value]) => typeof value === "object" ? keys(value).map((child) => `${key}.${child}`) : [key]).sort();
  const ru = keys(load("src/i18n/locales/ru.ts").ru);
  assert.deepEqual(keys(load("src/i18n/locales/kk.ts").kk), ru);
  assert.deepEqual(keys(load("src/i18n/locales/ky.ts").ky), ru);
});

test("onboarding selection survives reload, respects empty choices and isolates sessions", () => {
  const storage = load("src/mocks/mockStorage.ts");
  const data = storage.getMockStorage();
  data.selectedAchievementIds = ["school_medal"];
  storage.saveMockStorage(data);
  const draft = load("src/utils/onboardingDraft.ts");
  assert.deepEqual(draft.readOnboardingSelection("one", "selectedAchievementIds"), ["school_medal"]);
  draft.writeOnboardingSelection("one", "selectedAchievementIds", []);
  draft.writeOnboardingSelection("one", "selectedInterestIds", ["it"]);
  const reloaded = createLoader()("src/utils/onboardingDraft.ts");
  assert.deepEqual(reloaded.readOnboardingSelection("one", "selectedAchievementIds"), []);
  assert.deepEqual(reloaded.readOnboardingSelection("one", "selectedInterestIds"), ["it"]);
  assert.deepEqual(reloaded.readOnboardingSelection("two", "selectedAchievementIds"), ["school_medal"]);
  global.localStorage.getItem = () => { throw new Error("Storage blocked"); };
  global.localStorage.setItem = () => { throw new Error("Storage blocked"); };
  assert.deepEqual(reloaded.readOnboardingSelection("one", "selectedAchievementIds"), []);
  assert.doesNotThrow(() => reloaded.writeOnboardingSelection("one", "selectedAchievementIds", []));
});
