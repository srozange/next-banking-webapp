import { describe, expect, it } from 'vitest';
import { lireMontantEnCentimes, type SourceMontant } from './lireMontant';

function sourceFixe(montant: string): SourceMontant {
  return { lire: () => Promise.resolve(montant) };
}

describe('lireMontantEnCentimes', () => {
  it('rend en centimes le montant fourni par le port', async () => {
    await expect(lireMontantEnCentimes(sourceFixe('42.10'))).resolves.toBe(4210);
  });

  it('rejette un montant mal formé fourni par le port', async () => {
    await expect(lireMontantEnCentimes(sourceFixe('42,10'))).rejects.toThrow();
  });
});
