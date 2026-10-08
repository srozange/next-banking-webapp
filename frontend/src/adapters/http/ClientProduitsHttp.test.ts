import { describe, expect, it, vi } from 'vitest';
import { CHEMIN_CATALOGUE, CHEMIN_TRAITEMENTS, creerClientProduitsHttp } from './ClientProduitsHttp';

function repondre(statut: number, corps: string) {
  return vi.fn<typeof fetch>(() => Promise.resolve(new Response(corps, { status: statut })));
}

const reseauCoupe = () => vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')));
const ENTETES = { Accept: 'application/json' };

const EPARGNE = '{"id":1,"nom":"Livret A","description":"d","taux":"0.0300","depotMin":"10.00","capitalGaranti":true,"dureeMaxMois":null}';
const CREDIT = '{"id":2,"nom":"Credit Personnel","description":null,"taux":"0.0598","dureeMaxMois":84,"ltvMax":null,"assuranceRequise":false}';
const INVEST = '{"id":3,"nom":"SCPI","description":"d","secteur":"Immobilier","niveauRisque":4,"rendementAttendu":"0.0650"}';
const CATALOGUE = `{"epargne":[${EPARGNE}],"credit":[${CREDIT}],"investissement":[${INVEST}]}`;
const TRAITEMENT = '{"job":"CLOTURE","debut":"2026-10-06T02:00+02:00","fin":null,"dureeMs":0,"enregistrements":null,"statut":"SUCCESS","details":null}';

