// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { PointCotation, ValeurWatchlist } from '../../../domain/bourse';
import type { ClientBourse, ReponseAjout, ReponseBourse } from '../../../application/consulterBourse';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const valeur = (id: number, symbole: string, nom: string, marche = 'Euronext Paris', devise = 'EUR'): ValeurWatchlist => ({
  id,
  symbole,
  nom,
  marche,
  devise,
});

/** Watchlist de référence du service (9 valeurs, triées par marché puis symbole). */
const REFERENCE: ValeurWatchlist[] = [
  valeur(1, 'AI.PA', 'Air Liquide'),
  valeur(2, 'BNP.PA', 'BNP Paribas'),
  valeur(3, 'MC.PA', 'LVMH'),
  valeur(4, 'OR.PA', 'L Oreal'),
  valeur(5, 'SAN.PA', 'Sanofi'),
  valeur(6, 'TTE.PA', 'TotalEnergies'),
  valeur(7, 'AAPL', 'Apple', 'NASDAQ', 'USD'),
  valeur(8, 'GOOGL', 'Alphabet', 'NASDAQ', 'USD'),
  valeur(9, 'MSFT', 'Microsoft', 'NASDAQ', 'USD'),
];

function point(date: string, ouverture: string, cloture: string): PointCotation {
  return { date, ouverture, haut: '200.0000', bas: '100.0000', cloture, volume: 1133144 };
}

/** 12 séances : la première clôture à 172.76, la dernière à 170.16 (baisse de 1.50 %). */
const SERIE_BAISSE: PointCotation[] = [
  point('2026-09-01', '170.0000', '172.7600'),
  ...Array.from({ length: 10 }, (_, indice) => point(`2026-09-${String(indice + 2).padStart(2, '0')}`, '168.9000', '166.4400')),
  point('2026-10-02', '167.3000', '170.1600'),
];
const SERIE_HAUSSE: PointCotation[] = [point('2026-09-30', '100.0000', '100.0000'), point('2026-10-01', '100.0000', '101.5000')];

const SVG = '<svg viewBox="0 0 800 300"><text x="790" y="292">1 mois (données journalières)</text></svg>';

interface Options {
  valeurs?: ValeurWatchlist[];
  series?: Record<string, PointCotation[]>;
  graphique?: string | null;
}

/** Service bourse simulé, avec une watchlist qui évolue aux ajouts et aux retraits. */
function serviceBourse({ valeurs = REFERENCE, series = {}, graphique = SVG }: Options = {}) {
  let liste = [...valeurs];
  const par = {
    lireWatchlist: vi.fn<ClientBourse['lireWatchlist']>(() => Promise.resolve({ statut: 'ok', donnees: [...liste] })),
    lireHistorique: vi.fn<ClientBourse['lireHistorique']>((_jeton, symbole, periode) =>
      Promise.resolve({
        statut: 'ok',
        donnees: { symbole, periode, points: series[symbole] ?? [], graphique: (series[symbole]?.length ?? 0) > 0 ? graphique : null },
      }),
    ),
    ajouter: vi.fn<ClientBourse['ajouter']>((_jeton, demande) => {
      const symbole = demande.symbole.trim().toUpperCase();
      if (symbole === '') {
        return Promise.resolve<ReponseAjout>({ statut: 'refusee', message: 'Le symbole est obligatoire (ex: AMZN, SG.PA)' });
      }
      if (demande.nom.trim() === '') {
        return Promise.resolve<ReponseAjout>({ statut: 'refusee', message: 'Le nom de la valeur est obligatoire' });
      }
      if (liste.some((existante) => existante.symbole === symbole)) {
        return Promise.resolve<ReponseAjout>({ statut: 'refusee', message: `Le symbole « ${symbole} » existe déjà dans votre watchlist` });
      }
      const ajoutee = valeur(100, symbole, demande.nom, demande.marche, demande.devise);
      liste = [...liste, ajoutee];
      return Promise.resolve<ReponseAjout>({ statut: 'ok', donnees: ajoutee });
    }),
    retirer: vi.fn<ClientBourse['retirer']>((_jeton, id) => {
      liste = liste.filter((existante) => existante.id !== id);
      return Promise.resolve<ReponseBourse<null>>({ statut: 'ok', donnees: null });
    }),
  };
  return par;
}

