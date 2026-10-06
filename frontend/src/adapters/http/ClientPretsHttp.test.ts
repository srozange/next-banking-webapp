import { describe, expect, it, vi } from 'vitest';
import { CHEMIN_PRETS, CHEMIN_SIMULATIONS, creerClientPretsHttp } from './ClientPretsHttp';

function repondre(statut: number, corps: string) {
  return vi.fn<typeof fetch>(() => Promise.resolve(new Response(corps, { status: statut })));
}

const SAISIE = { montant: '10000', tauxAnnuel: '3.50', dureeMois: '36' };
const LIGNE = { numero: 1, capital: '263.85', interets: '29.17', total: '293.02', capitalRestant: '9736.15' };
const SIMULATION = { mensualite: '293.02', coutTotal: '10548.72', totalInterets: '548.72', echeancier: [LIGNE] };
const PRET = {
  id: 1,
  type: 'TRAVAUX',
  montant: '5000.00',
  tauxAnnuel: '4.9000',
  dureeMois: 24,
  mensualite: '218.63',
  coutTotal: '5247.12',
  totalInterets: '247.12',
  objet: 'Travaux',
  statut: 'EN_COURS',
  dateDebut: '2026-08-01',
  dateFin: '2028-08-01',
};

describe('ClientPretsHttp.simuler', () => {
  it('[FR-007-03] poste la saisie en JSON avec le jeton en Bearer et rend la simulation', async () => {
    const appeler = repondre(200, JSON.stringify(SIMULATION));

    await expect(creerClientPretsHttp(appeler).simuler('jeton', SAISIE)).resolves.toEqual({
      statut: 'simulation',
      simulation: SIMULATION,
    });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_SIMULATIONS, {
      method: 'POST',
      headers: { Authorization: 'Bearer jeton', Accept: 'application/json', 'Content-Type': 'application/json' },
      body: '{"montant":"10000","tauxAnnuel":"3.50","dureeMois":"36"}',
    });
    expect(CHEMIN_SIMULATIONS).toBe('/api/credits/simulations');
  });

  it('[FR-007-02] rend le message du service sur 400', async () => {
    await expect(
      creerClientPretsHttp(repondre(400, '{"message":"Duree invalide (1-360 mois)"}')).simuler('j', SAISIE),
    ).resolves.toEqual({ statut: 'refusee', message: 'Duree invalide (1-360 mois)' });
  });

  it('[FR-007-01] rend non authentifié sur 401', async () => {
    await expect(
      creerClientPretsHttp(repondre(401, '{"message":"Authentification requise"}')).simuler('j', SAISIE),
    ).resolves.toEqual({ statut: 'nonAuthentifie' });
  });

  it.each([
    ['500', repondre(500, '{"message":"Erreur simulation : le calcul n\'a pas pu être effectué. Veuillez réessayer."}')],
    ['403', repondre(403, '{"message":"Accès refusé"}')],
    ['400 sans message textuel', repondre(400, '{"message":3}')],
    ['400 au corps null', repondre(400, 'null')],
    ['200 au JSON illisible', repondre(200, 'pas du json')],
    ['200 au corps null', repondre(200, 'null')],
    ['200 sans échéancier', repondre(200, '{"mensualite":"1.00","coutTotal":"1.00","totalInterets":"0.00"}')],
    ['200 à la mensualité mal formée', repondre(200, JSON.stringify({ ...SIMULATION, mensualite: '293.2' }))],
    ['200 à la ligne non objet', repondre(200, JSON.stringify({ ...SIMULATION, echeancier: [3] }))],
    ['200 à la ligne sans numéro entier', repondre(200, JSON.stringify({ ...SIMULATION, echeancier: [{ ...LIGNE, numero: 1.5 }] }))],
    ['200 à la ligne au capital mal formé', repondre(200, JSON.stringify({ ...SIMULATION, echeancier: [{ ...LIGNE, capital: 1 }] }))],
    ['réseau coupé', vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')))],
  ])('[FR-007-03] rend une erreur technique : %s', async (_cas, appeler) => {
    await expect(creerClientPretsHttp(appeler).simuler('j', SAISIE)).resolves.toEqual({ statut: 'technique' });
  });
});

describe('ClientPretsHttp.lister', () => {
  it('[FR-007-08] lit la liste en GET avec le jeton en Bearer', async () => {
    const appeler = repondre(200, JSON.stringify([PRET, { ...PRET, id: 2, objet: null }]));

    await expect(creerClientPretsHttp(appeler).lister('jeton')).resolves.toEqual({
      statut: 'prets',
      prets: [PRET, { ...PRET, id: 2, objet: null }],
    });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_PRETS, {
      method: 'GET',
      headers: { Authorization: 'Bearer jeton', Accept: 'application/json' },
    });
    expect(CHEMIN_PRETS).toBe('/api/credits/prets');
  });

  it('[FR-007-08] rend une liste vide', async () => {
    await expect(creerClientPretsHttp(repondre(200, '[]')).lister('j')).resolves.toEqual({ statut: 'prets', prets: [] });
  });

  it('[FR-007-01] rend non authentifié sur 401', async () => {
    await expect(creerClientPretsHttp(repondre(401, '{}')).lister('j')).resolves.toEqual({ statut: 'nonAuthentifie' });
  });

  it.each([
    ['500', repondre(500, '{}')],
    ['200 au JSON illisible', repondre(200, 'pas du json')],
    ['200 qui n’est pas un tableau', repondre(200, '{}')],
    ['200 à l’élément non objet', repondre(200, '[3]')],
    ['200 au type inconnu', repondre(200, JSON.stringify([{ ...PRET, type: 'AUTRE' }]))],
    ['200 au statut inconnu', repondre(200, JSON.stringify([{ ...PRET, statut: 'AUTRE' }]))],
    ['200 au type non textuel', repondre(200, JSON.stringify([{ ...PRET, type: 3 }]))],
    ['200 au statut non textuel', repondre(200, JSON.stringify([{ ...PRET, statut: 3 }]))],
    ['200 au montant mal formé', repondre(200, JSON.stringify([{ ...PRET, montant: '5000' }]))],
    ['200 au taux mal formé', repondre(200, JSON.stringify([{ ...PRET, tauxAnnuel: 'x' }]))],
    ['200 au taux non textuel', repondre(200, JSON.stringify([{ ...PRET, tauxAnnuel: 4.9 }]))],
    ['200 à la durée non entière', repondre(200, JSON.stringify([{ ...PRET, dureeMois: '24' }]))],
    ['200 à l’identifiant non entier', repondre(200, JSON.stringify([{ ...PRET, id: 'a' }]))],
    ['200 à l’objet non textuel', repondre(200, JSON.stringify([{ ...PRET, objet: 3 }]))],
    ['200 à la date non textuelle', repondre(200, JSON.stringify([{ ...PRET, dateDebut: 3 }]))],
    ['200 à la date de fin non textuelle', repondre(200, JSON.stringify([{ ...PRET, dateFin: 3 }]))],
    ['réseau coupé', vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')))],
  ])('[FR-007-08] rend une erreur technique : %s', async (_cas, appeler) => {
    await expect(creerClientPretsHttp(appeler).lister('j')).resolves.toEqual({ statut: 'technique' });
  });
});
