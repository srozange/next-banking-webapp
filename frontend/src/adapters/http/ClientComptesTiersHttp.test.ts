import { describe, expect, it, vi } from 'vitest';
import { CHEMIN_CONNEXION, CHEMIN_POWENS, CHEMIN_RAFRAICHISSEMENT, creerClientComptesTiersHttp } from './ClientComptesTiersHttp';

function repondre(statut: number, corps: string) {
  return vi.fn<typeof fetch>(() => Promise.resolve(new Response(statut === 204 ? null : corps, { status: statut })));
}

const reseauCoupe = () => vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')));
const ENTETES = { Accept: 'application/json' };

const ETAT =
  '{"connecte":true,"comptes":[{"id":1,"nom":"Courant","type":"checking","solde":"120.50","soldeAbsent":false,"iban":"FR76"},' +
  '{"id":2,"nom":"Livret","type":null,"solde":null,"soldeAbsent":true,"iban":null}],"compteSelectionne":1,' +
  '"transactions":[{"date":"2026-10-01","libelle":"Cafe","montant":"-3.50"}],"message":null,"patrimoineTotal":"120.50"}';

describe('ClientComptesTiersHttp', () => {
  it('[FR-012-04] lit l’état en GET', async () => {
    const appeler = repondre(200, ETAT);

    const reponse = await creerClientComptesTiersHttp(appeler).lireEtat();

    expect(reponse).toEqual({
      statut: 'ok',
      donnees: {
        connecte: true,
        comptes: [
          { id: 1, nom: 'Courant', type: 'checking', solde: '120.50', soldeAbsent: false, iban: 'FR76' },
          { id: 2, nom: 'Livret', type: null, solde: null, soldeAbsent: true, iban: null },
        ],
        compteSelectionne: 1,
        transactions: [{ date: '2026-10-01', libelle: 'Cafe', montant: '-3.50' }],
        message: '',
        patrimoineTotal: '120.50',
      },
    });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_POWENS, { method: 'GET', headers: ENTETES });
    expect(CHEMIN_POWENS).toBe('/api/open-banking-conseiller/powens');
  });

  it('[BUG-042] un solde nul sans indicateur est tenu pour absent, un solde absent n’a jamais de valeur', async () => {
    const corps =
      '{"connecte":false,"comptes":[{"id":3,"nom":"A","solde":null,"soldeAbsent":false},{"id":4,"nom":"B","solde":"9.00","soldeAbsent":true}],' +
      '"transactions":[],"message":"M","patrimoineTotal":"0.00"}';

    const reponse = await creerClientComptesTiersHttp(repondre(200, corps)).lireEtat();

    expect(reponse).toMatchObject({
      donnees: {
        compteSelectionne: null,
        message: 'M',
        comptes: [
          { id: 3, solde: null, soldeAbsent: true, type: null, iban: null },
          { id: 4, solde: null, soldeAbsent: true },
        ],
      },
    });
  });

  it('[FR-012-05] lance la connexion en POST et rend l’adresse', async () => {
    const appeler = repondre(200, '{"adresse":"https://webview.powens/connect"}');

    const reponse = await creerClientComptesTiersHttp(appeler).connecter();

    expect(reponse).toEqual({ statut: 'ok', donnees: 'https://webview.powens/connect' });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_CONNEXION, { method: 'POST', headers: ENTETES });
  });

  it('[FR-012-13] sélectionne un compte en POST', async () => {
    const appeler = repondre(200, ETAT);

    const reponse = await creerClientComptesTiersHttp(appeler).selectionner(12);

    expect(reponse.statut).toBe('ok');
    expect(appeler).toHaveBeenCalledWith(`${CHEMIN_POWENS}/comptes/12/selection`, { method: 'POST', headers: ENTETES });
  });

  it('[FR-012-16] rafraîchit en POST', async () => {
    const appeler = repondre(200, ETAT);

    expect((await creerClientComptesTiersHttp(appeler).rafraichir()).statut).toBe('ok');
    expect(appeler).toHaveBeenCalledWith(CHEMIN_RAFRAICHISSEMENT, { method: 'POST', headers: ENTETES });
  });

  it('[FR-012-15] déconnecte en DELETE, réponse 204 sans corps', async () => {
    const appeler = repondre(204, '');

    await expect(creerClientComptesTiersHttp(appeler).deconnecter()).resolves.toEqual({ statut: 'ok', donnees: null });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_CONNEXION, { method: 'DELETE', headers: ENTETES });
  });

  it('[FR-012-01] un 401 est non authentifié', async () => {
    await expect(creerClientComptesTiersHttp(repondre(401, '{}')).lireEtat()).resolves.toEqual({ statut: 'nonAuthentifie' });
  });

  it('[BUG-043] un 503 est une indisponibilité de Powens', async () => {
    const client = creerClientComptesTiersHttp(repondre(503, '{"message":"Service Powens temporairement indisponible."}'));

    await expect(client.selectionner(1)).resolves.toEqual({ statut: 'indisponible' });
    await expect(client.connecter()).resolves.toEqual({ statut: 'indisponible' });
  });

  it.each([
    ['autre statut', repondre(500, '{}')],
    ['404', repondre(404, '{"message":"Ressource introuvable"}')],
    ['corps illisible', repondre(200, 'pas du json')],
    ['état inattendu', repondre(200, '{"connecte":"oui"}')],
    ['état sans tableau', repondre(200, '[]')],
    ['compte invalide', repondre(200, '{"connecte":true,"comptes":[{"id":"x"}],"transactions":[],"message":"","patrimoineTotal":"0.00"}')],
    ['solde mal formé', repondre(200, '{"connecte":true,"comptes":[{"id":1,"nom":"A","solde":"1.5","soldeAbsent":false}],"transactions":[],"message":"","patrimoineTotal":"0.00"}')],
    ['transaction invalide', repondre(200, '{"connecte":true,"comptes":[],"transactions":[{"date":1}],"message":"","patrimoineTotal":"0.00"}')],
    ['total mal formé', repondre(200, '{"connecte":true,"comptes":[],"transactions":[],"message":"","patrimoineTotal":"abc"}')],
    ['sélection mal typée', repondre(200, '{"connecte":true,"comptes":[],"compteSelectionne":"1","transactions":[],"message":"","patrimoineTotal":"0.00"}')],
    ['réseau coupé', reseauCoupe()],
  ])('[FR-012-04] %s : erreur technique', async (_cas, appeler) => {
    await expect(creerClientComptesTiersHttp(appeler).lireEtat()).resolves.toEqual({ statut: 'technique' });
  });

  it('[FR-012-05] une adresse absente ou vide est une erreur technique', async () => {
    await expect(creerClientComptesTiersHttp(repondre(200, '{"adresse":""}')).connecter()).resolves.toEqual({ statut: 'technique' });
    await expect(creerClientComptesTiersHttp(repondre(200, '[]')).connecter()).resolves.toEqual({ statut: 'technique' });
  });

  it('[FR-012-15] une déconnexion qui ne répond pas 204 est une erreur technique', async () => {
    await expect(creerClientComptesTiersHttp(repondre(200, '{}')).deconnecter()).resolves.toEqual({ statut: 'technique' });
  });
});