const SERIES = { 'AI.PA': SERIE_BAISSE, MSFT: SERIE_HAUSSE, 'SG.PA': SERIE_HAUSSE };

function ouvrir(service = serviceBourse({ series: SERIES }), roles: string[] = ['CLIENT']) {
  localStorage.setItem('nb.jeton', jetonDeTest({ sub: '1', roles }));
  const rendu = rendreApplication('/bourse', undefined, undefined, undefined, undefined, service);
  return { service, ...rendu };
}

async function ecranCharge(service = serviceBourse({ series: SERIES }), roles: string[] = ['CLIENT']) {
  const ouverture = ouvrir(service, roles);
  await screen.findByTestId('bourse-valeur');
  await waitFor(() => expect(screen.getByTestId('bourse-contenu').getAttribute('aria-busy')).toBeNull());
  return ouverture;
}

const liste = (testId: string) => screen.getByTestId(testId) as HTMLSelectElement;
const champ = (testId: string) => screen.getByTestId(testId) as HTMLInputElement;
const contenu = () => screen.getByTestId('bourse-contenu').textContent ?? '';

describe('écran Bourse : accès', () => {
  it.each(['CLIENT', 'CONSEILLER', 'ADMIN'])('[FR-009-01] %s ouvre /bourse et voit le même écran', async (role) => {
    const { routeur } = await ecranCharge(undefined, [role]);

    expect(routeur.state.location.pathname).toBe('/bourse');
    expect(screen.getByText('⭐ Ma Watchlist (9 valeurs)')).toBeTruthy();
    expect(liste('bourse-valeur').value).toBe('AI.PA');
    expect(screen.getByTestId('bourse-ajouter')).toBeTruthy();
  });

  it('[FR-009-01] sans jeton, /bourse mène à /login', async () => {
    const { routeur } = rendreApplication('/bourse');

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    expect(routeur.state.historyAction).toBe('REPLACE');
  });

  it('[FR-009-01] un 401 du service bourse ramène à /login', async () => {
    const service = serviceBourse();
    service.lireWatchlist.mockResolvedValue({ statut: 'nonAuthentifie' });
    const { routeur } = ouvrir(service);

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });

  it('porte le titre d’onglet du legacy', async () => {
    await ecranCharge();

    await waitFor(() => expect(document.title).toBe('BankApp Demo - Bourse'));
  });
});

describe('écran Bourse : ouverture', () => {
  it('[FR-009-02][FR-029-06] reste en aria-busy sans squelette jusqu’aux données, puis présélectionne 1 Mois et la première valeur', async () => {
    const service = serviceBourse({ series: SERIES });
    let rendre: (reponse: ReponseBourse<never>) => void = () => {};
    service.lireHistorique.mockImplementationOnce(() => new Promise((resolve) => (rendre = resolve)));
    ouvrir(service);

    expect(screen.getByTestId('bourse-contenu').getAttribute('aria-busy')).toBe('true');
    expect(screen.queryByTestId('bourse-valeur')).toBeNull();
    await waitFor(() => expect(service.lireHistorique).toHaveBeenCalledOnce());
    expect(service.lireHistorique).toHaveBeenCalledWith(expect.any(String), 'AI.PA', '1M');
    expect(screen.queryByTestId('bourse-valeur')).toBeNull();

    rendre({ statut: 'technique' });
    await screen.findByTestId('bourse-valeur');
    await waitFor(() => expect(screen.getByTestId('bourse-contenu').getAttribute('aria-busy')).toBeNull());
    expect(liste('bourse-periode').value).toBe('1M');
    expect(liste('bourse-valeur').value).toBe('AI.PA');
  });

  it('[FR-009-02][FR-029-06] présente les textes du legacy de la barre d’outils', async () => {
    await ecranCharge();

    const texte = contenu();
    for (const attendu of ['Valeur', '(9 valeurs)', 'Période', '+ Ajouter une valeur', 'BNP.PA', 'AI.PA']) {
      expect(texte).toContain(attendu);
    }
    expect(screen.getByLabelText('Valeur')).toBe(liste('bourse-valeur'));
    expect(screen.getByLabelText('Période')).toBe(liste('bourse-periode'));
    expect([...liste('bourse-valeur').options].map((option) => option.textContent)).toEqual(
      REFERENCE.map((v) => `${v.symbole} — ${v.nom}`),
    );
    expect([...liste('bourse-periode').options].map((option) => [option.value, option.textContent])).toEqual([
      ['1W', '1 Semaine'],
      ['1M', '1 Mois'],
      ['1Y', '1 An'],
      ['5Y', '5 Ans'],
    ]);
  });

  it('[FR-009-10][FR-029-11] liste la watchlist dans l’ordre du service, avec ses colonnes', async () => {
    await ecranCharge();

    const tableau = screen.getByRole('table', { name: '⭐ Ma Watchlist (9 valeurs)' });
    expect(within(tableau).getAllByRole('columnheader').map((entete) => entete.textContent)).toEqual([
      'Symbole',
      'Nom',
      'Marché',
      'Devise',
      'Suppr.',
    ]);
    const lignes = within(tableau).getAllByRole('row').slice(1);
    expect(lignes.map((ligne) => within(ligne).getAllByRole('cell').slice(0, 4).map((cellule) => cellule.textContent))).toEqual(
      REFERENCE.map((v) => [v.symbole, v.nom, v.marche, v.devise]),
    );
    expect(screen.getByTestId('bourse-retirer-1').getAttribute('title')).toBe('Supprimer Air Liquide de la watchlist');
    expect(screen.getByTestId('bourse-retirer-1').textContent).toBe('✕');
  });
});

