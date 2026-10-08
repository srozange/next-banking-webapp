import { describe, expect, it } from 'vitest';
import { compterNonLues, formaterHorodatage, notificationAffichee, type NotificationBrute } from './notifications';

const brute = (id: number, type: string, lue: boolean, creeLe = '2026-11-17T09:05:07Z'): NotificationBrute => ({
  id,
  type,
  message: `message ${id}`,
  creeLe,
  lue,
});

describe('formaterHorodatage', () => {
  it('[FR-025-05] affiche dd/MM/yyyy HH:mm:ss en Europe/Paris (heure d’hiver)', () => {
    expect(formaterHorodatage('2026-11-17T09:05:07Z')).toBe('17/11/2026 10:05:07');
  });

  it('[FR-025-05] affiche l’heure d’été de Paris', () => {
    expect(formaterHorodatage('2026-07-01T10:00:00Z')).toBe('01/07/2026 12:00:00');
  });

  it('[FR-025-05] passe au jour suivant à Paris et garde minuit en 00 (cycle 0-23)', () => {
    expect(formaterHorodatage('2026-11-17T23:00:00Z')).toBe('18/11/2026 00:00:00');
  });

  it('[FR-025-05] accepte les millisecondes servies par le service', () => {
    expect(formaterHorodatage('2026-11-17T10:00:00.123Z')).toBe('17/11/2026 11:00:00');
  });
});

describe('compterNonLues', () => {
  it('[FR-002-08] compte les notifications non lues', () => {
    expect(compterNonLues([brute(1, 'ALERTE', false), brute(2, 'ALERTE', true), brute(3, 'FRAUDE', false)])).toBe(2);
    expect(compterNonLues([brute(1, 'ALERTE', true)])).toBe(0);
    expect(compterNonLues([])).toBe(0);
  });
});

describe('notificationAffichee', () => {
  it.each([
    ['CONFIRMATION', '✅', 'confirmation'],
    ['ALERTE', '⚠️', 'alerte'],
    ['FRAUDE', '🚨', 'fraude'],
  ])('[FR-025-05] %s porte l’icône %s et la variante %s', (type, icone, variante) => {
    expect(notificationAffichee(brute(4, type, false))).toEqual({
      id: 4,
      type,
      icone,
      variante,
      message: 'message 4',
      horodatage: '17/11/2026 10:05:07',
      lue: false,
    });
  });

  it('[FR-025-05] un type sans icône prévue garde son libellé, sans icône ni variante', () => {
    expect(notificationAffichee(brute(5, 'ACCOUNT_BALANCE', true))).toMatchObject({
      type: 'ACCOUNT_BALANCE',
      icone: null,
      variante: 'neutre',
      lue: true,
    });
  });
});
