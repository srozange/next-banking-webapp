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
  it('[FR-007-03] poste la saisie en JSON et rend la simulation', async () => {
    const appeler = repondre(200, JSON.stringify(SIMULATION));

    await expect(creerClientPretsHttp(appeler).simuler(SAISIE)).resolves.toEqual({
      statut: 'simulation',
      simulation: SIMULATION,
    });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_SIMULATIONS, {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      body: '{"montant":"10000","tauxAnnuel":"3.50","dureeMois":"36"}',
    });
    expect(CHEMIN_SIMULATIONS).toBe('/api/credits/simulations');
  });

  it('[FR-007-02] rend le message du service sur 400', async () => {
    await expect(
      creerClientPretsHttp(repondre(400, '{"message":"Duree invalide (1-360 mois)"}')).simuler(SAISIE),
    ).resolves.toEqual({ statut: 'refusee', message: 'Duree invalide (1-360 mois)' });
  });

  it('[FR-007-01] rend non authentifié sur 401', async () => {
    await expect(
      creerClientPretsHttp(repondre(401, '{"message":"Authentification requise"}')).simuler(SAISIE),
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
    await expect(creerClientPretsHttp(appeler).simuler(SAISIE)).resolves.toEqual({ statut: 'technique' });
  });
});

describe('ClientPretsHttp.lister', () => {
  it('[FR-007-08] lit la liste en GET', async () => {
    const appeler = repondre(200, JSON.stringify([PRET, { ...PRET, id: 2, objet: null }]));

    await expect(creerClientPretsHttp(appeler).lister()).resolves.toEqual({
      statut: 'prets',
      prets: [PRET, { ...PRET, id: 2, objet: null }],
    });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_PRETS, {
      method: 'GET',
      headers: { Accept: 'application/json' },
    });
    expect(CHEMIN_PRETS).toBe('/api/credits/prets');
  });

  it('[FR-007-08] rend une liste vide', async () => {
    await expect(creerClientPretsHttp(repondre(200, '[]')).lister()).resolves.toEqual({ statut: 'prets', prets: [] });
  });

  it('[FR-007-01] rend non authentifié sur 401', async () => {
    await expect(creerClientPretsHttp(repondre(401, '{}')).lister()).resolves.toEqual({ statut: 'nonAuthentifie' });
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
    await expect(creerClientPretsHttp(appeler).lister()).resolves.toEqual({ statut: 'technique' });
  });
});

describe('ClientPretsHttp.deposer', () => {
  const DEMANDE = { type: 'CONSOMMATION', montant: '10000', tauxAnnuel: '3.50', dureeMois: '300', objet: 'Voiture' };

  it('[FR-007-07] poste la saisie en JSON et rend le prêt créé sur 201', async () => {
    const appeler = repondre(201, JSON.stringify(PRET));

    await expect(creerClientPretsHttp(appeler).deposer(DEMANDE)).resolves.toEqual({ statut: 'deposee', pret: PRET });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_PRETS, {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      body: '{"type":"CONSOMMATION","montant":"10000","tauxAnnuel":"3.50","dureeMois":"300","objet":"Voiture"}',
    });
  });

  it.each([
    [400, 'Duree invalide'],
    [503, 'Service credit temporairement indisponible, veuillez reessayer plus tard'],
  ])('[FR-007-05] rend le message du service sur %i', async (statut, message) => {
    await expect(
      creerClientPretsHttp(repondre(statut, JSON.stringify({ message }))).deposer(DEMANDE),
    ).resolves.toEqual({ statut: 'refusee', message });
  });

  it('[FR-007-01] rend non authentifié sur 401', async () => {
    await expect(
      creerClientPretsHttp(repondre(401, '{"message":"Authentification requise"}')).deposer(DEMANDE),
    ).resolves.toEqual({ statut: 'nonAuthentifie' });
  });

  it.each([
    ['500', repondre(500, '{"message":"Erreur : la demande de prêt n\'a pas été enregistrée. Veuillez réessayer."}')],
    ['403', repondre(403, '{"message":"Accès refusé"}')],
    ['400 sans message textuel', repondre(400, '{"message":3}')],
    ['400 au corps null', repondre(400, 'null')],
    ['201 au JSON illisible', repondre(201, 'pas du json')],
    ['201 au prêt mal formé', repondre(201, JSON.stringify({ ...PRET, montant: '5000' }))],
    ['200 inattendu', repondre(200, JSON.stringify(PRET))],
    ['réseau coupé', vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')))],
  ])('[FR-024-02] rend une erreur technique : %s', async (_cas, appeler) => {
    await expect(creerClientPretsHttp(appeler).deposer(DEMANDE)).resolves.toEqual({ statut: 'technique' });
  });
});

const ECHEANCE = {
  id: 5,
  numero: 5,
  dateEcheance: '2026-11-01',
  capital: '414.09',
  interets: '13.72',
  total: '427.81',
  capitalRestant: '2946.33',
  paye: false,
  datePaiement: null,
};
const ECHEANCE_PAYEE = { ...ECHEANCE, paye: true, datePaiement: '2026-10-06T08:30:01Z' };
const ENTETES_LECTURE = { Accept: 'application/json' };

