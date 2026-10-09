import { describe, expect, it, vi } from 'vitest';
import {
  CHEMIN_PORTEFEUILLE_COMPTES,
  CHEMIN_PORTEFEUILLE_IDENTITES,
  CHEMIN_PORTEFEUILLE_PRETS,
  creerClientPortefeuilleHttp,
} from './ClientPortefeuilleHttp';

function repondre(statut: number, corps: string) {
  return vi.fn<typeof fetch>(() => Promise.resolve(new Response(corps, { status: statut })));
}

const reseauCoupe = () => vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')));
const OPTIONS = { method: 'GET', headers: { Accept: 'application/json' } };
const TECHNIQUE = { statut: 'erreur', message: 'Une erreur technique est survenue' };

const JEAN = '{"clientId":1,"nomComplet":"Jean Dupont","login":"user1","email":"jean@example.com","telephone":"+33601020304","actif":true}';
const MARIE = '{"clientId":2,"nomComplet":"Marie Lebrun","login":"user2","email":null,"telephone":null,"actif":false}';
const COMPTE = '{"compteId":10,"numero":"FR76-0001-0001","type":"COURANT","solde":"2130.00"}';
const PRET = '{"pretId":5,"type":null,"montant":"5000.00","mensualite":"146.51","statut":"DEFAUT"}';

