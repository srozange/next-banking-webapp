import { describe, expect, it, vi } from 'vitest';
import { creerClientBeneficiairesHttp } from './ClientBeneficiairesHttp';

const json = (corps: unknown, status: number) => Promise.resolve(new Response(JSON.stringify(corps), { status }));
const beneficiaire = { id: 3, libelle: 'Loyer', titulaire: 'Marie', iban: 'FR76***0189', bic: 'AGRIFRPP', banque: 'Banque', zoneSepa: true };

describe('ClientBeneficiairesHttp.lister', () => {
  it('[FR-013-02] lit les bénéficiaires du client', async () => {
    const appeler = vi.fn((_e: unknown, _o?: unknown) => json([beneficiaire], 200));
    const client = creerClientBeneficiairesHttp(appeler as unknown as typeof fetch);

    expect(await client.lister()).toEqual({ statut: 'liste', beneficiaires: [beneficiaire] });
    expect(appeler).toHaveBeenCalledWith('/api/coeur-bancaire/beneficiaires', expect.objectContaining({ method: 'GET' }));
  });

  it('[FR-013-03] rend nuls le BIC et la banque absents ou nuls', async () => {
    const sans = { id: 4, libelle: 'a', titulaire: 'b', iban: 'FR35***9010', bic: null, zoneSepa: false };
    const client = creerClientBeneficiairesHttp((() => json([sans], 200)) as unknown as typeof fetch);

    expect(await client.lister()).toEqual({ statut: 'liste', beneficiaires: [{ ...sans, bic: null, banque: null }] });
  });

  it('[FR-013-02] accepte une liste vide', async () => {
    const client = creerClientBeneficiairesHttp((() => json([], 200)) as unknown as typeof fetch);

    expect(await client.lister()).toEqual({ statut: 'liste', beneficiaires: [] });
  });

  it.each([
    ['un 500', () => json({}, 500)],
    ['un 403', () => json({}, 403)],
    ['un corps qui n’est pas une liste', () => json({ a: 1 }, 200)],
    ['un élément nul', () => json([null], 200)],
    ['un identifiant texte', () => json([{ ...beneficiaire, id: '3' }], 200)],
    ['un libellé manquant', () => json([{ ...beneficiaire, libelle: undefined }], 200)],
    ['un titulaire manquant', () => json([{ ...beneficiaire, titulaire: undefined }], 200)],
    ['un IBAN manquant', () => json([{ ...beneficiaire, iban: 4 }], 200)],
    ['un BIC numérique', () => json([{ ...beneficiaire, bic: 1 }], 200)],
    ['une banque numérique', () => json([{ ...beneficiaire, banque: 1 }], 200)],
    ['une zone SEPA texte', () => json([{ ...beneficiaire, zoneSepa: 'oui' }], 200)],
    ['un corps illisible', () => Promise.resolve(new Response('pas du json', { status: 200 }))],
    ['une coupure', () => Promise.reject(new Error('réseau'))],
  ])('[FR-013-02] rend technique pour %s', async (_nom, reponse) => {
    const client = creerClientBeneficiairesHttp(reponse as unknown as typeof fetch);

    expect(await client.lister()).toEqual({ statut: 'technique' });
  });
});

describe('ClientBeneficiairesHttp.supprimer', () => {
  it('[FR-013-09] supprime par DELETE sur l’identifiant', async () => {
    const appeler = vi.fn((_e: unknown, _o?: unknown) => Promise.resolve(new Response(null, { status: 204 })));
    const client = creerClientBeneficiairesHttp(appeler as unknown as typeof fetch);

    expect(await client.supprimer(3)).toEqual({ statut: 'supprime' });
    expect(appeler).toHaveBeenCalledWith('/api/coeur-bancaire/beneficiaires/3', expect.objectContaining({ method: 'DELETE' }));
  });

  it.each([403, 404])('[FR-013-10] un %i est un refus', async (status) => {
    const client = creerClientBeneficiairesHttp((() => json({ message: 'Erreur lors de la suppression.' }, status)) as unknown as typeof fetch);

    expect(await client.supprimer(3)).toEqual({ statut: 'refuse' });
  });

  it.each([
    ['un 500', () => json({}, 500)],
    ['un 400', () => json({}, 400)],
    ['une coupure', () => Promise.reject(new Error('réseau'))],
  ])('[FR-013-09] rend technique pour %s', async (_nom, reponse) => {
    const client = creerClientBeneficiairesHttp(reponse as unknown as typeof fetch);

    expect(await client.supprimer(3)).toEqual({ statut: 'technique' });
  });
});