describe('écran Bourse : indicateurs, graphique et séances', () => {
  it('[FR-009-04][FR-009-05][FR-029-07] affiche nom, cours, variation, performance et marché, en rouge à la baisse', async () => {
    await ecranCharge();

    const texte = contenu();
    for (const attendu of ['Air Liquide — AI.PA', '170.16 EUR', 'Variation sur la période', '-2.60 EUR', 'Performance %', '▼ -1.50%', 'Marché', 'Euronext Paris']) {
      expect(texte).toContain(attendu);
    }
    expect(screen.getByText('-2.60 EUR').className).toContain('ecran-bourse--baisse');
    expect(screen.getByText('▼ -1.50%').className).toContain('ecran-bourse--baisse');
  });

  it('[FR-009-05][FR-029-07] une performance positive s’affiche en vert, préfixée « ▲ + »', async () => {
    const { service } = await ecranCharge();
    const utilisateur = userEvent.setup();

    await utilisateur.selectOptions(liste('bourse-valeur'), 'MSFT');
    await screen.findByText('▲ +1.50%');

    expect(screen.getByText('▲ +1.50%').className).toContain('ecran-bourse--hausse');
    expect(screen.getByText('+1.50 USD').className).toContain('ecran-bourse--hausse');
    expect(screen.getByText('101.50 USD')).toBeTruthy();
    expect(service.lireHistorique).toHaveBeenLastCalledWith(expect.any(String), 'MSFT', '1M');
  });

  it('[FR-009-06][FR-029-08] affiche le SVG du service dans une carte image nommée, sous son titre', async () => {
    await ecranCharge();

    expect(screen.getByRole('heading', { name: '📈 Évolution du cours' })).toBeTruthy();
    const graphique = screen.getByRole('img', { name: 'Évolution du cours de AI.PA sur 1 mois (données journalières)' });
    expect(graphique.className).toBe('carte');
    expect(graphique.querySelector('svg')).not.toBeNull();
    expect(graphique.textContent).toContain('1 mois (données journalières)');
  });

  it('[FR-009-06] sans graphique du service, la zone est absente mais le tableau reste', async () => {
    await ecranCharge(serviceBourse({ series: SERIES, graphique: null }));

    expect(screen.queryByRole('img')).toBeNull();
    expect(screen.queryByText('📈 Évolution du cours')).toBeNull();
    expect(screen.getByText('📋 Dernières séances')).toBeTruthy();
  });

  it('[FR-009-07][FR-029-08] affiche les 10 dernières séances, du plus récent au plus ancien, avec Var. J et couleurs', async () => {
    await ecranCharge();

    const tableau = screen.getByRole('table', { name: '📋 Dernières séances' });
    expect(within(tableau).getAllByRole('columnheader').map((entete) => entete.textContent)).toEqual([
      'Date',
      'Ouverture',
      'Haut',
      'Bas',
      'Clôture',
      'Volume',
      'Var. J',
    ]);
    const lignes = within(tableau).getAllByRole('row').slice(1);
    expect(lignes).toHaveLength(10);
    expect(within(lignes[0] as HTMLElement).getAllByRole('cell').map((cellule) => cellule.textContent)).toEqual([
      '2026-10-02',
      '167.30',
      '200.00',
      '100.00',
      '170.16',
      '1133144',
      '+ 1.71%',
    ]);
    expect(lignes[0]?.className).toContain('ecran-bourse--hausse');
    expect(lignes[1]?.textContent).toContain('-1.46%');
    expect(lignes[1]?.className).toContain('ecran-bourse--baisse');
    expect(lignes[9]?.textContent).toContain('2026-09-03');
  });

  it('[FR-009-09][FR-029-03] sans point : message du legacy, graphique et tableau masqués, écran utilisable', async () => {
    const utilisateur = userEvent.setup();
    await ecranCharge();

    await utilisateur.selectOptions(liste('bourse-valeur'), 'BNP.PA');
    const message = await screen.findByTestId('bourse-aucune-donnee');

    expect(message.textContent).toBe(
      '⚠️ Aucune donnée reçue pour « BNP.PA » sur 1 mois (données journalières). Causes possibles : symbole invalide pour cette place boursière (vérifiez le suffixe, ex: .PA pour Euronext), ou limite de 25 requêtes/jour atteinte (plan gratuit Alpha Vantage). Réessayez dans quelques minutes ou changez de valeur.',
    );
    expect(screen.queryByRole('img')).toBeNull();
    expect(screen.queryByText('📋 Dernières séances')).toBeNull();
    expect(screen.queryByText('Performance %')).toBeNull();
    expect(screen.getByTestId('bourse-ajouter')).toBeTruthy();

    await utilisateur.selectOptions(liste('bourse-periode'), '1Y');
    await waitFor(() => expect(screen.getByTestId('bourse-aucune-donnee').textContent).toContain('sur 1 an (données hebdomadaires).'));
  });
});

