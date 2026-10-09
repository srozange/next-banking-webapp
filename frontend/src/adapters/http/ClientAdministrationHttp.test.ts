import { describe, expect, it, vi } from 'vitest';
import {
  CHEMIN_ADMIN_CLIENTS,
  CHEMIN_ADMIN_INDICATEURS_IDENTITE,
  CHEMIN_ADMIN_INDICATEURS_PRETS,
  CHEMIN_ADMIN_UTILISATEURS,
  cheminEtatClient,
  creerClientAdministrationHttp,
} from './ClientAdministrationHttp';

function repondre(statut: number, corps: string) {
  return vi.fn<typeof fetch>(() => Promise.resolve(new Response(corps, { status: statut })));
}

const reseauCoupe = () => vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')));
const OPTIONS = { method: 'GET', headers: { Accept: 'application/json' } };
const TECHNIQUE = { statut: 'erreur', message: 'Une erreur technique est survenue' };

const JEAN =
  '{"clientId":1,"nomComplet":"Jean Dupont","login":"user1","email":"jean.dupont@example.com","telephone":"+33601020304",' +
  '"role":"CLIENT","actif":true,"nomConseiller":"Sophie Durand","dateCreation":"2024-02-01"}';
const ALICE = '{"utilisateurId":10,"nomComplet":"Alice Martin","login":"admin","email":"alice.martin@demobank.fr","role":"ADMIN","actif":true}';

