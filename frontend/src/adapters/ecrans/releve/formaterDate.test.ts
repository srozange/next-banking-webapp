import { describe, expect, it } from 'vitest';
import { formaterDate } from './formaterDate';

describe('[FR-005-06] formaterDate', () => {
  it.each([
    ['2026-11-03T08:05:00Z', '03/11/2026 09:05'],
    ['2026-07-14T22:30:00Z', '15/07/2026 00:30'],
    ['2026-12-31T23:59:00Z', '01/01/2027 00:59'],
    ['2026-03-29T00:30:00Z', '29/03/2026 01:30'],
    ['2026-03-29T01:30:00Z', '29/03/2026 03:30'],
  ])('%s en Europe/Paris donne %s', (instant, attendu) => {
    expect(formaterDate(instant)).toBe(attendu);
  });
});