describe('écran Bourse : valeur et période', () => {
  it('[FR-009-03] changer de période recharge seulement l’historique, sans bouton ni nouvelle lecture de la watchlist', async () => {
    const utilisateur = userEvent.setup();
    const { service } = await ecranCharge();

    await utilisateur.selectOptions(liste('bourse-periode'), '5Y');
    await waitFor(() => expect(service.lireHistorique).toHaveBeenLastCalledWith(expect.any(String), 'AI.PA', '5Y'));
    await waitFor(() => expect(screen.getByTestId('bourse-contenu').getAttribute('aria-busy')).toBeNull());

    expect(service.lireWatchlist).toHaveBeenCalledOnce();
    expect(liste('bourse-periode').value).toBe('5Y');
    expect(screen.getByRole('img', { name: 'Évolution du cours de AI.PA sur 5 ans (données hebdomadaires)' })).toBeTruthy();
    expect(screen.getAllByRole('button').map((bouton) => bouton.textContent)).not.toContain('Valider');
  });

  it('[FR-009-03] masque l’historique précédent et signale aria-busy pendant le rechargement', async () => {
    const utilisateur = userEvent.setup();
    const { service } = await ecranCharge();
    let rendre: (reponse: ReponseBourse<never>) => void = () => {};
    service.lireHistorique.mockImplementationOnce(() => new Promise((resolve) => (rendre = resolve)));

    await utilisateur.selectOptions(liste('bourse-valeur'), 'MSFT');

    expect(screen.getByTestId('bourse-contenu').getAttribute('aria-busy')).toBe('true');
    expect(screen.queryByText('Air Liquide — AI.PA')).toBeNull();
    rendre({ statut: 'technique' });
    await waitFor(() => expect(screen.getByTestId('bourse-contenu').getAttribute('aria-busy')).toBeNull());
  });

  it('[FR-009-03] une réponse plus ancienne que la dernière demande est ignorée', async () => {
    const utilisateur = userEvent.setup();
    const { service } = await ecranCharge();
    const reponses: Array<() => void> = [];
    service.lireHistorique.mockImplementation(
      (_jeton, symbole, periode) =>
        new Promise((resolve) => {
          // Première demande (MSFT) : hausse ; dernière demande (GOOGL) : baisse.
          const points = symbole === 'MSFT' ? SERIE_HAUSSE : SERIE_BAISSE;
          reponses.push(() => resolve({ statut: 'ok', donnees: { symbole, periode, points, graphique: null } }));
        }),
    );

    await utilisateur.selectOptions(liste('bourse-valeur'), 'MSFT');
    await utilisateur.selectOptions(liste('bourse-valeur'), 'GOOGL');
    await waitFor(() => expect(reponses.length).toBe(2));
    reponses[1]?.();
    await screen.findByText('▼ -1.50%');
    reponses[0]?.();

    await waitFor(() => expect(screen.getByTestId('bourse-contenu').getAttribute('aria-busy')).toBeNull());
    expect(screen.getByText('▼ -1.50%')).toBeTruthy();
    expect(screen.queryByText('▲ +1.50%')).toBeNull();
  });

  it('[FR-009-03] sans valeur sélectionnée, changer la période ne charge rien', async () => {
    const utilisateur = userEvent.setup();
    const { service } = await ecranCharge(serviceBourse({ valeurs: [] }));

    await utilisateur.selectOptions(liste('bourse-periode'), '1W');

    expect(service.lireHistorique).not.toHaveBeenCalled();
    expect(liste('bourse-periode').value).toBe('1W');
    expect(screen.getByText('⭐ Ma Watchlist (0 valeurs)')).toBeTruthy();
  });

  it('[FR-009-03] un 401 de l’historique ramène à /login', async () => {
    const utilisateur = userEvent.setup();
    const { service, routeur } = await ecranCharge();
    service.lireHistorique.mockResolvedValue({ statut: 'nonAuthentifie' });

    await utilisateur.selectOptions(liste('bourse-periode'), '1W');

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });
});

