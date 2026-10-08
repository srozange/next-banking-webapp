import { describe, expect, it, vi } from 'vitest';
import { creerClientPlafondsHttp } from './ClientPlafondsHttp';

const json = (corps: unknown, status: number) => Promise.resolve(new Response(JSON.stringify(corps), { status }));
const COMMANDE = { categorie: 'Alimentation', mois: '2026-10', plafond: '500.00' };
const http = (reponse: () => Promise<Response>) => creerClientPlafondsHttp(reponse as unknown as typeof fetch);

describe('ClientPlafondsHttp.enregistrer', () => {
  it('[FR-006-07] envoie un PUT JSON et rend enregistré sur 200', async () => {
    const appeler = vi.fn((_e: unknown, _o?: unknown) => json({ id: 1 }, 200));
    const client = creerClientPlafondsHttp(appeler as unknown as typeof fetch);

    expect(await client.enregistrer(COMMANDE)).toEqual({ statut: 'enregistre' });
    expect(appeler).toHaveBeenCalledWith(
      '/api/coeur-bancaire/budget/plafonds',
      expect.objectContaining({ method: 'PUT', body: JSON.stringify(COMMANDE) }),
    );
  });

  it('[FR-006-05] rend le texte d’un 400', async () => {
    expect(await http(() => json({ message: 'Veuillez saisir un plafond valide' }, 400)).enregistrer(COMMANDE)).toEqual({
      statut: 'refuse',
      message: 'Veuillez saisir un plafond valide',
    });
  });

  it.each([
    ['un 400 sans message', () => json({}, 400)],
    ['un 400 au corps nul', () => json(null, 400)],
    ['un 400 au corps illisible', () => Promise.resolve(new Response('x', { status: 400 }))],
    ['un 500', () => json({ message: 'détail' }, 500)],
    ['un 404', () => json({ message: 'Ressource introuvable' }, 404)],
    ['une coupure', () => Promise.reject(new Error('réseau'))],
  ])('[FR-006-07] rend technique pour %s', async (_nom, reponse) => {
    expect(await http(reponse).enregistrer(COMMANDE)).toEqual({ statut: 'technique' });
  });
});

describe('ClientPlafondsHttp.supprimer', () => {
  it('[FR-006-08] envoie un DELETE et rend supprimé sur 204', async () => {
    const appeler = vi.fn((_e: unknown, _o?: unknown) => Promise.resolve(new Response(null, { status: 204 })));
    const client = creerClientPlafondsHttp(appeler as unknown as typeof fetch);

    expect(await client.supprimer(7)).toEqual({ statut: 'supprime' });
    expect(appeler).toHaveBeenCalledWith('/api/coeur-bancaire/budget/plafonds/7', expect.objectContaining({ method: 'DELETE' }));
  });

  it.each([
    ['un 404', () => json({ message: 'Ressource introuvable' }, 404)],
    ['un 500', () => json({}, 500)],
    ['une coupure', () => Promise.reject(new Error('réseau'))],
  ])('[FR-006-08] rend technique pour %s', async (_nom, reponse) => {
    expect(await http(reponse).supprimer(7)).toEqual({ statut: 'technique' });
  });
});
