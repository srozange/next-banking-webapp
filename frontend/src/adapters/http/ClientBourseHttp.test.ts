import { describe, expect, it, vi } from 'vitest';
import { CHEMIN_HISTORIQUE, CHEMIN_WATCHLIST, creerClientBourseHttp } from './ClientBourseHttp';

function repondre(statut: number, corps: string) {
  return vi.fn<typeof fetch>(() => Promise.resolve(new Response(statut === 204 ? null : corps, { status: statut })));
}

const reseauCoupe = () => vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')));

const VALEUR = '{"id":1,"symbole":"AI.PA","nom":"Air Liquide","marche":"Euronext Paris","devise":"EUR"}';
const POINT = '{"date":"2026-10-02","ouverture":"167.3000","haut":"171.2600","bas":"166.9200","cloture":"170.1600","volume":1133144}';
const HISTORIQUE = `{"symbole":"AI.PA","periode":"1M","points":[${POINT}],"graphique":"<svg></svg>"}`;
const ENTETES = { Authorization: 'Bearer jeton', Accept: 'application/json' };

describe('ClientBourseHttp', () => {
  it('[FR-009-10][FR-029-11] lit la watchlist en GET avec le jeton en Bearer, dans l’ordre du service', async () => {
    const appeler = repondre(200, `[${VALEUR},{"id":2,"symbole":"BNP.PA","nom":"BNP Paribas","marche":"Euronext Paris","devise":"EUR"}]`);

    const reponse = await creerClientBourseHttp(appeler).lireWatchlist('jeton');

    expect(reponse).toMatchObject({ statut: 'ok', donnees: [{ id: 1, symbole: 'AI.PA' }, { id: 2, symbole: 'BNP.PA' }] });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_WATCHLIST, { method: 'GET', headers: ENTETES });
    expect(CHEMIN_WATCHLIST).toBe('/api/bourse/watchlist');
  });

  it('[FR-009-10] rend une watchlist vide', async () => {
    await expect(creerClientBourseHttp(repondre(200, '[]')).lireWatchlist('j')).resolves.toEqual({ statut: 'ok', donnees: [] });
  });

  it('[FR-009-03] lit l’historique avec le symbole et la période encodés', async () => {
    const appeler = repondre(200, HISTORIQUE);

    const reponse = await creerClientBourseHttp(appeler).lireHistorique('jeton', 'BRK B&C', '1Y');

    expect(reponse).toEqual({
      statut: 'ok',
      donnees: {
        symbole: 'AI.PA',
        periode: '1M',
        points: [{ date: '2026-10-02', ouverture: '167.3000', haut: '171.2600', bas: '166.9200', cloture: '170.1600', volume: 1133144 }],
        graphique: '<svg></svg>',
      },
    });
    expect(appeler).toHaveBeenCalledWith(`${CHEMIN_HISTORIQUE}?symbole=BRK%20B%26C&periode=1Y`, { method: 'GET', headers: ENTETES });
    expect(CHEMIN_HISTORIQUE).toBe('/api/bourse/historique');
  });

  it.each([
    ['sans le champ graphique', '{"symbole":"X","periode":"1M","points":[]}'],
    ['au graphique null', '{"symbole":"X","periode":"1M","points":[],"graphique":null}'],
  ])('[FR-009-09] un historique %s est vide, sans graphique', async (_cas, corps) => {
    await expect(creerClientBourseHttp(repondre(200, corps)).lireHistorique('j', 'X', '1M')).resolves.toEqual({
      statut: 'ok',
      donnees: { symbole: 'X', periode: '1M', points: [], graphique: null },
    });
  });

  it('[FR-009-01] rend non authentifié sur 401, pour chaque appel', async () => {
    const client = creerClientBourseHttp(repondre(401, '{"message":"Authentification requise"}'));
    const demande = { symbole: 'A', nom: 'B', marche: 'NYSE', devise: 'USD' };

    await expect(client.lireWatchlist('j')).resolves.toEqual({ statut: 'nonAuthentifie' });
    await expect(client.lireHistorique('j', 'A', '1M')).resolves.toEqual({ statut: 'nonAuthentifie' });
    await expect(client.ajouter('j', demande)).resolves.toEqual({ statut: 'nonAuthentifie' });
    await expect(client.retirer('j', 1)).resolves.toEqual({ statut: 'nonAuthentifie' });
  });

  it.each([
    ['500', () => repondre(500, '{"message":"Une erreur technique est survenue"}')],
    ['403', () => repondre(403, '{"message":"Accès refusé"}')],
    ['200 au JSON illisible', () => repondre(200, 'pas du json')],
    ['réseau coupé', reseauCoupe],
  ])('[FR-009-09] rend une erreur technique pour chaque appel : %s', async (_cas, appeler) => {
    const client = creerClientBourseHttp(appeler());

    await expect(client.lireWatchlist('j')).resolves.toEqual({ statut: 'technique' });
    await expect(client.lireHistorique('j', 'A', '1M')).resolves.toEqual({ statut: 'technique' });
    await expect(client.ajouter('j', { symbole: 'A', nom: 'B', marche: 'NYSE', devise: 'USD' })).resolves.toEqual({ statut: 'technique' });
    await expect(client.retirer('j', 1)).resolves.toEqual({ statut: 'technique' });
  });

  it.each([
    ['une watchlist qui n’est pas une liste', '{}'],
    ['une watchlist avec un null', '[null]'],
    ['une valeur sans id numérique', '[{"id":"1","symbole":"A","nom":"B","marche":"C","devise":"D"}]'],
    ['une valeur sans symbole', '[{"id":1,"nom":"B","marche":"C","devise":"D"}]'],
    ['une valeur sans nom', '[{"id":1,"symbole":"A","marche":"C","devise":"D"}]'],
    ['une valeur sans marché', '[{"id":1,"symbole":"A","nom":"B","devise":"D"}]'],
    ['une valeur sans devise', '[{"id":1,"symbole":"A","nom":"B","marche":"C"}]'],
    ['une valeur qui est une liste', '[[]]'],
  ])('rend une erreur technique pour %s', async (_cas, corps) => {
    await expect(creerClientBourseHttp(repondre(200, corps)).lireWatchlist('j')).resolves.toEqual({ statut: 'technique' });
  });

  it.each([
    ['un corps null', 'null'],
    ['sans symbole', '{"periode":"1M","points":[]}'],
    ['sans période', '{"symbole":"X","points":[]}'],
    ['sans points', '{"symbole":"X","periode":"1M"}'],
    ['au graphique non textuel', '{"symbole":"X","periode":"1M","points":[],"graphique":3}'],
    ['au point null', '{"symbole":"X","periode":"1M","points":[null]}'],
    ['au point sans date', `{"symbole":"X","periode":"1M","points":[${POINT.replace('"date":"2026-10-02",', '')}]}`],
    ['à la date mal formée', `{"symbole":"X","periode":"1M","points":[${POINT.replace('2026-10-02', '02/10/2026')}]}`],
    ['au prix flottant mal formé', `{"symbole":"X","periode":"1M","points":[${POINT.replace('"167.3000"', '"1e3"')}]}`],
    ['au prix numérique', `{"symbole":"X","periode":"1M","points":[${POINT.replace('"171.2600"', '171.26')}]}`],
    ['au prix bas absent', `{"symbole":"X","periode":"1M","points":[${POINT.replace('"bas":"166.9200",', '')}]}`],
    ['à la clôture absente', `{"symbole":"X","periode":"1M","points":[${POINT.replace('"cloture":"170.1600",', '')}]}`],
    ['au volume textuel', `{"symbole":"X","periode":"1M","points":[${POINT.replace('1133144', '"1133144"')}]}`],
  ])('[FR-009-09] rend une erreur technique pour un historique %s', async (_cas, corps) => {
    await expect(creerClientBourseHttp(repondre(200, corps)).lireHistorique('j', 'X', '1M')).resolves.toEqual({ statut: 'technique' });
  });

  it('[FR-009-12] ajoute en POST JSON avec le jeton et rend la valeur créée (201)', async () => {
    const appeler = repondre(201, VALEUR);
    const demande = { symbole: 'sg.pa', nom: 'Société Générale', marche: 'Euronext Paris', devise: 'EUR' };

    const reponse = await creerClientBourseHttp(appeler).ajouter('jeton', demande);

    expect(reponse).toMatchObject({ statut: 'ok', donnees: { id: 1, symbole: 'AI.PA' } });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_WATCHLIST, {
      method: 'POST',
      headers: { ...ENTETES, 'Content-Type': 'application/json' },
      body: JSON.stringify(demande),
    });
  });

  it.each([
    'Le symbole est obligatoire (ex: AMZN, SG.PA)',
    'Le nom de la valeur est obligatoire',
    'Le symbole « BNP.PA » existe déjà dans votre watchlist',
  ])('[FR-009-11][FR-029-09] un refus 400 rend son texte tel quel : %s', async (message) => {
    const reponse = await creerClientBourseHttp(repondre(400, JSON.stringify({ message }))).ajouter('j', {
      symbole: '',
      nom: '',
      marche: 'NYSE',
      devise: 'USD',
    });

    expect(reponse).toEqual({ statut: 'refusee', message });
  });

  it.each([
    ['un 400 sans message', '{}'],
    ['un 400 au message vide', '{"message":""}'],
    ['un 400 au message non textuel', '{"message":3}'],
    ['un 400 au corps null', 'null'],
    ['un 400 au corps liste', '[]'],
    ['un 400 illisible', 'pas du json'],
  ])('l’ajout est une erreur technique pour %s', async (_cas, corps) => {
    await expect(creerClientBourseHttp(repondre(400, corps)).ajouter('j', { symbole: 'A', nom: 'B', marche: 'NYSE', devise: 'USD' })).resolves.toEqual({
      statut: 'technique',
    });
  });

  it('l’ajout est une erreur technique si la valeur créée est illisible', async () => {
    await expect(
      creerClientBourseHttp(repondre(201, '{}')).ajouter('j', { symbole: 'A', nom: 'B', marche: 'NYSE', devise: 'USD' }),
    ).resolves.toEqual({ statut: 'technique' });
  });

  it('[FR-009-13] retire en DELETE sur l’identifiant (204), sans corps', async () => {
    const appeler = repondre(204, '');

    await expect(creerClientBourseHttp(appeler).retirer('jeton', 7)).resolves.toEqual({ statut: 'ok', donnees: null });
    expect(appeler).toHaveBeenCalledWith(`${CHEMIN_WATCHLIST}/7`, { method: 'DELETE', headers: ENTETES });
  });

  it('[FR-009-13] une valeur déjà retirée (404) est un retrait accompli', async () => {
    await expect(creerClientBourseHttp(repondre(404, '{"message":"Ressource introuvable"}')).retirer('j', 7)).resolves.toEqual({
      statut: 'ok',
      donnees: null,
    });
  });

  it('appelle fetch global par défaut', async () => {
    const appeler = repondre(401, '{}');
    vi.stubGlobal('fetch', appeler);
    try {
      await expect(creerClientBourseHttp().lireWatchlist('j')).resolves.toEqual({ statut: 'nonAuthentifie' });
      expect(appeler).toHaveBeenCalledOnce();
    } finally {
      vi.unstubAllGlobals();
    }
  });
});
