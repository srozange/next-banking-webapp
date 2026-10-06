import { describe, expect, it, vi } from 'vitest';
import { creerClientRevueHttp } from './ClientRevueHttp';

const json = (corps: unknown, status: number) => Promise.resolve(new Response(JSON.stringify(corps), { status }));

const virement = {
  virementId: 12,
  clientId: 7,
  nomClient: 'Claire Martin',
  montant: '12000.00',
  compteDestinataire: 'FR76-0001-0002',
  motifSuspension: 'Score cumulé 130 - signaux : LARGE_AMOUNT',
  heureButoir: '2026-11-06T09:15:00Z',
};
const traite = { virement, issue: 'LIBERE', dateIssue: '2026-11-05T10:00:00Z' };

describe('[FR-016-19] ClientRevueHttp', () => {
  it('lit la file des virements suspendus et l’historique des virements traités', async () => {
    const appeler = vi.fn((chemin: unknown, _o?: unknown) =>
      chemin === '/api/coeur-bancaire/revue/virements-suspendus' ? json([virement, { ...virement, nomClient: null }], 200) : json([traite], 200),
    );
    const client = creerClientRevueHttp(appeler as unknown as typeof fetch);

    expect(await client.lireFile()).toEqual({ statut: 'liste', virements: [virement, { ...virement, nomClient: null }] });
    expect(await client.lireHistorique()).toEqual({ statut: 'liste', virements: [traite] });
    expect(appeler).toHaveBeenCalledWith('/api/coeur-bancaire/revue/virements-traites', expect.objectContaining({ method: 'GET' }));
  });

  it.each([
    ['un statut d’erreur (403)', () => json({ message: 'Accès refusé' }, 403)],
    ['un corps qui n’est pas une liste', () => json({ a: 1 }, 200)],
    ['une ligne mal formée', () => json([{ virementId: 'x' }], 200)],
    ['une issue inconnue', () => json([{ ...traite, issue: 'AUTRE' }], 200)],
    ['une ligne traitée sans virement', () => json([{ issue: 'LIBERE', dateIssue: 'x' }], 200)],
    ['un corps illisible', () => Promise.resolve(new Response('pas du json', { status: 200 }))],
    ['une coupure', () => Promise.reject(new Error('réseau'))],
  ])('rend technique pour %s à la lecture', async (_nom, reponse) => {
    const client = creerClientRevueHttp(reponse as unknown as typeof fetch);
    expect(await client.lireFile()).toEqual({ statut: 'technique' });
    expect(await client.lireHistorique()).toEqual({ statut: 'technique' });
  });

  it('poste la décision sur le virement avec son motif', async () => {
    const appeler = vi.fn((_e: unknown, _o?: RequestInit) => json({ virementId: 12, issue: 'ACCEPTEE' }, 200));
    const client = creerClientRevueHttp(appeler as unknown as typeof fetch);

    expect(await client.decider('liberer', 12, 'vérifié par téléphone')).toEqual({ statut: 'enregistre' });
    expect(await client.decider('rejeter', 13, 'client injoignable')).toEqual({ statut: 'enregistre' });

    const [chemin, options] = appeler.mock.calls[0] as [string, RequestInit];
    expect(chemin).toBe('/api/coeur-bancaire/revue/virements/12/liberer');
    expect(options.method).toBe('POST');
    expect(JSON.parse(options.body as string)).toEqual({ motif: 'vérifié par téléphone' });
    expect((appeler.mock.calls[1] as [string, RequestInit])[0]).toBe('/api/coeur-bancaire/revue/virements/13/rejeter');
  });

  it('traduit les refus du service : issue refusée (409), motif manquant (400), autres erreurs', async () => {
    const cas: Array<[() => Promise<Response>, unknown]> = [
      [() => json({ message: 'x', issue: 'REFUSEE_SOLDE_INSUFFISANT' }, 409), { statut: 'refuse', issue: 'REFUSEE_SOLDE_INSUFFISANT' }],
      [() => json({ message: 'x', issue: 'REFUSEE_HEURE_BUTOIR' }, 409), { statut: 'refuse', issue: 'REFUSEE_HEURE_BUTOIR' }],
      [() => json({ message: 'x', issue: 'AUTRE' }, 409), { statut: 'erreur', message: 'x' }],
      [() => json({ message: 'Veuillez indiquer un motif' }, 400), { statut: 'motif' }],
      [() => json({ message: 'Requête invalide' }, 400), { statut: 'erreur', message: 'Requête invalide' }],
      [() => json({ message: 'Accès refusé' }, 403), { statut: 'erreur', message: 'Accès refusé' }],
      [() => json({ message: 4 }, 500), { statut: 'erreur', message: null }],
      [() => json(null, 404), { statut: 'erreur', message: null }],
      [() => Promise.resolve(new Response('pas du json', { status: 500 })), { statut: 'erreur', message: null }],
      [() => Promise.reject(new Error('réseau')), { statut: 'erreur', message: null }],
    ];
    for (const [reponse, attendu] of cas) {
      const client = creerClientRevueHttp(reponse as unknown as typeof fetch);
      expect(await client.decider('liberer', 12, 'ok')).toEqual(attendu);
    }
  });
});
