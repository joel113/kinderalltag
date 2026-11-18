import { readFileSync } from 'node:fs';
import assert from 'node:assert/strict';
import { test, before, after, beforeEach } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, collection, getDoc, getDocs, setDoc, updateDoc, deleteDoc, serverTimestamp, writeBatch, Timestamp } from 'firebase/firestore';

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
test('members can read parent names but cannot grant membership', async () => {
  const db = dbFor('parent-a');
  await assertSucceeds(getDoc(doc(db, `${path}/members/parent-a`)));
  await assertSucceeds(getDoc(doc(db, `${path}/members/parent-b`)));
  await assertSucceeds(getDocs(collection(db, `${path}/members`)));
  await assertFails(getDocs(collection(dbFor('stranger'), `${path}/members`)));
  await assertFails(getDoc(doc(dbFor('stranger'), `${path}/members/parent-a`)));
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

const appointment = uid => ({
  details: { title: 'Kinderarzt', recipient: 'BOTH_CHILDREN', date: '2027-01-20', startTime: '09:00', endTime: '10:00',
    allDay: false, responsibleParentId: null, location: '', notes: '', reminderMinutes: 30 },
  repeat: 'WEEKS', interval: 2, until: null, cutoff: null, version: 0, fromDate: '2027-01-20', authorId: uid, createdAt: serverTimestamp(),
});
const exception = (uid, extra = {}) => ({ seriesId: 's1', originalDate: '2027-01-20', replacement: null,
  version: 0, seriesVersion: 0, authorId: uid, updatedAt: serverTimestamp(), ...extra });

test('task responsibility accepts either parent and legacy/shared values but rejects unrelated accounts', async () => {
  const db = dbFor('parent-a');
  for (const [index, responsibleParentId] of [null, 'parent-a', 'parent-b'].entries()) {
    await assertSucceeds(setDoc(doc(db, `${path}/revisions/assigned-${index}`), { ...revision('parent-a'), responsibleParentId }));
  }
  await assertFails(setDoc(doc(db, `${path}/revisions/assigned-stranger`), { ...revision('parent-a'), responsibleParentId: 'stranger' }));
});
test('appointments validate recipients, responsibility, times, intervals, reminders and authors', async () => {
  const db = dbFor('parent-a');
  for (const [index, recipient] of ['FERDINAND', 'JOHANN', 'BOTH_CHILDREN', 'HOUSEHOLD'].entries()) {
    const item = appointment('parent-a');
    item.details.recipient = recipient;
    item.details.responsibleParentId = index % 2 === 0 ? 'parent-b' : null;
    await assertSucceeds(setDoc(doc(db, `${path}/appointmentSeries/valid-${index}`), item));
  }
  for (const [index, mutate] of [
    a => { a.details.responsibleParentId = 'stranger'; },
    a => { a.details.recipient = 'UNKNOWN'; },
    a => { a.details.endTime = '08:00'; },
    a => { a.details.startTime = '25:00'; },
    a => { a.interval = 0; },
    a => { a.interval = 1.5; },
    a => { a.repeat = 'UNKNOWN'; },
    a => { a.details.reminderMinutes = -1; },
    a => { a.details.allDay = true; },
    a => { a.authorId = 'parent-b'; },
    a => { a.until = '2026-01-01'; },
  ].entries()) {
    const item = appointment('parent-a'); mutate(item);
    await assertFails(setDoc(doc(db, `${path}/appointmentSeries/invalid-${index}`), item));
  }
  const allDay = appointment('parent-a'); allDay.details.allDay = true; allDay.details.reminderMinutes = 1440;
  await assertSucceeds(setDoc(doc(db, `${path}/appointmentSeries/all-day`), allDay));
  await assertFails(setDoc(doc(dbFor('stranger'), `${path}/appointmentSeries/forbidden`), appointment('stranger')));
  await assertFails(getDocs(collection(dbFor('stranger'), `${path}/appointmentSeries`)));
  await assertFails(getDocs(collection(env.unauthenticatedContext().firestore(), `${path}/appointmentExceptions`)));
});
test('appointment definitions are preserved and future boundaries only shorten with version checks', async () => {
  const db = dbFor('parent-a'), ref = doc(db, `${path}/appointmentSeries/s1`);
  await assertSucceeds(setDoc(ref, appointment('parent-a')));
  await assertFails(updateDoc(ref, { 'details.title': 'Rewrite history' }));
  await assertFails(updateDoc(ref, { cutoff: '2027-02-01', version: 0 }));
  await assertSucceeds(updateDoc(ref, { cutoff: '2027-02-01', version: 1 }));
  await assertFails(updateDoc(ref, { cutoff: null, version: 2 }));
  await assertFails(updateDoc(ref, { cutoff: '2027-03-01', version: 2 }));
  await assertSucceeds(updateDoc(ref, { cutoff: '2027-01-20', version: 2 }));
  await assertFails(deleteDoc(ref));
});
test('single-occurrence changes require valid targets, schema and optimistic versions', async () => {
  const a = dbFor('parent-a'), b = dbFor('parent-b');
  await setDoc(doc(a, `${path}/appointmentSeries/s1`), appointment('parent-a'));
  const ref = doc(a, `${path}/appointmentExceptions/s1_2027-01-20`);
  const changed = appointment('parent-a').details; changed.date = '2027-01-21';
  await assertSucceeds(setDoc(ref, exception('parent-a', { replacement: changed })));
  await assertFails(setDoc(ref, exception('parent-a', { version: 0 })));
  await assertSucceeds(setDoc(doc(b, `${path}/appointmentExceptions/s1_2027-01-20`), exception('parent-b', { version: 1 })));
  await assertFails(setDoc(doc(a, `${path}/appointmentExceptions/wrong-id`), exception('parent-a')));
  await assertFails(setDoc(doc(a, `${path}/appointmentExceptions/missing_2027-01-20`), exception('parent-a', { seriesId: 'missing' })));
  await assertFails(setDoc(doc(a, `${path}/appointmentExceptions/s1_2027-01-01`), exception('parent-a', { originalDate: '2027-01-01' })));
  await assertFails(setDoc(ref, exception('parent-a', { version: 2, seriesVersion: 99 })));
  await assertFails(deleteDoc(ref));
});

test('a rejected replacement cannot partially truncate a series', async () => {
  const db = dbFor('parent-a');
  const original = doc(db, `${path}/appointmentSeries/s1`);
  await setDoc(original, appointment('parent-a'));
  const batch = writeBatch(db);
  batch.update(original, { cutoff: '2027-02-01', version: 1 });
  const invalid = appointment('parent-a'); invalid.details.responsibleParentId = 'stranger';
  batch.set(doc(db, `${path}/appointmentSeries/replacement`), invalid);
  await assertFails(batch.commit());
  assert.equal((await getDoc(original)).data().cutoff, null);
  assert.equal((await getDoc(original)).data().version, 0);
});

test('completion can credit either member while preserving the authenticated author', async () => {
  const db = dbFor('parent-a');
  for (const [index, completedById] of ['parent-a', 'parent-b'].entries()) {
    await assertSucceeds(setDoc(doc(db, `${path}/events/credited-${index}`),
      event('parent-a', { completedById, completedByName: 'Mama' })));
  }
  for (const [index, extra] of [
    { completedById: 'stranger', completedByName: 'Stranger' },
    { completedById: null, completedByName: 'Mama' },
    { completedById: 'parent-b' }, { completedByName: 'Mama' },
    { completedById: 'parent-b', completedByName: '' },
    { completedById: 'parent-b', completedByName: 'Mama', authorId: 'parent-b' },
  ].entries()) {
    await assertFails(setDoc(doc(db, `${path}/events/invalid-credit-${index}`), event('parent-a', extra)));
  }
  await assertFails(setDoc(doc(dbFor('stranger'), `${path}/events/outsider-credit`),
    event('stranger', { completedById: 'parent-b', completedByName: 'Mama' })));
  await assertFails(setDoc(doc(db, `${path}/events/credit-on-retraction`),
    event('parent-a', { kind: 'RETRACT', targetId: 'credited-0', completedById: 'parent-b', completedByName: 'Mama' })));
});

const activity = (uid, item, extra = {}) => ({
  seriesId: 's1', originalDate: item.fromDate, kind: 'ADDED', following: false,
  details: item.details, resultSeriesId: null, authorId: uid, authorName: 'Papa',
  createdAt: serverTimestamp(), ...extra,
});
test('appointment activity is atomic, immutable and restricted to members and the actual author', async () => {
  const db = dbFor('parent-a');
  const item = appointment('parent-a');
  const batch = writeBatch(db);
  batch.set(doc(db, `${path}/appointmentSeries/s1`), item);
  batch.set(doc(db, `${path}/appointmentActivity/added`), activity('parent-a', item));
  await assertSucceeds(batch.commit());
  await assertSucceeds(getDocs(collection(dbFor('parent-b'), `${path}/appointmentActivity`)));
  await assertFails(getDocs(collection(dbFor('stranger'), `${path}/appointmentActivity`)));
  await assertFails(updateDoc(doc(db, `${path}/appointmentActivity/added`), { authorName: 'Other' }));
  await assertFails(deleteDoc(doc(db, `${path}/appointmentActivity/added`)));
  await assertFails(setDoc(doc(db, `${path}/appointmentActivity/orphan`), activity('parent-a', item, { seriesId: 'missing' })));
  for (const [index, extra] of [{ authorId: 'parent-b' }, { kind: 'UNKNOWN' }, { following: 'yes' },
    { createdAt: null }, { details: { ...item.details, title: '' } }, { unexpected: true }].entries()) {
    const invalid = writeBatch(db);
    const id = `invalid-${index}`;
    invalid.set(doc(db, `${path}/appointmentSeries/${id}`), item);
    invalid.set(doc(db, `${path}/appointmentActivity/${id}`), activity('parent-a', item, { seriesId: id, ...extra }));
    await assertFails(invalid.commit());
  }
  await assertFails(setDoc(doc(dbFor('stranger'), `${path}/appointmentActivity/outsider`), activity('stranger', item)));
});
test('single appointment edits and cancellations retain separate activity records', async () => {
  const db = dbFor('parent-a');
  const item = appointment('parent-a');
  await setDoc(doc(db, `${path}/appointmentSeries/s1`), item);
  const edit = writeBatch(db);
  const replacement = { ...item.details, title: 'Changed' };
  edit.set(doc(db, `${path}/appointmentExceptions/s1_2027-01-20`), exception('parent-a', { replacement }));
  edit.set(doc(db, `${path}/appointmentActivity/edit`), activity('parent-a', item, { kind: 'CHANGED', details: replacement }));
  await assertSucceeds(edit.commit());
  const cancel = writeBatch(db);
  cancel.set(doc(db, `${path}/appointmentExceptions/s1_2027-01-20`), exception('parent-a', { version: 1 }));
  cancel.set(doc(db, `${path}/appointmentActivity/cancel`), activity('parent-a', item, { kind: 'CANCELLED' }));
  await assertSucceeds(cancel.commit());
  assert.equal((await getDocs(collection(db, `${path}/appointmentActivity`))).size, 2);
  await assertFails(setDoc(doc(db, `${path}/appointmentActivity/fake`), activity('parent-a', item, { kind: 'CHANGED' })));
});
test('series split and following cancellation record activity in the same batch', async () => {
  const db = dbFor('parent-a');
  const item = appointment('parent-a');
  await setDoc(doc(db, `${path}/appointmentSeries/s1`), item);
  const replacement = { ...item, details: { ...item.details, date: '2027-02-03', title: 'New series' }, fromDate: '2027-02-03' };
  const split = writeBatch(db);
  split.update(doc(db, `${path}/appointmentSeries/s1`), { cutoff: '2027-02-03', version: 1 });
  split.set(doc(db, `${path}/appointmentSeries/s2`), replacement);
  split.set(doc(db, `${path}/appointmentActivity/split`), activity('parent-a', replacement,
    { originalDate: '2027-02-03', kind: 'CHANGED', following: true, resultSeriesId: 's2' }));
  await assertSucceeds(split.commit());
  const cancel = writeBatch(db);
  cancel.update(doc(db, `${path}/appointmentSeries/s1`), { cutoff: '2027-01-27', version: 2 });
  cancel.set(doc(db, `${path}/appointmentActivity/cancel-following`), activity('parent-a', item,
    { originalDate: '2027-01-27', kind: 'CANCELLED', following: true }));
  await assertSucceeds(cancel.commit());
});


const taskSnapshot = () => ({ id: 'source', taskId: 'task-1', childId: 'johann', title: 'Testaufgabe', notes: '',
  frequency: 'DAILY', weekdays: [], times: ['08:00'], start: '2026-11-16', end: null, effective: '2026-11-16',
  active: true, createdMillis: 0, authorId: 'parent-a', responsibleParentId: null, slotOrigins: ['source|28800'] });
const itemOperation = (uid, extra = {}) => ({
  type: 'TASK', itemId: 'task-1', action: 'DELETE', scope: 'ALL', anchorId: 'task-1_source_2026-11-16_28800',
  anchorDate: '2026-11-16', anchorTime: '08:00', sourceId: 'source', title: 'Testaufgabe', actualDate: '2026-11-16',
  actualStartAt: Timestamp.fromMillis(1), replacementStartAt: null, childId: 'johann', recipient: null,
  task: null, appointment: null, restoresId: null, createdAt: serverTimestamp(), authorId: uid, authorName: 'Papa', version: 1, ...extra,
});
const writeOperation = (db, id, operation) => {
  const batch = writeBatch(db);
  batch.set(doc(db, `${path}/itemOperations/${id}`), operation);
  batch.set(doc(db, `${path}/itemControls/${operation.type}_${operation.itemId}`), {
    version: operation.version, operationId: id, authorId: operation.authorId, updatedAt: serverTimestamp(),
  });
  return batch.commit();
};
const seedOperationSource = async db => setDoc(doc(db, `${path}/revisions/source`), revision('parent-a'));

test('recoverable deletion and restore are append-only atomic member operations', async () => {
  const db = dbFor('parent-a');
  await seedOperationSource(db);
  await assertSucceeds(writeOperation(db, 'delete', itemOperation('parent-a')));
  await assertSucceeds(getDocs(collection(dbFor('parent-b'), `${path}/itemOperations`)));
  await assertFails(getDocs(collection(dbFor('stranger'), `${path}/itemOperations`)));
  await assertFails(updateDoc(doc(db, `${path}/itemOperations/delete`), { scope: 'SINGLE' }));
  await assertFails(deleteDoc(doc(db, `${path}/itemOperations/delete`)));
  await assertFails(deleteDoc(doc(db, `${path}/itemControls/TASK_task-1`)));
  await assertSucceeds(writeOperation(dbFor('parent-b'), 'restore', itemOperation('parent-b', {
    action: 'RESTORE', restoresId: 'delete', version: 2,
  })));
  assert.equal((await getDocs(collection(db, `${path}/itemOperations`))).size, 2);
  assert.equal((await getDoc(doc(db, `${path}/revisions/source`))).exists(), true);
});
test('operation writes reject missing paired controls, stale versions and schema or actor forgery', async () => {
  const db = dbFor('parent-a');
  await seedOperationSource(db);
  await assertFails(setDoc(doc(db, `${path}/itemOperations/orphan`), itemOperation('parent-a')));
  await assertFails(setDoc(doc(db, `${path}/itemControls/TASK_task-1`), {
    version: 1, operationId: 'missing', authorId: 'parent-a', updatedAt: serverTimestamp(),
  }));
  await assertSucceeds(writeOperation(db, 'first', itemOperation('parent-a')));
  for (const [index, extra] of [
    { version: 1 }, { version: 3 }, { scope: 'UNKNOWN' }, { sourceId: 'missing' }, { itemId: 'other-task' },
    { authorId: 'parent-b' }, { createdAt: null }, { title: '' }, { unexpected: true },
    { action: 'RESTORE', restoresId: 'missing' }, { action: 'RESTORE', restoresId: 'first', scope: 'SINGLE' },
  ].entries()) {
    await assertFails(writeOperation(db, `invalid-${index}`, itemOperation('parent-a', { version: 2, ...extra })));
    assert.equal((await getDoc(doc(db, `${path}/itemControls/TASK_task-1`))).data().version, 1);
  }
  await assertFails(writeOperation(dbFor('stranger'), 'outsider', itemOperation('stranger', { version: 2 })));
});
test('future task edits accept a validated snapshot but cannot edit past anchors', async () => {
  const db = dbFor('parent-a');
  await seedOperationSource(db);
  const future = Timestamp.fromMillis(Date.now() + 86400000);
  const edit = itemOperation('parent-a', { action: 'EDIT', scope: 'FOLLOWING', task: taskSnapshot(), actualStartAt: future });
  await assertSucceeds(writeOperation(db, 'edit', edit));
  await assertSucceeds(writeOperation(db, 'single', { ...edit, scope: 'SINGLE', replacementStartAt: future, version: 2 }));
  for (const [index, extra] of [
    { actualStartAt: Timestamp.fromMillis(1) }, { scope: 'ALL' }, { task: { ...taskSnapshot(), taskId: 'wrong' } },
    { scope: 'SINGLE', replacementStartAt: Timestamp.fromMillis(1) },
    { task: { ...taskSnapshot(), times: ['25:00'] } }, { task: { ...taskSnapshot(), slotOrigins: ['source|999999'] } },
  ].entries()) await assertFails(writeOperation(db, `bad-edit-${index}`, { ...edit, version: 3, ...extra }));
});
test('past appointment instances and whole series can be hidden and restored without modifying their records', async () => {
  const db = dbFor('parent-a');
  const source = appointment('parent-a');
  await setDoc(doc(db, `${path}/appointmentSeries/series`), source);
  const deletion = itemOperation('parent-a', { type: 'APPOINTMENT', itemId: 'series', sourceId: 'series',
    anchorId: 'appointment_series_2026-11-16', childId: null, recipient: 'BOTH_CHILDREN', scope: 'SINGLE' });
  await assertSucceeds(writeOperation(db, 'appointment-delete', deletion));
  await assertSucceeds(writeOperation(db, 'appointment-restore', { ...deletion, action: 'RESTORE', restoresId: 'appointment-delete', version: 2 }));
  await assertFails(writeOperation(db, 'wrong-restore', { ...deletion, itemId: 'other', action: 'RESTORE', restoresId: 'appointment-delete', version: 1 }));
  assert.equal((await getDoc(doc(db, `${path}/appointmentSeries/series`))).data().details.title, source.details.title);
});
test('future appointment edits validate recurrence and do not accept past replacements', async () => {
  const db = dbFor('parent-a');
  const source = appointment('parent-a');
  await setDoc(doc(db, `${path}/appointmentSeries/series`), source);
  const future = Timestamp.fromMillis(Date.now() + 86400000);
  const replacement = { id: 'draft', details: source.details, repeat: source.repeat, interval: source.interval, until: source.until, fromDate: source.fromDate };
  const edit = itemOperation('parent-a', { type: 'APPOINTMENT', itemId: 'series', sourceId: 'series',
    anchorId: 'appointment_series_2027-01-20', childId: null, recipient: 'BOTH_CHILDREN', action: 'EDIT', scope: 'FOLLOWING',
    appointment: replacement, actualStartAt: future, replacementStartAt: future });
  await assertSucceeds(writeOperation(db, 'appointment-edit', edit));
  await assertFails(writeOperation(db, 'past-replacement', { ...edit, replacementStartAt: Timestamp.fromMillis(1), version: 2 }));
  await assertFails(writeOperation(db, 'invalid-recurrence', { ...edit, appointment: { ...replacement, interval: 0 }, version: 2 }));
});
test('offline confirmations can carry immutable occurrence snapshots tied to their source', async () => {
  const db = dbFor('parent-a');
  await seedOperationSource(db);
  const occurrenceId = 'task-1_source_2026-11-16_28800';
  const snapshot = { revision: taskSnapshot(), date: '2026-11-16', time: '08:00', id: occurrenceId,
    originalDate: '2026-11-16', originalTime: '08:00', sourceId: 'source' };
  await assertSucceeds(setDoc(doc(db, `${path}/events/snapshot`), event('parent-a', { occurrenceId, taskSnapshot: snapshot })));
  await assertFails(setDoc(doc(db, `${path}/events/forged-snapshot`), event('parent-a', {
    occurrenceId, taskSnapshot: { ...snapshot, id: 'other-occurrence' },
  })));
  await assertFails(setDoc(doc(db, `${path}/events/missing-source`), event('parent-a', {
    occurrenceId, taskSnapshot: { ...snapshot, sourceId: 'missing' },
  })));
});
