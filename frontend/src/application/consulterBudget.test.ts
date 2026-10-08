import { describe, expect, it, vi } from 'vitest';
import type { BudgetBrut } from '../domain/budget';
import { chargerBudget, type ClientBudget } from './consulterBudget';

const brut: BudgetBrut = {
  aujourdhui: '2026-11-17',
  periode: '2026-03',
  periodes: ['2026-03'],
  totalDepenses: '1234.50',
  totalRevenus: '2000.00',
  plafondsDefinis: 2,
  repartition: [],
};

describe('[FR-006-02] chargerBudget', () => {
  it('demande la période au service et rend le budget affichable', async () => {
    const lire = vi.fn(() => Promise.resolve({ statut: 'budget' as const, budget: brut }));

    const budget = await chargerBudget('2026-03', { lire });

    expect(lire).toHaveBeenCalledWith('2026-03');
    expect(budget?.totalDepenses).toBe('1234.50 EUR');
  });

  it('sans période demandée, laisse le service choisir', async () => {
    const lire = vi.fn(() => Promise.resolve({ statut: 'budget' as const, budget: brut }));

    await chargerBudget(null, { lire });

    expect(lire).toHaveBeenCalledWith(null);
  });

  it.each<[string, ClientBudget['lire']]>([
    ['une réponse technique', () => Promise.resolve({ statut: 'technique' })],
    ['une exception', () => Promise.reject(new Error('réseau'))],
  ])('rend null pour %s', async (_nom, lire) => {
    expect(await chargerBudget(null, { lire })).toBeNull();
  });
});
