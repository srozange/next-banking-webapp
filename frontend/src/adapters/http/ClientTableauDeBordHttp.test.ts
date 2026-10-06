import { describe, expect, it, vi } from 'vitest';
import { creerClientTableauDeBordHttp } from './ClientTableauDeBordHttp';

const json = (corps: unknown, status: number) => Promise.resolve(new Response(JSON.stringify(corps), { status }));
const compte = { id: 1, type: 'COURANT', numero: 'FR76-0001-0001', solde: '2130.00' };
const operation = { date: '2026-11-03T08:00:00Z', description: 'x', type: 'DEBIT', montant: '30.00', soldeApres: '2100.00' };
const corps = { comptes: [compte], soldeTotal: '2130.00', dernieresOperations: [operation] };

describe('ClientTableauDeBordHttp', () => {
  it('[FR-002-03] lit le tableau de bord sans aucun paramètre', async () => {
    const appeler = vi.fn((_e: unknown, _o?: unknown) => json(corps, 200));
    const client = creerClientTableauDeBordHttp(appeler as unknown as typeof fetch);

    expect(await client.lire()).toEqual({ statut: 'tableau', comptes: [compte], dernieresOperations: [operation] });
    expect(appeler).toHaveBeenCalledWith('/api/coeur-bancaire/tableau-de-bord', expect.objectContaining({ method: 'GET' }));
  });

  it.each([
    ['un 500', () => json({}, 500)],
    ['un corps nul', () => json(null, 200)],
    ['des comptes absents', () => json({ dernieresOperations: [] }, 200)],
    ['des opérations absentes', () => json({ comptes: [] }, 200)],
    ['un compte sans type', () => json({ ...corps, comptes: [{ ...compte, type: 3 }] }, 200)],
    ['un compte sans numéro', () => json({ ...corps, comptes: [{ ...compte, numero: 3 }] }, 200)],
    ['un compte sans identifiant', () => json({ ...corps, comptes: [{ ...compte, id: 'a' }] }, 200)],
    ['un solde mal formé', () => json({ ...corps, comptes: [{ ...compte, solde: '2130' }] }, 200)],
    ['un compte nul', () => json({ ...corps, comptes: [null] }, 200)],
    ['une opération nulle', () => json({ ...corps, dernieresOperations: [null] }, 200)],
    ['une opération sans type connu', () => json({ ...corps, dernieresOperations: [{ ...operation, type: 'X' }] }, 200)],
    ['une opération sans description', () => json({ ...corps, dernieresOperations: [{ ...operation, description: 1 }] }, 200)],
    ['une opération sans date', () => json({ ...corps, dernieresOperations: [{ ...operation, date: 1 }] }, 200)],
    ['un montant mal formé', () => json({ ...corps, dernieresOperations: [{ ...operation, montant: '30' }] }, 200)],
    ['un solde après mal formé', () => json({ ...corps, dernieresOperations: [{ ...operation, soldeApres: 'x' }] }, 200)],
    ['un corps illisible', () => Promise.resolve(new Response('pas du json', { status: 200 }))],
    ['une coupure', () => Promise.reject(new Error('réseau'))],
  ])('[FR-002-03] rend technique pour %s', async (_nom, reponse) => {
    const client = creerClientTableauDeBordHttp(reponse as unknown as typeof fetch);

    expect(await client.lire()).toEqual({ statut: 'technique' });
  });
});