describe('ClientPretsHttp.echeances', () => {
  it('[FR-007-11] lit les échéances du prêt', async () => {
    const appeler = repondre(200, JSON.stringify([ECHEANCE, ECHEANCE_PAYEE]));

    await expect(creerClientPretsHttp(appeler).echeances(7)).resolves.toEqual({
      statut: 'echeances',
      echeances: [ECHEANCE, ECHEANCE_PAYEE],
    });
    expect(appeler).toHaveBeenCalledWith('/api/credits/prets/7/echeances', { method: 'GET', headers: ENTETES_LECTURE });
  });

  it('[FR-007-11] rend non authentifié sur 401', async () => {
    await expect(creerClientPretsHttp(repondre(401, '{}')).echeances(7)).resolves.toEqual({ statut: 'nonAuthentifie' });
  });

  it.each([
    ['403', repondre(403, '{"message":"Accès refusé"}')],
    ['404', repondre(404, '{"message":"Ressource introuvable"}')],
    ['200 au JSON illisible', repondre(200, 'pas du json')],
    ['200 hors tableau', repondre(200, '{}')],
    ['200 au corps null', repondre(200, '[null]')],
    ['200 à l’échéance non objet', repondre(200, '[3]')],
    ['200 à la date mal formée', repondre(200, JSON.stringify([{ ...ECHEANCE, dateEcheance: '01/11/2026' }]))],
    ['200 au montant mal formé', repondre(200, JSON.stringify([{ ...ECHEANCE, total: '427.8' }]))],
    ['200 au paiement mal typé', repondre(200, JSON.stringify([{ ...ECHEANCE, datePaiement: 3 }]))],
    ['200 à l’indicateur mal typé', repondre(200, JSON.stringify([{ ...ECHEANCE, paye: 'oui' }]))],
    ['réseau coupé', vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')))],
  ])('[FR-007-11] rend une erreur technique : %s', async (_cas, appeler) => {
    await expect(creerClientPretsHttp(appeler).echeances(7)).resolves.toEqual({ statut: 'technique' });
  });
});

describe('ClientPretsHttp.payer', () => {
  it('[FR-007-12] poste le paiement de l’échéance sans corps et rend le succès', async () => {
    const appeler = repondre(200, JSON.stringify(ECHEANCE_PAYEE));

    await expect(creerClientPretsHttp(appeler).payer(5)).resolves.toEqual({ statut: 'faite' });
    expect(appeler).toHaveBeenCalledWith('/api/credits/echeances/5/paiement', { method: 'POST', headers: ENTETES_LECTURE });
  });

  it('[FR-007-12] rend non authentifié sur 401', async () => {
    await expect(creerClientPretsHttp(repondre(401, '{}')).payer(5)).resolves.toEqual({ statut: 'nonAuthentifie' });
  });

  it.each([
    ['403', repondre(403, '{"message":"Erreur : l\'échéance n\'a pas été marquée comme payée. Veuillez réessayer."}')],
    ['404', repondre(404, '{"message":"Ressource introuvable"}')],
    ['500', repondre(500, '{"message":"x"}')],
    ['200 au JSON illisible', repondre(200, 'pas du json')],
    ['200 à l’échéance mal formée', repondre(200, '{"id":5}')],
    ['réseau coupé', vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')))],
  ])('[FR-007-12] rend une erreur technique : %s', async (_cas, appeler) => {
    await expect(creerClientPretsHttp(appeler).payer(5)).resolves.toEqual({ statut: 'technique' });
  });
});

describe('ClientPretsHttp.supprimer', () => {
  it('[FR-007-13] envoie DELETE sur le prêt et rend le succès sur 204', async () => {
    const appeler = vi.fn<typeof fetch>(() => Promise.resolve(new Response(null, { status: 204 })));

    await expect(creerClientPretsHttp(appeler).supprimer(7)).resolves.toEqual({ statut: 'faite' });
    expect(appeler).toHaveBeenCalledWith('/api/credits/prets/7', { method: 'DELETE', headers: ENTETES_LECTURE });
  });

  it('[FR-007-13] rend non authentifié sur 401', async () => {
    await expect(creerClientPretsHttp(repondre(401, '{}')).supprimer(7)).resolves.toEqual({ statut: 'nonAuthentifie' });
  });

  it.each([
    ['403', repondre(403, '{"message":"x"}')],
    ['404', repondre(404, '{"message":"x"}')],
    ['500', repondre(500, '{"message":"x"}')],
    ['200 inattendu', repondre(200, '{}')],
    ['réseau coupé', vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')))],
  ])('[FR-007-13] rend une erreur technique : %s', async (_cas, appeler) => {
    await expect(creerClientPretsHttp(appeler).supprimer(7)).resolves.toEqual({ statut: 'technique' });
  });
});
