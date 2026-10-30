const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validSubjectPayload(userId = ALICE_UID, now = new Date()) {
  return {
    userId,
    name: "Математический анализ",
    teacher: "Иванов И.И., ауд. 304",
    colorHex: "#7C5CFF",
    iconKey: "MATH",
    createdAt: now,
    updatedAt: now,
  };
}

function validAssignmentPayload(userId = ALICE_UID, now = new Date()) {
  return {
    userId,
    subjectId: "subj_math",
    subjectName: "Математический анализ",
    subjectColorHex: "#7C5CFF",
    title: "Лабораторная №3: Интегралы",
    workType: "LAB",
    stage: "REVISION",
    progressPercent: 75,
    currentProgressNote: "Решил 8 из 10 примеров, остановился на несобственных интегралах",
    revisionNotes: "Пересчитать пункт 4 и добавить график функции",
    checklistItems: ["[x] Решить базовые примеры", "[ ] Построить график в GeoGebra"],
    dueDateMillis: 1793400000000,
    priority: "HIGH",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or create subjects or assignments", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/assignments`).where("userId", "==", ALICE_UID).get()
  );
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/subjects/subj_1`).set(validSubjectPayload(ALICE_UID))
  );
});

test("Authenticated user: can create, read, list, update, and delete own subject and assignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const subjRef = aliceDb.doc(`users/${ALICE_UID}/subjects/subj_math`);
  await assertSucceeds(subjRef.set(validSubjectPayload(ALICE_UID)));

  const assignRef = aliceDb.doc(`users/${ALICE_UID}/assignments/assign_1`);
  const initialPayload = validAssignmentPayload(ALICE_UID);
  await assertSucceeds(assignRef.set(initialPayload));

  await assertSucceeds(assignRef.get());
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/assignments`).where("userId", "==", ALICE_UID).get()
  );

  await assertSucceeds(
    assignRef.update({
      stage: "READY",
      progressPercent: 95,
      currentProgressNote: "График готов, осталось распечатать",
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(assignRef.delete());
});

test("Authenticated user: cannot read or modify another user's assignments", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .doc(`users/${ALICE_UID}/assignments/assign_secret`)
      .set(validAssignmentPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/assignments/assign_secret`).get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/assignments`).where("userId", "==", ALICE_UID).get()
  );
  await assertFails(bobDb.doc(`users/${ALICE_UID}/assignments/assign_secret`).delete());
});

test("Security bounds: rejects shadow fields, invalid progressPercent, and userId spoofing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // 1. Spoofed userId
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/assignments/bad_1`).set({
      ...validAssignmentPayload(ALICE_UID),
      userId: BOB_UID,
    })
  );

  // 2. Shadow field injection
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/assignments/bad_2`).set({
      ...validAssignmentPayload(ALICE_UID),
      isAdmin: true,
    })
  );

  // 3. Invalid progressPercent > 100
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/assignments/bad_3`).set({
      ...validAssignmentPayload(ALICE_UID),
      progressPercent: 120,
    })
  );
});
