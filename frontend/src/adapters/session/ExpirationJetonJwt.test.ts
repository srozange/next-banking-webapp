import { describe, expect, it } from 'vitest';
import { jetonDeTest } from '../jeton/jetonDeTest';
import { creerExpirationJeton } from './ExpirationJetonJwt';
import { horlogeSysteme } from './horloge';

describe('ExpirationJetonJwt', () => {
  const MAINTENANT_MS = Date.parse('2026-10-06T10:00:00Z');
  const expiration = creerExpirationJeton({ maintenant: () => MAINTENANT_MS });

  it('[FR-030-05] calcule les secondes restantes sur l’horloge simulée', () => {
    const exp = MAINTENANT_MS / 1000;
    expect(expiration.secondesRestantes(jetonDeTest({ exp: exp + 1800 }))).toBe(1800);
    expect(expiration.secondesRestantes(jetonDeTest({ exp: exp + 599 }))).toBe(599);
    expect(expiration.secondesRestantes(jetonDeTest({ exp: exp - 30 }))).toBe(-30);
  });

  it('[FR-030-05] rend null pour un jeton illisible ou sans exp numérique', () => {
    expect(expiration.secondesRestantes('abc')).toBeNull();
    expect(expiration.secondesRestantes(jetonDeTest({ roles: ['CLIENT'] }))).toBeNull();
    expect(expiration.secondesRestantes(jetonDeTest({ exp: '123' }))).toBeNull();
  });

  it('l’horloge système rend l’heure courante en millisecondes', () => {
    const avant = Date.now();
    const lu = horlogeSysteme.maintenant();
    expect(lu).toBeGreaterThanOrEqual(avant);
    expect(lu).toBeLessThanOrEqual(Date.now());
  });
});
