// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { ClientProduits, ReponseProduits } from '../../../application/consulterProduits';
import type { Catalogue, Traitement } from '../../../domain/produits';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const epargne = (id: number, nom: string, taux: string | null, depotMin: string | null, capitalGaranti: boolean, dureeMaxMois: number | null) => ({
  id, nom, description: `Description ${nom}`, taux, depotMin, capitalGaranti, dureeMaxMois,
});
const credit = (id: number, nom: string, taux: string | null, dureeMaxMois: number | null, ltvMax: string | null, assuranceRequise: boolean) => ({
  id, nom, description: null, taux, dureeMaxMois, ltvMax, assuranceRequise,
});
const investissement = (id: number, nom: string, secteur: string | null, niveauRisque: number | null, rendementAttendu: string | null) => ({
  id, nom, description: `Description ${nom}`, secteur, niveauRisque, rendementAttendu,
});

/** Données de référence du service : 4 produits d'épargne, 4 de crédit, 5 d'investissement. */
const REFERENCE: Catalogue = {
  epargne: [
    epargne(1, 'CEL - Compte Epargne Logement', '0.0175', '300.00', true, null),
    epargne(2, 'Livret A', '0.0300', '10.00', true, null),
    epargne(3, 'Livret Demo Bank', null, null, false, null),
    epargne(4, 'PEL - Plan Epargne Logement', '0.0225', '225.00', true, 120),
  ],
  credit: [
    credit(5, 'Credit Auto Classique', '0.0449', 72, null, false),
    credit(6, 'Credit Immobilier Classique', '0.0365', 300, '0.8000', true),
    credit(7, 'Credit Personnel', '0.0598', 84, null, false),
    credit(8, 'Rachat de Credits', null, null, '0.7000', true),
  ],
  investissement: [
    investissement(9, 'ETF Monde Diversifie', 'Actions mondiales', 3, '0.0720'),
    investissement(10, 'Fonds Euro Garanti', 'Obligations souveraines', 1, '0.0280'),
    investissement(11, 'PEA Actions Europe', 'Actions Europe', 4, '0.0950'),
    investissement(12, 'Private Equity Croissance', null, 5, '0.0650'),
    investissement(13, 'SCPI Rendement Plus', 'Immobilier tertiaire', 0, null),
  ],
};

const traitement = (job: string, debut: string | null, fin: string | null, dureeMs: number, statut: string | null = 'SUCCESS'): Traitement => ({
  job, debut, fin, dureeMs, enregistrements: debut === null ? null : 12, statut, details: debut === null ? null : 'Termine',
});

const JOURNAL: Traitement[] = [
  traitement('INTERETS', '2026-10-06T02:30:05+02:00', '2026-10-06T02:30:07+02:00', 2000),
  traitement('CLOTURE', '2026-10-06T02:00+02:00', null, 0, 'FAILED'),
  traitement('ARCHIVAGE', null, null, 0, 'PARTIAL'),
];

function service(catalogue: ReponseProduits<Catalogue> = { statut: 'ok', donnees: REFERENCE }, journal: ReponseProduits<readonly Traitement[]> = { statut: 'ok', donnees: JOURNAL }) {
  return {
    lireCatalogue: vi.fn<ClientProduits['lireCatalogue']>(() => Promise.resolve(catalogue)),
    lireTraitements: vi.fn<ClientProduits['lireTraitements']>(() => Promise.resolve(journal)),
  };
}

function ouvrir(client = service(), roles: string[] = ['CLIENT']) {
  localStorage.setItem('nb.jeton', jetonDeTest({ sub: '1', roles }));
  const rendu = rendreApplication('/produits', undefined, undefined, undefined, undefined, undefined, undefined, undefined, client);
  return { client, ...rendu };
}

async function ecranCharge(client = service(), roles: string[] = ['CLIENT']) {
  const ouverture = ouvrir(client, roles);
  await waitFor(() => expect(screen.getByTestId('produits-contenu').getAttribute('aria-busy')).toBeNull());
  return ouverture;
}

const lignes = (testId: string) =>
  within(screen.getByTestId(testId))
    .getAllByRole('row')
    .map((ligne) => Array.from(ligne.querySelectorAll('th, td')).map((cellule) => cellule.textContent));