describe('ClientPortefeuilleHttp', () => {
  it('[FR-015-02] lit l’identité du portefeuille en GET, sans paramètre', async () => {
    const appeler = repondre(200, `[${JEAN},${MARIE}]`);

    const reponse = await creerClientPortefeuilleHttp(appeler).lireIdentites();

    expect(reponse).toEqual({
      statut: 'ok',
      donnees: [
        { clientId: 1, nomComplet: 'Jean Dupont', login: 'user1', email: 'jean@example.com', telephone: '+33601020304', actif: true },
        { clientId: 2, nomComplet: 'Marie Lebrun', login: 'user2', email: null, telephone: null, actif: false },
      ],
    });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_PORTEFEUILLE_IDENTITES, OPTIONS);
    expect(CHEMIN_PORTEFEUILLE_IDENTITES).toBe('/api/identite-clients/conseiller/portefeuille');
  });

  it('[FR-015-02] un téléphone ou un email omis est absent', async () => {
    const reponse = await creerClientPortefeuilleHttp(
      repondre(200, '[{"clientId":3,"nomComplet":"A","login":"a","actif":true}]'),
    ).lireIdentites();

    expect(reponse).toEqual({ statut: 'ok', donnees: [{ clientId: 3, nomComplet: 'A', login: 'a', email: null, telephone: null, actif: true }] });
  });

  it('[FR-015-09] lit les comptes du portefeuille, codes tels quels', async () => {
    const appeler = repondre(200, `[{"clientId":1,"comptes":[${COMPTE}]},{"clientId":2,"comptes":[]}]`);

    const reponse = await creerClientPortefeuilleHttp(appeler).lireComptes();

    expect(reponse).toEqual({
      statut: 'ok',
      donnees: [
        { clientId: 1, comptes: [{ compteId: 10, numero: 'FR76-0001-0001', type: 'COURANT', solde: '2130.00' }] },
        { clientId: 2, comptes: [] },
      ],
    });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_PORTEFEUILLE_COMPTES, OPTIONS);
    expect(CHEMIN_PORTEFEUILLE_COMPTES).toBe('/api/coeur-bancaire/conseiller/portefeuille');
  });

  it('[FR-015-09] lit les prêts du portefeuille, type nul et statut DEFAUT tels quels', async () => {
    const appeler = repondre(200, `[{"clientId":1,"prets":[${PRET}]}]`);

    const reponse = await creerClientPortefeuilleHttp(appeler).lirePrets();

    expect(reponse).toEqual({
      statut: 'ok',
      donnees: [{ clientId: 1, prets: [{ pretId: 5, type: null, montant: '5000.00', mensualite: '146.51', statut: 'DEFAUT' }] }],
    });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_PORTEFEUILLE_PRETS, OPTIONS);
    expect(CHEMIN_PORTEFEUILLE_PRETS).toBe('/api/credits/conseiller/portefeuille');

    const avecType = await creerClientPortefeuilleHttp(
      repondre(200, '[{"clientId":1,"prets":[{"pretId":6,"type":"AUTO","montant":"1.00","mensualite":"1.00","statut":"EN_COURS"}]}]'),
    ).lirePrets();
    expect(avecType).toMatchObject({ statut: 'ok', donnees: [{ prets: [{ type: 'AUTO' }] }] });
  });

  it('[FR-015-02] 401 : non authentifié', async () => {
    await expect(creerClientPortefeuilleHttp(repondre(401, '{}')).lireIdentites()).resolves.toEqual({ statut: 'nonAuthentifie' });
  });

  it('[FR-015-02] autre statut : le message texte du corps, sinon l’erreur technique', async () => {
    await expect(
      creerClientPortefeuilleHttp(repondre(500, '{"message":"Service crédits indisponible"}')).lirePrets(),
    ).resolves.toEqual({ statut: 'erreur', message: 'Service crédits indisponible' });
    await expect(creerClientPortefeuilleHttp(repondre(503, '{"message":42}')).lirePrets()).resolves.toEqual(TECHNIQUE);
    await expect(creerClientPortefeuilleHttp(repondre(500, 'pas du json')).lireComptes()).resolves.toEqual(TECHNIQUE);
    await expect(creerClientPortefeuilleHttp(repondre(502, '[]')).lireComptes()).resolves.toEqual(TECHNIQUE);
  });

  it('[FR-015-02] réseau coupé : erreur technique', async () => {
    await expect(creerClientPortefeuilleHttp(reseauCoupe()).lireIdentites()).resolves.toEqual(TECHNIQUE);
  });

  it.each([
    ['identité non liste', 'lireIdentites', '{}'],
    ['identité sans actif', 'lireIdentites', '[{"clientId":1,"nomComplet":"A","login":"a"}]'],
    ['identité non objet', 'lireIdentites', '[1]'],
    ['identité email non texte', 'lireIdentites', '[{"clientId":1,"nomComplet":"A","login":"a","email":3,"actif":true}]'],
    ['identité téléphone non texte', 'lireIdentites', '[{"clientId":1,"nomComplet":"A","login":"a","telephone":3,"actif":true}]'],
    ['identité id non entier', 'lireIdentites', '[{"clientId":"1","nomComplet":"A","login":"a","actif":true}]'],
    ['identité nom absent', 'lireIdentites', '[{"clientId":1,"login":"a","actif":true}]'],
    ['identité login absent', 'lireIdentites', '[{"clientId":1,"nomComplet":"A","actif":true}]'],
    ['comptes non liste', 'lireComptes', '[{"clientId":1,"comptes":{}}]'],
    ['comptes sans clientId', 'lireComptes', '[{"comptes":[]}]'],
    ['compte non objet', 'lireComptes', '[{"clientId":1,"comptes":[null]}]'],
    ['solde flottant', 'lireComptes', '[{"clientId":1,"comptes":[{"compteId":1,"numero":"N","type":"COURANT","solde":2130.5}]}]'],
    ['compte sans numéro', 'lireComptes', '[{"clientId":1,"comptes":[{"compteId":1,"type":"COURANT","solde":"1.00"}]}]'],
    ['compte sans type', 'lireComptes', '[{"clientId":1,"comptes":[{"compteId":1,"numero":"N","solde":"1.00"}]}]'],
    ['compte sans id', 'lireComptes', '[{"clientId":1,"comptes":[{"numero":"N","type":"COURANT","solde":"1.00"}]}]'],
    ['client des comptes non objet', 'lireComptes', '["x"]'],
    ['prêts non liste', 'lirePrets', '[{"clientId":1}]'],
    ['prêts sans clientId', 'lirePrets', '[{"prets":[]}]'],
    ['client des prêts non objet', 'lirePrets', '[3]'],
    ['prêt non objet', 'lirePrets', '[{"clientId":1,"prets":[[]]}]'],
    ['prêt sans id', 'lirePrets', '[{"clientId":1,"prets":[{"type":null,"montant":"1.00","mensualite":"1.00","statut":"EN_COURS"}]}]'],
    ['prêt type non texte', 'lirePrets', '[{"clientId":1,"prets":[{"pretId":1,"type":1,"montant":"1.00","mensualite":"1.00","statut":"EN_COURS"}]}]'],
    ['prêt montant hors format', 'lirePrets', '[{"clientId":1,"prets":[{"pretId":1,"montant":"1","mensualite":"1.00","statut":"EN_COURS"}]}]'],
    ['prêt mensualité absente', 'lirePrets', '[{"clientId":1,"prets":[{"pretId":1,"montant":"1.00","statut":"EN_COURS"}]}]'],
    ['prêt sans statut', 'lirePrets', '[{"clientId":1,"prets":[{"pretId":1,"montant":"1.00","mensualite":"1.00"}]}]'],
  ] as const)('[FR-015-02] corps hors contrat (%s) : erreur technique', async (_cas, methode, corps) => {
    await expect(creerClientPortefeuilleHttp(repondre(200, corps))[methode]()).resolves.toEqual(TECHNIQUE);
  });

  it('appelle fetch par défaut', async () => {
    const fetch = vi.fn<typeof globalThis.fetch>(() => Promise.resolve(new Response('[]', { status: 200 })));
    vi.stubGlobal('fetch', fetch);
    try {
      await expect(creerClientPortefeuilleHttp().lireIdentites()).resolves.toEqual({ statut: 'ok', donnees: [] });
      expect(fetch).toHaveBeenCalledWith(CHEMIN_PORTEFEUILLE_IDENTITES, OPTIONS);
    } finally {
      vi.unstubAllGlobals();
    }
  });
});