describe('ClientAdministrationHttp', () => {
  it('[FR-016-02] lit les indicateurs d’identité en GET, sans paramètre', async () => {
    const appeler = repondre(200, '{"totalClients":4,"clientsActifs":3,"conseillers":2,"totalUtilisateurs":7}');

    const reponse = await creerClientAdministrationHttp(appeler).lireIndicateursIdentite();

    expect(reponse).toEqual({ statut: 'ok', donnees: { totalClients: 4, clientsActifs: 3, conseillers: 2, totalUtilisateurs: 7 } });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_ADMIN_INDICATEURS_IDENTITE, OPTIONS);
    expect(CHEMIN_ADMIN_INDICATEURS_IDENTITE).toBe('/api/identite-clients/administration/indicateurs');
  });

  it('[FR-016-02] lit l’indicateur des prêts en attente sur credits', async () => {
    const appeler = repondre(200, '{"pretsEnAttente":2,"montantEnAttente":"15000.00"}');

    const reponse = await creerClientAdministrationHttp(appeler).lireIndicateursPrets();

    expect(reponse).toEqual({ statut: 'ok', donnees: { pretsEnAttente: 2, montantEnAttente: '15000.00' } });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_ADMIN_INDICATEURS_PRETS, OPTIONS);
    expect(CHEMIN_ADMIN_INDICATEURS_PRETS).toBe('/api/credits/administration/indicateurs');
  });

  it('[FR-016-08][FR-016-09][FR-016-10] lit les clients, téléphone et conseiller absents possibles', async () => {
    const appeler = repondre(
      200,
      `[${JEAN},{"clientId":2,"nomComplet":"Marie Lebrun","login":"user2","email":"m@example.com","telephone":null,"role":"CLIENT",` +
        '"actif":false,"dateCreation":"2023-11-20"}]',
    );

    const reponse = await creerClientAdministrationHttp(appeler).lireClients();

    expect(reponse).toEqual({
      statut: 'ok',
      donnees: [
        {
          clientId: 1, nomComplet: 'Jean Dupont', login: 'user1', email: 'jean.dupont@example.com', telephone: '+33601020304',
          role: 'CLIENT', actif: true, nomConseiller: 'Sophie Durand', dateCreation: '2024-02-01',
        },
        {
          clientId: 2, nomComplet: 'Marie Lebrun', login: 'user2', email: 'm@example.com', telephone: null, role: 'CLIENT',
          actif: false, nomConseiller: null, dateCreation: '2023-11-20',
        },
      ],
    });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_ADMIN_CLIENTS, OPTIONS);
    expect(CHEMIN_ADMIN_CLIENTS).toBe('/api/identite-clients/administration/clients');
  });

  it('[FR-016-13] lit tous les utilisateurs', async () => {
    const appeler = repondre(200, `[${ALICE}]`);

    const reponse = await creerClientAdministrationHttp(appeler).lireUtilisateurs();

    expect(reponse).toEqual({
      statut: 'ok',
      donnees: [{ utilisateurId: 10, nomComplet: 'Alice Martin', login: 'admin', email: 'alice.martin@demobank.fr', role: 'ADMIN', actif: true }],
    });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_ADMIN_UTILISATEURS, OPTIONS);
    expect(CHEMIN_ADMIN_UTILISATEURS).toBe('/api/identite-clients/administration/utilisateurs');
  });

  it('[FR-016-02] un 401 est non authentifié', async () => {
    const client = creerClientAdministrationHttp(repondre(401, '{"message":"Authentification requise"}'));

    expect(await client.lireIndicateursIdentite()).toEqual({ statut: 'nonAuthentifie' });
  });

  it('[FR-016-02] un autre statut rend le message du corps, sinon l’erreur technique', async () => {
    expect(
      await creerClientAdministrationHttp(repondre(500, '{"message":"Service crédits indisponible"}')).lireIndicateursPrets(),
    ).toEqual({ statut: 'erreur', message: 'Service crédits indisponible' });
    expect(await creerClientAdministrationHttp(repondre(403, '{"message":"Accès refusé"}')).lireClients()).toEqual({
      statut: 'erreur',
      message: 'Accès refusé',
    });
    expect(await creerClientAdministrationHttp(repondre(500, 'illisible')).lireIndicateursPrets()).toEqual(TECHNIQUE);
    expect(await creerClientAdministrationHttp(repondre(500, '{"message":3}')).lireUtilisateurs()).toEqual(TECHNIQUE);
    expect(await creerClientAdministrationHttp(repondre(500, '[]')).lireUtilisateurs()).toEqual(TECHNIQUE);
  });

  it('[FR-016-02] le réseau coupé rend l’erreur technique', async () => {
    expect(await creerClientAdministrationHttp(reseauCoupe()).lireIndicateursIdentite()).toEqual(TECHNIQUE);
  });

  it.each([
    ['indicateurs d’identité', 'lireIndicateursIdentite', '{"totalClients":4,"clientsActifs":"3","conseillers":2,"totalUtilisateurs":7}'],
    ['indicateurs d’identité nuls', 'lireIndicateursIdentite', 'null'],
    ['montant flottant', 'lireIndicateursPrets', '{"pretsEnAttente":2,"montantEnAttente":15000}'],
    ['montant sans décimales', 'lireIndicateursPrets', '{"pretsEnAttente":2,"montantEnAttente":"15000"}'],
    ['nombre non entier', 'lireIndicateursPrets', '{"pretsEnAttente":1.5,"montantEnAttente":"1.00"}'],
    ['clients hors liste', 'lireClients', '{}'],
    ['date de création illisible', 'lireClients', JEAN.replace('2024-02-01', '01/02/2024')],
    ['date de création absente', 'lireClients', `[${JEAN.replace(',"dateCreation":"2024-02-01"', '')}]`],
    ['client sans rôle', 'lireClients', `[${JEAN.replace('"role":"CLIENT",', '')}]`],
    ['client nul', 'lireClients', '[null]'],
    ['utilisateur sans actif', 'lireUtilisateurs', `[${ALICE.replace(',"actif":true', '')}]`],
    ['utilisateur nul', 'lireUtilisateurs', '[null]'],
  ] as const)('[FR-016-13] un corps hors contrat (%s) rend l’erreur technique', async (_cas, methode, corps) => {
    expect(await creerClientAdministrationHttp(repondre(200, corps))[methode]()).toEqual(TECHNIQUE);
  });

  it('[FR-016-13] le fetch global est utilisé par défaut', async () => {
    const appeler = vi.fn<typeof fetch>(() => Promise.resolve(new Response('[]', { status: 200 })));
    vi.stubGlobal('fetch', appeler);
    try {
      expect(await creerClientAdministrationHttp().lireUtilisateurs()).toEqual({ statut: 'ok', donnees: [] });
      expect(appeler).toHaveBeenCalledWith(CHEMIN_ADMIN_UTILISATEURS, OPTIONS);
    } finally {
      vi.unstubAllGlobals();
    }
  });

  describe('changerEtatClient', () => {
    const POST = { method: 'POST', headers: { Accept: 'application/json' } };

    it('[FR-016-11] chemins exacts de l’API ADMIN d’identite-clients', () => {
      expect(cheminEtatClient(3, false)).toBe('/api/identite-clients/clients/3/desactivation');
      expect(cheminEtatClient(3, true)).toBe('/api/identite-clients/clients/3/activation');
    });

    it('[FR-016-11] [FR-001-07] un seul appel part, en POST vers identite-clients, sans corps ni paramètre ; 204 est un succès', async () => {
      const appeler = vi.fn<typeof fetch>(() => Promise.resolve(new Response(null, { status: 204 })));

      expect(await creerClientAdministrationHttp(appeler).changerEtatClient(3, false)).toEqual({ statut: 'ok', donnees: null });
      expect(appeler).toHaveBeenCalledExactlyOnceWith('/api/identite-clients/clients/3/desactivation', POST);
      const options = appeler.mock.calls[0]?.[1];
      expect(options?.body).toBeUndefined();
    });

    it('[FR-016-11] l’activation appelle /activation ; un 2xx avec corps illisible reste un succès', async () => {
      const appeler = repondre(200, 'illisible');

      expect(await creerClientAdministrationHttp(appeler).changerEtatClient(3, true)).toEqual({ statut: 'ok', donnees: null });
      expect(appeler).toHaveBeenCalledExactlyOnceWith('/api/identite-clients/clients/3/activation', POST);
    });

    it('[FR-016-11] un 401 est non authentifié', async () => {
      expect(await creerClientAdministrationHttp(repondre(401, '{"message":"Authentification requise"}')).changerEtatClient(3, false)).toEqual({
        statut: 'nonAuthentifie',
      });
    });

    it('[FR-016-11] 403 et 500 rendent le message du corps ; sinon l’erreur technique', async () => {
      expect(await creerClientAdministrationHttp(repondre(403, '{"message":"Accès refusé"}')).changerEtatClient(3, false)).toEqual({
        statut: 'erreur',
        message: 'Accès refusé',
      });
      expect(
        await creerClientAdministrationHttp(repondre(500, '{"message":"Une erreur technique est survenue"}')).changerEtatClient(3, true),
      ).toEqual({ statut: 'erreur', message: 'Une erreur technique est survenue' });
      expect(await creerClientAdministrationHttp(repondre(404, 'illisible')).changerEtatClient(3, false)).toEqual(TECHNIQUE);
      expect(await creerClientAdministrationHttp(repondre(500, '{"message":3}')).changerEtatClient(3, false)).toEqual(TECHNIQUE);
      expect(await creerClientAdministrationHttp(repondre(500, '[]')).changerEtatClient(3, false)).toEqual(TECHNIQUE);
    });

    it('[FR-016-11] le réseau coupé rend l’erreur technique', async () => {
      expect(await creerClientAdministrationHttp(reseauCoupe()).changerEtatClient(3, false)).toEqual(TECHNIQUE);
    });
  });
});