describe('écran Produits : accès', () => {
  it.each(['CLIENT', 'CONSEILLER', 'ADMIN'])('[FR-010-01][FR-011-01] %s ouvre /produits : même écran, sans champ de saisie', async (role) => {
    const { routeur } = await ecranCharge(undefined, [role]);

    expect(routeur.state.location.pathname).toBe('/produits');
    expect(screen.getByRole('heading', { name: 'Catalogue des produits' })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Historique des traitements nocturnes' })).toBeTruthy();
    expect(screen.getByTestId('produits-contenu').querySelectorAll('input, select, textarea')).toHaveLength(0);
    expect(screen.getByTestId('produits-rafraichir').textContent).toBe('Rafraichir');
  });

  it('[FR-010-01] sans jeton, /produits mène à /login', async () => {
    const { routeur } = rendreApplication('/produits');

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    expect(routeur.state.historyAction).toBe('REPLACE');
  });

  it.each([
    ['le catalogue', service({ statut: 'nonAuthentifie' })],
    ['le journal', service(undefined, { statut: 'nonAuthentifie' })],
  ])('[FR-010-01] un 401 de %s ramène à /login', async (_nom, client) => {
    const { routeur } = ouvrir(client);

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    expect(localStorage.getItem('nb.jeton')).toBeNull();
  });

  it('porte le titre d’onglet du legacy (MSG-011-001)', async () => {
    await ecranCharge();

    await waitFor(() => expect(document.title).toBe('BankApp Demo - Produits bancaires'));
  });

  it('[FR-011-02] la zone de contenu est aria-busy, sans rien d’autre, jusqu’aux données', async () => {
    ouvrir(service());

    const contenu = screen.getByTestId('produits-contenu');
    expect(contenu.getAttribute('aria-busy')).toBe('true');
    expect(contenu.textContent).toBe('');
    await waitFor(() => expect(contenu.getAttribute('aria-busy')).toBeNull());
  });

  it('[FR-011-02] appelle les deux services une fois à l’ouverture, avec le jeton', async () => {
    const jeton = jetonDeTest({ sub: '1', roles: ['CLIENT'] });
    const { client } = await ecranCharge();

    expect(client.lireCatalogue).toHaveBeenCalledWith(jeton);
    expect(client.lireTraitements).toHaveBeenCalledWith(jeton);
    expect(client.lireCatalogue).toHaveBeenCalledTimes(1);
  });
});

describe('écran Produits : catalogue', () => {
  it('[FR-011-03][FR-011-04][FR-011-05] produits d’épargne : colonnes, ordre du service, valeurs et absents', async () => {
    await ecranCharge();

    expect(screen.getByRole('heading', { name: 'Produits d epargne' })).toBeTruthy();
    expect(lignes('produits-epargne')).toEqual([
      ['Nom', 'Description', 'Taux', 'Depot min', 'Capital garanti', 'Duree max'],
      ['CEL - Compte Epargne Logement', 'Description CEL - Compte Epargne Logement', '1,75 %', '300,00 EUR', 'Oui', 'Illimite'],
      ['Livret A', 'Description Livret A', '3,00 %', '10,00 EUR', 'Oui', 'Illimite'],
      ['Livret Demo Bank', 'Description Livret Demo Bank', '—', '—', 'Non', 'Illimite'],
      ['PEL - Plan Epargne Logement', 'Description PEL - Plan Epargne Logement', '2,25 %', '225,00 EUR', 'Oui', '120 mois'],
    ]);
  });

  it('[FR-011-03][FR-011-04][BUG-039] produits de crédit : colonnes, durées, LTV en pourcentage, absents', async () => {
    await ecranCharge();

    expect(lignes('produits-credit')).toEqual([
      ['Nom', 'Description', 'Taux', 'Duree max', 'LTV max', 'Assurance requise'],
      ['Credit Auto Classique', '—', '4,49 %', '72 mois', '—', 'Non'],
      ['Credit Immobilier Classique', '—', '3,65 %', '300 mois', '80,00 %', 'Oui'],
      ['Credit Personnel', '—', '5,98 %', '84 mois', '—', 'Non'],
      ['Rachat de Credits', '—', '—', '—', '70,00 %', 'Oui'],
    ]);
  });

  it('[FR-011-03][FR-011-05][FR-011-06] produits d’investissement : risque, rendement, secteur absent', async () => {
    await ecranCharge();

    expect(lignes('produits-investissement')).toEqual([
      ['Nom', 'Description', 'Secteur', 'Niveau de risque', 'Rendement attendu'],
      ['ETF Monde Diversifie', 'Description ETF Monde Diversifie', 'Actions mondiales', 'Moyen (3/5)', '7,20 %'],
      ['Fonds Euro Garanti', 'Description Fonds Euro Garanti', 'Obligations souveraines', 'Tres faible (1/5)', '2,80 %'],
      ['PEA Actions Europe', 'Description PEA Actions Europe', 'Actions Europe', 'Eleve (4/5)', '9,50 %'],
      ['Private Equity Croissance', 'Description Private Equity Croissance', '—', 'Tres eleve (5/5)', '6,50 %'],
      ['SCPI Rendement Plus', 'Description SCPI Rendement Plus', 'Immobilier tertiaire', 'N/A', '—'],
    ]);
  });

  it('[BUG-039][FR-011-05] 0.0300 s’affiche « 3,00 % » et 0.0650 « 6,50 % », jamais « 0.03 » ni « 0,03 % »', async () => {
    await ecranCharge();

    const texte = screen.getByTestId('produits-catalogue').textContent ?? '';
    expect(texte).toContain('3,00 %');
    expect(texte).toContain('6,50 %');
    expect(texte).not.toContain('0.03');
    expect(texte).not.toContain('0,03 %');
  });

  it('[FR-011-08] un type vide affiche son message dans sa seule section, les autres restent', async () => {
    await ecranCharge(service({ statut: 'ok', donnees: { ...REFERENCE, credit: [] } }));

    expect(screen.getByTestId('produits-credit-vide').textContent).toBe('Aucun produit de credit disponible');
    expect(within(screen.getByTestId('produits-credit')).queryByRole('table')).toBeNull();
    expect(screen.queryByTestId('produits-epargne-vide')).toBeNull();
    expect(screen.queryByTestId('produits-investissement-vide')).toBeNull();
    expect(within(screen.getByTestId('produits-epargne')).getByRole('table')).toBeTruthy();
  });

  it('[FR-011-08] chaque type vide a son message', async () => {
    await ecranCharge(service({ statut: 'ok', donnees: { epargne: [], credit: [], investissement: [] } }));

    expect(screen.getByTestId('produits-epargne-vide').textContent).toBe('Aucun produit d epargne disponible');
    expect(screen.getByTestId('produits-credit-vide').textContent).toBe('Aucun produit de credit disponible');
    expect(screen.getByTestId('produits-investissement-vide').textContent).toBe('Aucun produit d investissement disponible');
  });
});

describe('écran Produits : journal des traitements', () => {
  it('[FR-011-09] sections, colonnes, ordre du service, dates, durée et cellules vides', async () => {
    await ecranCharge();

    expect(screen.getByText('Traitements nocturnes')).toBeTruthy();
    expect(lignes('produits-traitements')).toEqual([
      ['Job', 'Debut', 'Fin', 'Duree (ms)', 'Enregistrements', 'Statut', 'Details'],
      ['INTERETS', '06/10/2026 02:30:05', '06/10/2026 02:30:07', '2000', '12', 'SUCCESS', 'Termine'],
      ['CLOTURE', '06/10/2026 02:00:00', '', '0', '12', 'FAILED', 'Termine'],
      ['ARCHIVAGE', '', '', '0', '', 'PARTIAL', ''],
    ]);
    expect(screen.getByTestId('produits-traitements').textContent).not.toContain('EJB');
  });

  it('[FR-011-09] un statut inconnu ou absent s’affiche sans badge coloré', async () => {
    await ecranCharge(service(undefined, { statut: 'ok', donnees: [traitement('A', null, null, 0, null), traitement('B', null, null, 0, 'AUTRE')] }));

    expect(lignes('produits-traitements').slice(1).map((ligne) => ligne[5])).toEqual(['', 'AUTRE']);
  });

  it('[FR-011-08] journal vide : message legacy', async () => {
    await ecranCharge(service(undefined, { statut: 'ok', donnees: [] }));

    expect(screen.getByTestId('produits-traitements-vide').textContent).toBe(
      'Aucun traitement nocturne enregistre — les jobs s executent a 02h00, 02h30 et le 1er du mois a 03h00',
    );
  });

  it('[FR-011-09] affiche 50 exécutions', async () => {
    const cinquante = Array.from({ length: 50 }, (_, rang) => traitement(`JOB-${rang}`, '2026-10-06T02:00:00+02:00', '2026-10-06T02:00:01+02:00', 1000));
    await ecranCharge(service(undefined, { statut: 'ok', donnees: cinquante }));

    expect(lignes('produits-traitements')).toHaveLength(51);
  });
});

describe('écran Produits : rafraîchir et pannes', () => {
  it('[FR-011-02] « Rafraichir » rappelle les deux services en parallèle et reste sur /produits', async () => {
    const utilisateur = userEvent.setup();
    const client = service();
    const { routeur } = await ecranCharge(client);
    let liberer: (reponse: ReponseProduits<Catalogue>) => void = () => {};
    client.lireCatalogue.mockImplementationOnce(() => new Promise((resolu) => (liberer = resolu)));

    await utilisateur.click(screen.getByTestId('produits-rafraichir'));

    await waitFor(() => expect(client.lireTraitements).toHaveBeenCalledTimes(2));
    expect(client.lireCatalogue).toHaveBeenCalledTimes(2);
    expect(screen.getByTestId('produits-contenu').getAttribute('aria-busy')).toBe('true');
    liberer({ statut: 'ok', donnees: { ...REFERENCE, epargne: [] } });
    await screen.findByTestId('produits-epargne-vide');
    expect(screen.getByTestId('produits-contenu').getAttribute('aria-busy')).toBeNull();
    expect(routeur.state.location.pathname).toBe('/produits');
  });

  it('[FR-011-02] un 401 au rafraîchissement ramène à /login', async () => {
    const utilisateur = userEvent.setup();
    const client = service();
    const { routeur } = await ecranCharge(client);
    client.lireCatalogue.mockResolvedValue({ statut: 'nonAuthentifie' });

    await utilisateur.click(screen.getByTestId('produits-rafraichir'));

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });

  it('[FR-011-02] la réponse d’une demande plus ancienne est ignorée', async () => {
    const utilisateur = userEvent.setup();
    const client = service();
    await ecranCharge(client);
    let liberer: (reponse: ReponseProduits<Catalogue>) => void = () => {};
    client.lireCatalogue.mockImplementationOnce(() => new Promise((resolu) => (liberer = resolu)));
    await utilisateur.click(screen.getByTestId('produits-rafraichir'));
    await waitFor(() => expect(client.lireCatalogue).toHaveBeenCalledTimes(2));
    client.lireCatalogue.mockResolvedValueOnce({ statut: 'ok', donnees: { ...REFERENCE, credit: [] } });
    await utilisateur.click(screen.getByTestId('produits-rafraichir'));
    await screen.findByTestId('produits-credit-vide');

    liberer({ statut: 'ok', donnees: { ...REFERENCE, epargne: [] } });
    await new Promise((resolu) => setTimeout(resolu, 0));

    expect(screen.queryByTestId('produits-epargne-vide')).toBeNull();
    expect(screen.getByTestId('produits-credit-vide')).toBeTruthy();
  });

  it('[FR-011-02] une réponse qui arrive après la fermeture de l’écran est ignorée', async () => {
    const client = service();
    let liberer: (reponse: ReponseProduits<Catalogue>) => void = () => {};
    client.lireCatalogue.mockImplementation(() => new Promise((resolu) => (liberer = resolu)));
    const { routeur, unmount } = ouvrir(client);
    await waitFor(() => expect(client.lireCatalogue).toHaveBeenCalled());

    unmount();
    liberer({ statut: 'nonAuthentifie' });
    await new Promise((resolu) => setTimeout(resolu, 0));

    expect(routeur.state.location.pathname).toBe('/produits');
  });

  it('[FR-011-02] catalogue en panne : le journal s’affiche, la panne technique est annoncée', async () => {
    await ecranCharge(service({ statut: 'technique' }));

    expect(screen.getByTestId('produits-erreur-technique').textContent).toBe('Une erreur technique est survenue');
    expect(screen.getByRole('alert')).toBeTruthy();
    expect(screen.queryByTestId('produits-catalogue')).toBeNull();
    expect(screen.getByTestId('produits-traitements')).toBeTruthy();
    expect(screen.getByTestId('produits-rafraichir')).toBeTruthy();
  });

  it('[FR-011-02] journal en panne : le catalogue s’affiche, la panne technique est annoncée', async () => {
    await ecranCharge(service(undefined, { statut: 'technique' }));

    expect(screen.getByTestId('produits-erreur-technique')).toBeTruthy();
    expect(screen.getByTestId('produits-catalogue')).toBeTruthy();
    expect(screen.queryByTestId('produits-traitements')).toBeNull();
  });

  it('[FR-011-02] la panne disparaît quand « Rafraichir » réussit', async () => {
    const utilisateur = userEvent.setup();
    const client = service({ statut: 'technique' });
    await ecranCharge(client);
    client.lireCatalogue.mockResolvedValue({ statut: 'ok', donnees: REFERENCE });

    await utilisateur.click(screen.getByTestId('produits-rafraichir'));

    await screen.findByTestId('produits-catalogue');
    expect(screen.queryByTestId('produits-erreur-technique')).toBeNull();
  });

  it('par défaut (aide de rendu), les deux services sont en panne technique', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ sub: '1', roles: ['CLIENT'] }));
    rendreApplication('/produits');

    await screen.findByTestId('produits-erreur-technique');
    expect(screen.queryByTestId('produits-catalogue')).toBeNull();
    expect(screen.queryByTestId('produits-traitements')).toBeNull();
  });
});