describe('écran Bourse : formulaire d’ajout', () => {
  async function ouvrirFormulaire() {
    const utilisateur = userEvent.setup();
    const ouverture = await ecranCharge();
    await utilisateur.click(screen.getByTestId('bourse-ajouter'));
    return { utilisateur, ...ouverture };
  }

  it('[FR-009-11][FR-029-14] ouvre dans la page le formulaire du legacy, marché et devise par défaut', async () => {
    const { container } = await ouvrirFormulaire();

    expect(screen.getByText('➕ Ajouter une valeur à la watchlist')).toBeTruthy();
    expect(container.querySelector('label[for="bourse-symbole"]')?.textContent).toBe('Symbole');
    expect(container.querySelector('label[for="bourse-nom"]')?.textContent).toBe('Nom');
    expect(champ('bourse-symbole').placeholder).toBe('ex: AMZN');
    expect(champ('bourse-nom').placeholder).toBe('ex: Amazon');
    expect(screen.getByText('Symbole Alpha Vantage').id).toBe('bourse-symbole-aide');
    expect(champ('bourse-symbole').getAttribute('aria-describedby')).toBe('bourse-symbole-aide');
    expect(document.activeElement).toBe(champ('bourse-symbole'));
    expect(liste('bourse-marche').value).toBe('Euronext Paris');
    expect([...liste('bourse-marche').options].map((option) => [option.value, option.textContent])).toEqual([
      ['Euronext Paris', 'Euronext Paris'],
      ['NASDAQ', 'NASDAQ'],
      ['NYSE', 'NYSE'],
      ['LSE', 'London SE'],
      ['Autre', 'Autre'],
    ]);
    expect(liste('bourse-devise').value).toBe('EUR');
    expect([...liste('bourse-devise').options].map((option) => option.value)).toEqual(['EUR', 'USD', 'GBP']);
    expect(screen.getByTestId('bourse-formulaire-ajouter').textContent).toBe('Ajouter');
    expect(screen.getByTestId('bourse-formulaire-annuler').textContent).toBe('Annuler');
  });

  it('[FR-009-11] Annuler referme le formulaire, rend le focus au bouton et efface la saisie', async () => {
    const { utilisateur } = await ouvrirFormulaire();
    await utilisateur.type(champ('bourse-symbole'), 'abc');

    await utilisateur.click(screen.getByTestId('bourse-formulaire-annuler'));

    expect(screen.queryByTestId('bourse-symbole')).toBeNull();
    expect(document.activeElement).toBe(screen.getByTestId('bourse-ajouter'));
    await utilisateur.click(screen.getByTestId('bourse-ajouter'));
    expect(champ('bourse-symbole').value).toBe('');
  });

  it('[FR-029-13] bloque le symbole à 20 caractères et le nom à 100, sans message', async () => {
    const { utilisateur } = await ouvrirFormulaire();

    await utilisateur.type(champ('bourse-symbole'), 'A'.repeat(25));
    await utilisateur.click(champ('bourse-nom'));
    await utilisateur.paste('N'.repeat(120));

    expect(champ('bourse-symbole').maxLength).toBe(20);
    expect(champ('bourse-nom').maxLength).toBe(100);
    expect(champ('bourse-symbole').value).toHaveLength(20);
    expect(champ('bourse-nom').value).toHaveLength(100);
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it.each([
    ['symbole vide', '', 'Société', 'Le symbole est obligatoire (ex: AMZN, SG.PA)'],
    ['nom vide', 'sg.pa', '', 'Le nom de la valeur est obligatoire'],
    ['symbole existant', 'bnp.pa', 'BNP', 'Le symbole « BNP.PA » existe déjà dans votre watchlist'],
  ])('[FR-009-11][FR-029-09] %s : message du service sous le formulaire, rien n’est ajouté', async (_cas, symbole, nom, message) => {
    const { utilisateur, service } = await ouvrirFormulaire();
    if (symbole !== '') {
      await utilisateur.type(champ('bourse-symbole'), symbole);
    }
    if (nom !== '') {
      await utilisateur.type(champ('bourse-nom'), nom);
    }

    await utilisateur.click(screen.getByTestId('bourse-formulaire-ajouter'));

    const alerte = await screen.findByTestId('bourse-ajout-erreur');
    expect(alerte.textContent).toBe(message);
    expect(alerte.getAttribute('role')).toBe('alert');
    expect(document.activeElement).toBe(alerte);
    expect(screen.getByTestId('bourse-symbole')).toBeTruthy();
    expect(screen.getByText('⭐ Ma Watchlist (9 valeurs)')).toBeTruthy();
    expect(service.lireWatchlist).toHaveBeenCalledOnce();
    expect(screen.queryByTestId('bourse-erreur-technique')).toBeNull();
  });

  it('[FR-009-12][FR-029-10] ajoute « sg.pa » : confirmation, formulaire fermé, valeur sélectionnée et historique chargé', async () => {
    const { utilisateur, service } = await ouvrirFormulaire();
    await utilisateur.type(champ('bourse-symbole'), 'sg.pa');
    await utilisateur.type(champ('bourse-nom'), 'Société Générale');

    await utilisateur.click(screen.getByTestId('bourse-formulaire-ajouter'));

    await waitFor(() => expect(screen.getByTestId('bourse-message-succes').textContent).toBe('Valeur ajoutée !'));
    expect(service.ajouter).toHaveBeenCalledWith(expect.any(String), {
      symbole: 'sg.pa',
      nom: 'Société Générale',
      marche: 'Euronext Paris',
      devise: 'EUR',
    });
    expect(screen.queryByTestId('bourse-symbole')).toBeNull();
    expect(document.activeElement).toBe(screen.getByTestId('bourse-ajouter'));
    expect(liste('bourse-valeur').value).toBe('SG.PA');
    expect(service.lireHistorique).toHaveBeenLastCalledWith(expect.any(String), 'SG.PA', '1M');
    expect(screen.getByText('⭐ Ma Watchlist (10 valeurs)')).toBeTruthy();
    expect(contenu()).toContain('Société Générale — SG.PA');
  });

  it('[FR-009-12] la confirmation disparaît au changement de valeur', async () => {
    const { utilisateur } = await ouvrirFormulaire();
    await utilisateur.type(champ('bourse-symbole'), 'sg.pa');
    await utilisateur.type(champ('bourse-nom'), 'Société Générale');
    await utilisateur.click(screen.getByTestId('bourse-formulaire-ajouter'));
    await waitFor(() => expect(screen.getByTestId('bourse-message-succes').textContent).toBe('Valeur ajoutée !'));

    await utilisateur.selectOptions(liste('bourse-valeur'), 'MSFT');

    expect(screen.getByTestId('bourse-message-succes').textContent).toBe('');
  });

  it('[FR-009-12] envoie le marché et la devise choisis', async () => {
    const { utilisateur, service } = await ouvrirFormulaire();
    await utilisateur.type(champ('bourse-symbole'), 'vod.l');
    await utilisateur.type(champ('bourse-nom'), 'Vodafone');
    await utilisateur.selectOptions(liste('bourse-marche'), 'LSE');
    await utilisateur.selectOptions(liste('bourse-devise'), 'GBP');

    await utilisateur.click(screen.getByTestId('bourse-formulaire-ajouter'));

    await waitFor(() =>
      expect(service.ajouter).toHaveBeenCalledWith(expect.any(String), { symbole: 'vod.l', nom: 'Vodafone', marche: 'LSE', devise: 'GBP' }),
    );
  });

  it('une panne technique à l’ajout garde le formulaire ouvert et annonce le texte technique', async () => {
    const { utilisateur, service } = await ouvrirFormulaire();
    service.ajouter.mockResolvedValue({ statut: 'technique' });
    await utilisateur.type(champ('bourse-symbole'), 'x');

    await utilisateur.click(screen.getByTestId('bourse-formulaire-ajouter'));

    expect((await screen.findByTestId('bourse-erreur-technique')).textContent).toBe('Une erreur technique est survenue');
    expect(screen.getByTestId('bourse-symbole')).toBeTruthy();
    expect(screen.queryByTestId('bourse-ajout-erreur')).toBeNull();
  });

  it('un 401 à l’ajout ramène à /login', async () => {
    const { utilisateur, service, routeur } = await ouvrirFormulaire();
    service.ajouter.mockResolvedValue({ statut: 'nonAuthentifie' });

    await utilisateur.click(screen.getByTestId('bourse-formulaire-ajouter'));

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });

  it('ignore une seconde soumission pendant l’envoi', async () => {
    const { utilisateur, service } = await ouvrirFormulaire();
    let rendre: (reponse: ReponseAjout) => void = () => {};
    service.ajouter.mockImplementation(() => new Promise((resolve) => (rendre = resolve)));

    const bouton = screen.getByTestId('bourse-formulaire-ajouter') as HTMLButtonElement;
    await utilisateur.click(bouton);
    expect(bouton.disabled).toBe(true);
    bouton.form?.requestSubmit();
    rendre({ statut: 'technique' });

    await waitFor(() => expect(bouton.disabled).toBe(false));
    expect(service.ajouter).toHaveBeenCalledOnce();
  });
});

describe('écran Bourse : retrait', () => {
  it('[FR-009-13][FR-029-12] retire la valeur sélectionnée sans confirmation et sélectionne la première restante', async () => {
    const utilisateur = userEvent.setup();
    const { service } = await ecranCharge();

    await utilisateur.click(screen.getByTestId('bourse-retirer-1'));

    await waitFor(() => expect(liste('bourse-valeur').value).toBe('BNP.PA'));
    expect(service.retirer).toHaveBeenCalledWith(expect.any(String), 1);
    expect(screen.getByText('⭐ Ma Watchlist (8 valeurs)')).toBeTruthy();
    expect(screen.getByText('(8 valeurs)')).toBeTruthy();
    expect(screen.queryByTestId('bourse-retirer-1')).toBeNull();
    expect(service.lireHistorique).toHaveBeenLastCalledWith(expect.any(String), 'BNP.PA', '1M');
  });

  it('[FR-009-13] retirer une autre valeur conserve la sélection', async () => {
    const utilisateur = userEvent.setup();
    await ecranCharge();

    await utilisateur.click(screen.getByTestId('bourse-retirer-9'));

    await waitFor(() => expect(screen.getByText('⭐ Ma Watchlist (8 valeurs)')).toBeTruthy());
    expect(liste('bourse-valeur').value).toBe('AI.PA');
  });

  it('[FR-009-13][FR-029-12] la dernière valeur retirée efface la sélection, le graphique et le tableau', async () => {
    const utilisateur = userEvent.setup();
    await ecranCharge(serviceBourse({ valeurs: [REFERENCE[0] as ValeurWatchlist], series: SERIES }));

    await utilisateur.click(screen.getByTestId('bourse-retirer-1'));

    await waitFor(() => expect(screen.getByText('⭐ Ma Watchlist (0 valeurs)')).toBeTruthy());
    expect(liste('bourse-valeur').value).toBe('');
    expect(screen.queryByRole('img')).toBeNull();
    expect(screen.queryByText('📋 Dernières séances')).toBeNull();
    expect(screen.queryByTestId('bourse-aucune-donnee')).toBeNull();
  });

  it('une panne technique au retrait laisse l’écran en l’état et l’annonce', async () => {
    const utilisateur = userEvent.setup();
    const { service } = await ecranCharge();
    service.retirer.mockResolvedValue({ statut: 'technique' });

    await utilisateur.click(screen.getByTestId('bourse-retirer-1'));

    expect((await screen.findByTestId('bourse-erreur-technique')).textContent).toBe('Une erreur technique est survenue');
    expect(screen.getByText('⭐ Ma Watchlist (9 valeurs)')).toBeTruthy();
  });

  it('un 401 au retrait ramène à /login', async () => {
    const utilisateur = userEvent.setup();
    const { service, routeur } = await ecranCharge();
    service.retirer.mockResolvedValue({ statut: 'nonAuthentifie' });

    await utilisateur.click(screen.getByTestId('bourse-retirer-1'));

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });
});

describe('écran Bourse : pannes', () => {
  it('[FR-009-09] watchlist illisible : l’écran reste utilisable et annonce le texte technique', async () => {
    const service = serviceBourse();
    service.lireWatchlist.mockResolvedValue({ statut: 'technique' });
    ouvrir(service);

    expect((await screen.findByTestId('bourse-erreur-technique')).textContent).toBe('Une erreur technique est survenue');
    expect(screen.getByTestId('bourse-ajouter')).toBeTruthy();
    expect(screen.getByText('⭐ Ma Watchlist (0 valeurs)')).toBeTruthy();
    expect(screen.getByTestId('bourse-contenu').getAttribute('aria-busy')).toBeNull();
  });

  it('[FR-009-09] historique en panne : la watchlist reste, le texte technique est annoncé, rien d’autre', async () => {
    const service = serviceBourse({ series: SERIES });
    service.lireHistorique.mockResolvedValue({ statut: 'technique' });
    ouvrir(service);

    expect(await screen.findByTestId('bourse-erreur-technique')).toBeTruthy();
    expect(liste('bourse-valeur').value).toBe('AI.PA');
    expect(screen.queryByTestId('bourse-aucune-donnee')).toBeNull();
    expect(screen.queryByRole('img')).toBeNull();
  });

  it('la panne disparaît quand un changement de période réussit', async () => {
    const utilisateur = userEvent.setup();
    const service = serviceBourse({ series: SERIES });
    service.lireHistorique.mockResolvedValueOnce({ statut: 'technique' });
    ouvrir(service);
    await screen.findByTestId('bourse-erreur-technique');

    await utilisateur.selectOptions(liste('bourse-periode'), '1W');

    await waitFor(() => expect(screen.queryByTestId('bourse-erreur-technique')).toBeNull());
    expect(contenu()).toContain('Air Liquide — AI.PA');
  });

  it('un changement de période en panne annonce le texte technique', async () => {
    const utilisateur = userEvent.setup();
    const { service } = await ecranCharge();
    service.lireHistorique.mockResolvedValue({ statut: 'technique' });

    await utilisateur.selectOptions(liste('bourse-periode'), '1W');

    expect(await screen.findByTestId('bourse-erreur-technique')).toBeTruthy();
    expect(screen.queryByText('Air Liquide — AI.PA')).toBeNull();
  });

  it('quitter l’écran avant la réponse n’applique rien', async () => {
    const service = serviceBourse();
    let rendre: (reponse: ReponseBourse<readonly ValeurWatchlist[]>) => void = () => {};
    service.lireWatchlist.mockImplementation(() => new Promise((resolve) => (rendre = resolve)));
    const { unmount } = ouvrir(service);

    unmount();
    rendre({ statut: 'ok', donnees: REFERENCE });

    await waitFor(() => expect(service.lireHistorique).toHaveBeenCalledTimes(1));
    expect(screen.queryByTestId('bourse-valeur')).toBeNull();
  });
});