describe('ClientProduitsHttp', () => {
  it('[FR-011-02] lit le catalogue en GET', async () => {
    const appeler = repondre(200, CATALOGUE);

    const reponse = await creerClientProduitsHttp(appeler).lireCatalogue();

    expect(reponse).toEqual({
      statut: 'ok',
      donnees: {
        epargne: [{ id: 1, nom: 'Livret A', description: 'd', taux: '0.0300', depotMin: '10.00', capitalGaranti: true, dureeMaxMois: null }],
        credit: [{ id: 2, nom: 'Credit Personnel', description: null, taux: '0.0598', dureeMaxMois: 84, ltvMax: null, assuranceRequise: false }],
        investissement: [{ id: 3, nom: 'SCPI', description: 'd', secteur: 'Immobilier', niveauRisque: 4, rendementAttendu: '0.0650' }],
      },
    });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_CATALOGUE, { method: 'GET', headers: ENTETES });
    expect(CHEMIN_CATALOGUE).toBe('/api/open-banking-conseiller/produits');
  });

  it('[FR-011-08] rend un catalogue aux groupes vides, et des champs facultatifs omis comme absents', async () => {
    await expect(
      creerClientProduitsHttp(repondre(200, '{"epargne":[],"credit":[],"investissement":[]}')).lireCatalogue(),
    ).resolves.toEqual({ statut: 'ok', donnees: { epargne: [], credit: [], investissement: [] } });
    const sansFacultatifs = '{"id":3,"nom":"X","capitalGaranti":false}';
    const reponse = await creerClientProduitsHttp(
      repondre(200, `{"epargne":[${sansFacultatifs}],"credit":[],"investissement":[]}`),
    ).lireCatalogue();
    expect(reponse).toEqual({
      statut: 'ok',
      donnees: {
        epargne: [{ id: 3, nom: 'X', description: null, taux: null, depotMin: null, capitalGaranti: false, dureeMaxMois: null }],
        credit: [],
        investissement: [],
      },
    });
  });

  it('[FR-011-09] lit le journal avec la limite de 50, dans l’ordre du service', async () => {
    const appeler = repondre(200, `[${TRAITEMENT},${TRAITEMENT.replace('CLOTURE', 'AUTRE')}]`);

    const reponse = await creerClientProduitsHttp(appeler).lireTraitements();

    expect(reponse).toMatchObject({
      statut: 'ok',
      donnees: [
        { job: 'CLOTURE', debut: '2026-10-06T02:00+02:00', fin: null, dureeMs: 0, enregistrements: null, statut: 'SUCCESS', details: null },
        { job: 'AUTRE' },
      ],
    });
    expect(appeler).toHaveBeenCalledWith(`${CHEMIN_TRAITEMENTS}?limite=50`, { method: 'GET', headers: ENTETES });
    expect(CHEMIN_TRAITEMENTS).toBe('/api/coeur-bancaire/traitements');
  });

  it('[FR-011-09] rend un journal vide ; début, fin et champs facultatifs omis sont absents', async () => {
    await expect(creerClientProduitsHttp(repondre(200, '[]')).lireTraitements()).resolves.toEqual({ statut: 'ok', donnees: [] });
    await expect(creerClientProduitsHttp(repondre(200, '[{"job":"J","dureeMs":5}]')).lireTraitements()).resolves.toEqual({
      statut: 'ok',
      donnees: [{ job: 'J', debut: null, fin: null, dureeMs: 5, enregistrements: null, statut: null, details: null }],
    });
  });

  it('[FR-010-01] rend non authentifié sur 401, pour chaque appel', async () => {
    const client = creerClientProduitsHttp(repondre(401, ''));

    await expect(client.lireCatalogue()).resolves.toEqual({ statut: 'nonAuthentifie' });
    await expect(client.lireTraitements()).resolves.toEqual({ statut: 'nonAuthentifie' });
  });

  it('[FR-011-02] 5xx ou réseau coupé : erreur technique', async () => {
    for (const appeler of [repondre(500, '{}'), repondre(204, ''), reseauCoupe()]) {
      const client = creerClientProduitsHttp(appeler);
      await expect(client.lireCatalogue()).resolves.toEqual({ statut: 'technique' });
      await expect(client.lireTraitements()).resolves.toEqual({ statut: 'technique' });
    }
  });

  it.each([
    ['corps non JSON', 'pas du json'],
    ['corps nul', 'null'],
    ['tableau au lieu d’une enveloppe', '[]'],
    ['groupe manquant', '{"epargne":[],"credit":[]}'],
    ['groupe qui n’est pas une liste', '{"epargne":{},"credit":[],"investissement":[]}'],
    ['produit qui n’est pas un objet', '{"epargne":[1],"credit":[],"investissement":[]}'],
    ['taux hors format décimal', `{"epargne":[${EPARGNE.replace('"0.0300"', '"3 %"')}],"credit":[],"investissement":[]}`],
    ['taux numérique (flottant)', `{"epargne":[${EPARGNE.replace('"0.0300"', '0.03')}],"credit":[],"investissement":[]}`],
    ['dépôt à une décimale', `{"epargne":[${EPARGNE.replace('"10.00"', '"10.0"')}],"credit":[],"investissement":[]}`],
    ['booléen manquant', `{"epargne":[],"credit":[${CREDIT.replace('"assuranceRequise":false', '"assuranceRequise":"non"')}],"investissement":[]}`],
    ['risque non entier', `{"epargne":[],"credit":[],"investissement":[${INVEST.replace('"niveauRisque":4', '"niveauRisque":"4"')}]}`],
    ['nom manquant', `{"epargne":[],"credit":[],"investissement":[${INVEST.replace('"nom":"SCPI",', '')}]}`],
  ])('[FR-011-05] catalogue illisible (%s) : erreur technique', async (_cas, corps) => {
    await expect(creerClientProduitsHttp(repondre(200, corps)).lireCatalogue()).resolves.toEqual({ statut: 'technique' });
  });

  it.each([
    ['enveloppe au lieu d’une liste', '{}'],
    ['élément non objet', '[1]'],
    ['début illisible', `[${TRAITEMENT.replace('2026-10-06T02:00+02:00', 'hier')}]`],
    ['durée non entière', `[${TRAITEMENT.replace('"dureeMs":0', '"dureeMs":"0"')}]`],
    ['job manquant', `[${TRAITEMENT.replace('"job":"CLOTURE",', '')}]`],
  ])('[FR-011-09] journal illisible (%s) : erreur technique', async (_cas, corps) => {
    await expect(creerClientProduitsHttp(repondre(200, corps)).lireTraitements()).resolves.toEqual({ statut: 'technique' });
  });

  it('appelle le fetch global par défaut', async () => {
    const global = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('[]', { status: 200 }));

    await expect(creerClientProduitsHttp().lireTraitements()).resolves.toEqual({ statut: 'ok', donnees: [] });
    expect(global).toHaveBeenCalled();
    global.mockRestore();
  });
});
