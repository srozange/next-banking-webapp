import { describe, expect, it, vi } from 'vitest';
import { creerClientReleveHttp } from './ClientReleveHttp';

const json = (corps: unknown, status: number) => Promise.resolve(new Response(JSON.stringify(corps), { status }));
const operation = { date: '2026-11-03T08:00:00Z', description: 'x', type: 'DEBIT', montant: '30.00', soldeApres: '2100.00' };

describe('ClientReleveHttp', () => {
  it('[FR-005-05] lit les opérations du compte demandé', async () => {
    const appeler = vi.fn((_e: unknown, _o?: unknown) => json([operation], 200));
    const client = creerClientReleveHttp(appeler as unknown as typeof fetch);

    expect(await client.lireOperations(7)).toEqual({ statut: 'operations', operations: [operation] });
    expect(appeler).toHaveBeenCalledWith('/api/coeur-bancaire/comptes/7/operations', expect.objectContaining({ method: 'GET' }));
  });

  it('[FR-005-05] accepte un compte sans opération', async () => {
    const client = creerClientReleveHttp((() => json([], 200)) as unknown as typeof fetch);

    expect(await client.lireOperations(1)).toEqual({ statut: 'operations', operations: [] });
  });

  it.each([
    ['un 403', () => json({ message: 'Accès refusé' }, 403)],
    ['un 404', () => json({ message: 'Ressource introuvable' }, 404)],
    ['un 500', () => json({}, 500)],
    ['un corps qui n’est pas une liste', () => json({ a: 1 }, 200)],
    ['une opération sans type connu', () => json([{ ...operation, type: 'AUTRE' }], 200)],
    ['une opération sans description', () => json([{ ...operation, description: undefined }], 200)],
    ['une opération sans date', () => json([{ ...operation, date: 3 }], 200)],
    ['un montant mal formé', () => json([{ ...operation, montant: '30' }], 200)],
    ['un solde après mal formé', () => json([{ ...operation, soldeApres: 'x' }], 200)],
    ['un élément nul', () => json([null], 200)],
    ['un corps illisible', () => Promise.resolve(new Response('pas du json', { status: 200 }))],
    ['une coupure', () => Promise.reject(new Error('réseau'))],
  ])('[FR-005-05] rend technique pour %s', async (_nom, reponse) => {
    const client = creerClientReleveHttp(reponse as unknown as typeof fetch);

    expect(await client.lireOperations(1)).toEqual({ statut: 'technique' });
  });
});
