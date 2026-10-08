import { describe, expect, it, vi } from 'vitest';
import { creerClientPipelinePretsHttp } from './ClientPipelinePretsHttp';

const json = (corps: unknown, status: number) => Promise.resolve(new Response(JSON.stringify(corps), { status }));

const pret = {
  id: 42,
  clientId: 1,
  nomClient: 'Jean Dupont',
  type: 'CONSOMMATION',
  montant: '10000.00',
  statut: 'EN_ATTENTE',
  dureeMois: 36,
  tauxAnnuel: '3.5000',
  mensualite: '293.02',
  objet: 'Voiture',
  dateDebut: '2026-10-01',
};

describe('ClientPipelinePretsHttp', () => {
  it('[FR-016-03][FR-016-12] lit le pipeline et l’historique, type et motif pouvant être nuls', async () => {
    const sansTypeNiMotif = { ...pret, id: 43, type: null, objet: null, statut: 'REFUSE' };
    const appeler = vi.fn((chemin: unknown, _o?: unknown) =>
      chemin === '/api/credits/administration/prets/en-attente' ? json([pret], 200) : json([pret, sansTypeNiMotif], 200),
    );
    const client = creerClientPipelinePretsHttp(appeler as unknown as typeof fetch);

    expect(await client.lireEnAttente()).toEqual({ statut: 'liste', prets: [pret] });
    expect(await client.lireHistorique()).toEqual({ statut: 'liste', prets: [pret, sansTypeNiMotif] });
    expect(appeler).toHaveBeenCalledWith('/api/credits/administration/prets', expect.objectContaining({ method: 'GET' }));
  });

  it.each([
    ['un statut d’erreur (403)', () => json({ message: 'Accès refusé' }, 403)],
    ['un corps qui n’est pas une liste', () => json({ a: 1 }, 200)],
    ['une ligne mal formée', () => json([{ id: 'x' }], 200)],
    ['un type inconnu', () => json([{ ...pret, type: 'AUTRE' }], 200)],
    ['un type qui n’est pas du texte', () => json([{ ...pret, type: 4 }], 200)],
    ['un statut inconnu', () => json([{ ...pret, statut: 'AUTRE' }], 200)],
    ['un motif numérique', () => json([{ ...pret, objet: 4 }], 200)],
    ['une ligne nulle', () => json([null], 200)],
    ['un corps illisible', () => Promise.resolve(new Response('pas du json', { status: 200 }))],
    ['une coupure', () => Promise.reject(new Error('réseau'))],
  ])('[FR-016-03][FR-016-12] rend technique pour %s à la lecture', async (_nom, reponse) => {
    const client = creerClientPipelinePretsHttp(reponse as unknown as typeof fetch);

    expect(await client.lireEnAttente()).toEqual({ statut: 'technique' });
    expect(await client.lireHistorique()).toEqual({ statut: 'technique' });
  });

  it('[FR-016-04][FR-016-05] poste l’approbation et le refus du prêt', async () => {
    const appeler = vi.fn((_e: unknown, _o?: RequestInit) => Promise.resolve(new Response(null, { status: 200 })));
    const client = creerClientPipelinePretsHttp(appeler as unknown as typeof fetch);

    expect(await client.decider('approuver', 42)).toEqual({ statut: 'effectuee' });
    expect(await client.decider('refuser', 43)).toEqual({ statut: 'effectuee' });

    const [chemin, options] = appeler.mock.calls[0] as [string, RequestInit];
    expect(chemin).toBe('/api/credits/administration/prets/42/approbation');
    expect(options.method).toBe('POST');
    expect((appeler.mock.calls[1] as [string, RequestInit])[0]).toBe('/api/credits/administration/prets/43/refus');
  });

  it.each([
    ['403', () => json({ message: 'Accès refusé' }, 403)],
    ['404', () => json({ message: 'Ressource introuvable' }, 404)],
    ['500', () => json({ message: 'Erreur lors du traitement du prêt. Veuillez réessayer.' }, 500)],
    ['une coupure', () => Promise.reject(new Error('réseau'))],
  ])('[FR-016-07] rend technique pour %s à la décision', async (_nom, reponse) => {
    const client = creerClientPipelinePretsHttp(reponse as unknown as typeof fetch);

    expect(await client.decider('approuver', 42)).toEqual({ statut: 'technique' });
    expect(await client.decider('refuser', 42)).toEqual({ statut: 'technique' });
  });
});
