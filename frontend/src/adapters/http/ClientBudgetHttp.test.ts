import { describe, expect, it, vi } from 'vitest';
import { creerClientBudgetHttp } from './ClientBudgetHttp';

const json = (corps: unknown, status: number) => Promise.resolve(new Response(JSON.stringify(corps), { status }));
const budget = {
  aujourdhui: '2026-11-17',
  periode: '2026-03',
  periodes: ['2026-03', '2026-02'],
  totalDepenses: '1234.50',
  totalRevenus: '2000.00',
  plafondsDefinis: 2,
  repartition: [{ categorie: 'Logement', montant: '1000.00' }],
  plafonds: [{ id: 4, categorie: 'Logement', plafond: '500.00', depense: '1000.00' }],
};

describe('ClientBudgetHttp', () => {
  it('[FR-006-02] lit le budget sans paramètre quand aucune période n’est demandée', async () => {
    const appeler = vi.fn((_e: unknown, _o?: unknown) => json(budget, 200));
    const client = creerClientBudgetHttp(appeler as unknown as typeof fetch);

    expect(await client.lire(null)).toEqual({ statut: 'budget', budget });
    expect(appeler).toHaveBeenCalledWith('/api/coeur-bancaire/budget', expect.objectContaining({ method: 'GET' }));
  });

  it('[FR-006-01] ne transmet que la période, jamais d’identifiant de client ni de compte', async () => {
    const appeler = vi.fn((_e: unknown, _o?: unknown) => json(budget, 200));
    const client = creerClientBudgetHttp(appeler as unknown as typeof fetch);

    await client.lire('2026-02');

    expect(appeler).toHaveBeenCalledWith('/api/coeur-bancaire/budget?periode=2026-02', expect.anything());
  });

  it.each([
    ['un 401', () => json({ message: 'Authentification requise' }, 401)],
    ['un 500', () => json({}, 500)],
    ['un corps nul', () => json(null, 200)],
    ['une date du jour mal formée', () => json({ ...budget, aujourdhui: '17/11/2026' }, 200)],
    ['une date du jour absente', () => json({ ...budget, aujourdhui: undefined }, 200)],
    ['une période mal formée', () => json({ ...budget, periode: '2026-3' }, 200)],
    ['une période absente', () => json({ ...budget, periode: 3 }, 200)],
    ['des périodes qui ne sont pas une liste', () => json({ ...budget, periodes: 'x' }, 200)],
    ['une période de la liste mal formée', () => json({ ...budget, periodes: ['x'] }, 200)],
    ['un total de dépenses mal formé', () => json({ ...budget, totalDepenses: '12' }, 200)],
    ['un total de dépenses absent', () => json({ ...budget, totalDepenses: 12 }, 200)],
    ['un total de revenus mal formé', () => json({ ...budget, totalRevenus: 'x' }, 200)],
    ['un total de revenus absent', () => json({ ...budget, totalRevenus: 1 }, 200)],
    ['un nombre de plafonds non entier', () => json({ ...budget, plafondsDefinis: 1.5 }, 200)],
    ['un nombre de plafonds négatif', () => json({ ...budget, plafondsDefinis: -1 }, 200)],
    ['une répartition qui n’est pas une liste', () => json({ ...budget, repartition: {} }, 200)],
    ['une ligne nulle', () => json({ ...budget, repartition: [null] }, 200)],
    ['une ligne sans catégorie', () => json({ ...budget, repartition: [{ categorie: 1, montant: '1.00' }] }, 200)],
    ['une ligne au montant mal formé', () => json({ ...budget, repartition: [{ categorie: 'A', montant: '1' }] }, 200)],
    ['une ligne au montant absent', () => json({ ...budget, repartition: [{ categorie: 'A', montant: 1 }] }, 200)],
    ['des plafonds absents', () => json({ ...budget, plafonds: undefined }, 200)],
    ['une ligne de plafond nulle', () => json({ ...budget, plafonds: [null] }, 200)],
    ['un plafond sans identifiant entier', () => json({ ...budget, plafonds: [{ ...budget.plafonds[0], id: 'x' }] }, 200)],
    ['un plafond sans catégorie', () => json({ ...budget, plafonds: [{ ...budget.plafonds[0], categorie: 1 }] }, 200)],
    ['un plafond au montant mal formé', () => json({ ...budget, plafonds: [{ ...budget.plafonds[0], plafond: '5' }] }, 200)],
    ['un plafond au montant absent', () => json({ ...budget, plafonds: [{ ...budget.plafonds[0], plafond: 5 }] }, 200)],
    ['une dépense de plafond mal formée', () => json({ ...budget, plafonds: [{ ...budget.plafonds[0], depense: 'x' }] }, 200)],
    ['une dépense de plafond absente', () => json({ ...budget, plafonds: [{ ...budget.plafonds[0], depense: 1 }] }, 200)],
    ['un corps illisible', () => Promise.resolve(new Response('pas du json', { status: 200 }))],
    ['une coupure', () => Promise.reject(new Error('réseau'))],
  ])('[FR-006-02] rend technique pour %s', async (_nom, reponse) => {
    const client = creerClientBudgetHttp(reponse as unknown as typeof fetch);

    expect(await client.lire(null)).toEqual({ statut: 'technique' });
  });
});
