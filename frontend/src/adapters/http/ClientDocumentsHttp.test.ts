import { describe, expect, it, vi } from 'vitest';
import { creerClientDocumentsHttp } from './ClientDocumentsHttp';

const json = (corps: unknown, status: number) => Promise.resolve(new Response(JSON.stringify(corps), { status }));
const page = { aujourdhui: '2026-11-17', comptes: [{ compteId: 1, numero: 'FR76-0001-0001', type: 'COURANT' }] };

describe('ClientDocumentsHttp', () => {
  it('[FR-008-03] lit les comptes du jeton et aujourdhui, sans paramètre', async () => {
    const appeler = vi.fn((_e: unknown, _o?: unknown) => json(page, 200));
    const client = creerClientDocumentsHttp(appeler as unknown as typeof fetch);

    expect(await client.lirePage()).toEqual({ statut: 'page', aujourdhui: '2026-11-17', comptes: page.comptes });
    expect(appeler).toHaveBeenCalledWith('/api/documents/comptes', expect.objectContaining({ method: 'GET' }));
  });

  it.each([
    ['un 401', () => json({ message: 'Authentification requise' }, 401)],
    ['un 500', () => json({}, 500)],
    ['un corps nul', () => json(null, 200)],
    ['une date mal formée', () => json({ ...page, aujourdhui: '17/11/2026' }, 200)],
    ['une date absente', () => json({ comptes: [] }, 200)],
    ['des comptes absents', () => json({ aujourdhui: '2026-11-17' }, 200)],
    ['un compte nul', () => json({ ...page, comptes: [null] }, 200)],
    ['un compte sans identifiant entier', () => json({ ...page, comptes: [{ compteId: 'x', numero: 'a', type: 'COURANT' }] }, 200)],
    ['un compte sans numéro', () => json({ ...page, comptes: [{ compteId: 1, numero: 2, type: 'COURANT' }] }, 200)],
    ['un compte sans type', () => json({ ...page, comptes: [{ compteId: 1, numero: 'a', type: 3 }] }, 200)],
    ['un corps illisible', () => Promise.resolve(new Response('pas du json', { status: 200 }))],
    ['une coupure', () => Promise.reject(new Error('coupure'))],
  ])('[FR-008-01] lirePage rend « technique » sur %s', async (_cas, reponse) => {
    const client = creerClientDocumentsHttp((() => reponse()) as unknown as typeof fetch);

    expect(await client.lirePage()).toEqual({ statut: 'technique' });
  });

  it.each([
    [{ type: 'RIB', compteId: 1 } as const, 'type=RIB&compteId=1'],
    [{ type: 'ATTESTATION', compteId: 1 } as const, 'type=ATTESTATION&compteId=1'],
    [{ type: 'RELEVE', compteId: 1, annee: 2026, mois: 3 } as const, 'type=RELEVE&compteId=1&annee=2026&mois=3'],
    [{ type: 'IFU', annee: 2025 } as const, 'type=IFU&annee=2025'],
  ])('[FR-008-06] demande le document %j, sans identifiant de client', async (demande, requete) => {
    const pdf = new Response('%PDF-1.4', { status: 200, headers: { 'Content-Type': 'application/pdf' } });
    const appeler = vi.fn((_e: unknown, _o?: unknown) => Promise.resolve(pdf));
    const client = creerClientDocumentsHttp(appeler as unknown as typeof fetch);

    const reponse = await client.telecharger(demande);

    expect(appeler).toHaveBeenCalledWith(`/api/documents/telechargement?${requete}`, expect.objectContaining({ method: 'GET' }));
    expect(reponse.statut).toBe('pdf');
    expect(reponse.statut === 'pdf' && (await reponse.contenu.text())).toBe('%PDF-1.4');
  });

  it.each([
    ['un 401', 401],
    ['un 403', 403],
    ['un 404', 404],
    ['un 500', 500],
  ])('[FR-008-06] télécharger rend « technique » sur %s', async (_cas, status) => {
    const client = creerClientDocumentsHttp((() => json({ message: 'x' }, status)) as unknown as typeof fetch);

    expect(await client.telecharger({ type: 'RIB', compteId: 1 })).toEqual({ statut: 'technique' });
  });

  it('[FR-008-06] télécharger rend « technique » sur une coupure', async () => {
    const client = creerClientDocumentsHttp((() => Promise.reject(new Error('coupure'))) as unknown as typeof fetch);

    expect(await client.telecharger({ type: 'IFU', annee: 2025 })).toEqual({ statut: 'technique' });
  });
});
