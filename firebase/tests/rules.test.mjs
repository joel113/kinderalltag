import { readFileSync } from 'node:fs';
import { test, before, after, beforeEach } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, collection, getDoc, getDocs, setDoc, updateDoc, deleteDoc, serverTimestamp } from 'firebase/firestore';

let env;
const path = 'households/family';
before(async () => {
  env = await initializeTestEnvironment({ projectId: 'demo-kinderalltag', firestore: {
    rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'), host: '127.0.0.1', port: 8080,
  }});
});
after(async () => { await env?.cleanup(); });
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async context => {
    const db = context.firestore();
    await setDoc(doc(db, `${path}/members/parent-a`), { name: 'Joel' });
    await setDoc(doc(db, `${path}/members/parent-b`), { name: 'Mama' });
    await setDoc(doc(db, `${path}/children/johann`), { name: 'Johann' });
  });
});
const dbFor = uid => env.authenticatedContext(uid).firestore();
const revision = uid => ({ taskId: 'task-1', childId: 'johann', title: 'Testaufgabe', notes: '',
  frequency: 'DAILY', weekdays: [], times: ['08:00'], start: '2026-11-16', end: null,
  effective: '2026-11-16', active: true, authorId: uid, createdAt: serverTimestamp() });
const event = (uid, extra = {}) => ({ occurrenceId: 'task_revision_2026-11-16_28800', date: '2026-11-16',
  kind: 'COMPLETE', authorId: uid, authorName: uid, clientMillis: Date.now(), createdAt: serverTimestamp(), targetId: null, ...extra });

test('both parents can share data; unauthenticated and unrelated accounts cannot', async () => {
  for (const uid of ['parent-a', 'parent-b']) await assertSucceeds(getDocs(collection(dbFor(uid), `${path}/children`)));
  await assertFails(getDocs(collection(dbFor('stranger'), `${path}/children`)));
  await assertFails(getDocs(collection(env.unauthenticatedContext().firestore(), `${path}/events`)));
  await assertFails(getDocs(collection(dbFor('stranger'), `${path}/revisions`)));
});
test('membership cannot be granted or enumerated by a client', async () => {
  const db = dbFor('parent-a');
  await assertSucceeds(getDoc(doc(db, `${path}/members/parent-a`)));
  await assertFails(getDoc(doc(db, `${path}/members/parent-b`)));
  await assertFails(getDocs(collection(db, `${path}/members`)));
  await assertFails(setDoc(doc(db, `${path}/members/stranger`), { name: 'Stranger' }));
});
test('definitions are validated and immutable', async () => {
  const db = dbFor('parent-a');
  const ref = doc(db, `${path}/revisions/r1`);
  await assertSucceeds(setDoc(ref, revision('parent-a')));
  await assertFails(updateDoc(ref, { title: 'Changed past' }));
  await assertFails(deleteDoc(ref));
  for (const [index, extra] of [{ times: ['25:00'] }, { times: ['08:00', '08:00'] },
      { frequency: 'WEEKDAYS', weekdays: [] }, { authorId: 'parent-b' }, { title: '' }, { childId: 'unknown' }].entries()) {
    await assertFails(setDoc(doc(db, `${path}/revisions/bad-${index}`), { ...revision('parent-a'), ...extra }));
  }
});
test('either parent can acknowledge and retract while preserving audit records', async () => {
  const a = dbFor('parent-a'), b = dbFor('parent-b');
  await assertSucceeds(setDoc(doc(a, `${path}/events/e1`), event('parent-a')));
  await assertSucceeds(setDoc(doc(b, `${path}/events/e2`), event('parent-b')));
  await assertSucceeds(setDoc(doc(b, `${path}/events/undo`), event('parent-b', { kind: 'RETRACT', targetId: 'e1' })));
  await assertFails(updateDoc(doc(a, `${path}/events/e1`), { authorId: 'parent-b' }));
  await assertFails(deleteDoc(doc(a, `${path}/events/e1`)));
  await assertFails(setDoc(doc(a, `${path}/events/forged`), event('parent-b')));
  await assertFails(setDoc(doc(a, `${path}/events/missing`), event('parent-a', { kind: 'RETRACT', targetId: 'missing' })));
  await assertFails(setDoc(doc(a, `${path}/events/wrong-task`), event('parent-a', { kind: 'RETRACT', targetId: 'e1', occurrenceId: 'other' })));
});
test('household tasks need no child profile and remain restricted to family members', async () => {
  for (const uid of ['parent-a', 'parent-b']) {
    await assertSucceeds(setDoc(doc(dbFor(uid), `${path}/revisions/household-${uid}`),
      { ...revision(uid), taskId: `household-${uid}`, childId: 'haushalt', title: 'Wäsche waschen' }));
  }
  await assertFails(setDoc(doc(dbFor('stranger'), `${path}/revisions/household-stranger`),
    { ...revision('stranger'), childId: 'haushalt' }));
});
test('child names are editable but schema and unrelated paths stay protected', async () => {
  const db = dbFor('parent-a');
  await assertSucceeds(updateDoc(doc(db, `${path}/children/johann`), { name: 'Johann J.' }));
  await assertFails(updateDoc(doc(db, `${path}/children/johann`), { name: '' }));
  await assertFails(setDoc(doc(db, 'households/other/children/johann'), { name: 'Johann' }));
});
test('bear colors are optional for existing profiles and validated for both parents', async () => {
  for (const [index, color] of ['BLUE', 'ROSE', 'GREEN', 'HONEY', 'BROWN', 'PURPLE'].entries()) {
    const db = dbFor(index % 2 === 0 ? 'parent-a' : 'parent-b');
    await assertSucceeds(updateDoc(doc(db, `${path}/children/johann`), { bearColor: color }));
  }
  const db = dbFor('parent-a');
  await assertSucceeds(updateDoc(doc(db, `${path}/children/johann`), { name: 'Johann J.' }));
  await assertFails(updateDoc(doc(db, `${path}/children/johann`), { bearColor: 'INVALID' }));
  await assertFails(updateDoc(doc(db, `${path}/children/johann`), { bearColor: 123 }));
  await assertFails(updateDoc(doc(dbFor('stranger'), `${path}/children/johann`), { bearColor: 'BLUE' }));
});
