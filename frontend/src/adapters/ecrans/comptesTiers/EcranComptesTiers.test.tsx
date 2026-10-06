// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { ClientComptesTiers, ReponseTiers } from '../../../application/comptesTiers';
import type { CompteExterne, EtatComptesTiers, TransactionExterne } from '../../../domain/comptesTiers';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
  sessionStorage.clear();
});

const INDISPONIBLE = 'Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants.';

const compte = (id: number, nom: string, type: string | null, solde: string | null, iban: string | null = null): CompteExterne => ({
  id,
  nom,
  type,
  solde,
  soldeAbsent: solde === null,
  iban,
});

const transaction = (libelle: string, montant: string, date = '2026-10-01'): TransactionExterne => ({ date, libelle, montant });

const NON_CONNECTE: EtatComptesTiers = { connecte: false, comptes: [], compteSelectionne: null, transactions: [], message: '', patrimoineTotal: '0.00' };

const COURANT = compte(11, 'Compte Courant', 'checking', '1234.50', 'FR7612345678901234567890123');
const LIVRET = compte(12, 'Livret A', 'savings', '120.50');

const connecte = (surcharge: Partial<EtatComptesTiers> = {}): EtatComptesTiers => ({
  connecte: true,
  comptes: [COURANT, LIVRET],
  compteSelectionne: 11,
  transactions: [transaction('Salaire', '2000.00', '2026-09-30'), transaction('Loyer', '-750.00')],
  message: '',
  patrimoineTotal: '1355.00',
  ...surcharge,
});

const ok = (donnees: EtatComptesTiers): ReponseTiers<EtatComptesTiers> => ({ statut: 'ok', donnees });

function service(etat: EtatComptesTiers, surcharge: Partial<ClientComptesTiers> = {}) {
  return {
    lireEtat: vi.fn<ClientComptesTiers['lireEtat']>(surcharge.lireEtat ?? (() => Promise.resolve(ok(etat)))),
    connecter: vi.fn<ClientComptesTiers['connecter']>(
      surcharge.connecter ?? (() => Promise.resolve({ statut: 'ok', donnees: 'https://webview.powens.test/connect' })),
    ),
    selectionner: vi.fn<ClientComptesTiers['selectionner']>(surcharge.selectionner ?? (() => Promise.resolve(ok(etat)))),
    rafraichir: vi.fn<ClientComptesTiers['rafraichir']>(surcharge.rafraichir ?? (() => Promise.resolve(ok(etat)))),
    deconnecter: vi.fn<ClientComptesTiers['deconnecter']>(surcharge.deconnecter ?? (() => Promise.resolve({ statut: 'ok', donnees: null }))),
  };
}

async function ouvrir(client = service(NON_CONNECTE), roles: string[] = ['CLIENT']) {
  localStorage.setItem('nb.jeton', jetonDeTest({ sub: '1', roles }));
  const rendu = rendreApplication('/comptes-tiers', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, client);
  await waitFor(() => expect(screen.getByTestId('tiers-contenu').getAttribute('aria-busy')).toBeNull());
  return { client, ...rendu };
}

const texte = () => screen.getByTestId('tiers-contenu').textContent ?? '';

describe('écran Comptes Tiers : accès', () => {
  it.each(['CLIENT', 'CONSEILLER', 'ADMIN'])('[FR-012-01] %s ouvre /comptes-tiers et voit le même écran', async (role) => {
    const { routeur } = await ouvrir(undefined, [role]);

    expect(routeur.state.location.pathname).toBe('/comptes-tiers');
    expect(screen.getByRole('heading', { name: 'Agregez vos comptes bancaires' })).toBeTruthy();
    expect(screen.getByTestId('tiers-connecter-banque')).toBeTruthy();
  });

  it('[FR-012-01] sans jeton, /comptes-tiers mène à /login', async () => {
    const { routeur } = rendreApplication('/comptes-tiers');

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    expect(routeur.state.historyAction).toBe('REPLACE');
  });

  it('[FR-012-01] un 401 du service mène à /login', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ sub: '1', roles: ['CLIENT'] }));
    const { routeur } = rendreApplication('/comptes-tiers', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
      lireEtat: () => Promise.resolve({ statut: 'nonAuthentifie' }),
    });

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });

  it('porte le titre d’onglet du legacy', async () => {
    await ouvrir();

    await waitFor(() => expect(document.title).toBe('BankApp Demo - Comptes Tiers'));
  });

  it('[FR-012-01] une panne technique à l’ouverture est annoncée, avec le bloc de connexion', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ sub: '1', roles: ['CLIENT'] }));
    rendreApplication('/comptes-tiers');

    expect((await screen.findByTestId('tiers-message-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(screen.getByTestId('tiers-connecter-banque')).toBeTruthy();
  });
});

describe('écran Comptes Tiers : non connecté', () => {
  it('[FR-012-03] affiche le bloc de connexion, sans bandeau technique', async () => {
    await ouvrir();

    const contenu = texte();
    for (const attendu of [
      'Agregez vos comptes bancaires',
      'Connectez vos comptes d autres banques pour avoir une vue complete de votre patrimoine financier. Connexion securisee via le protocole OAuth2 PSD2.',
      'BNP Paribas',
      'Credit Agricole',
      'La Banque Postale',
      'CIC',
      'LCL',
      '+ 1800 banques',
      'Connecter une banque',
    ]) {
      expect(contenu).toContain(attendu);
    }
    expect(contenu).not.toContain('WildFly');
    expect(contenu).not.toContain('Open Banking —');
    expect(screen.queryByTestId('tiers-message-erreur')).toBeNull();
    expect(screen.queryByTestId('tiers-deconnecter')).toBeNull();
  });

  it('[FR-012-05] « Connecter une banque » envoie le navigateur à l’adresse renvoyée', async () => {
    const { client, allerVers } = await ouvrir();

    await userEvent.click(screen.getByTestId('tiers-connecter-banque'));

    await waitFor(() => expect(allerVers).toHaveBeenCalledWith('https://webview.powens.test/connect'));
    expect(client.connecter).toHaveBeenCalledWith(expect.any(String));
    expect(screen.queryByTestId('tiers-message-erreur')).toBeNull();
  });

  it.each([
    ['503', { statut: 'indisponible' } as const],
    ['panne technique', { statut: 'technique' } as const],
  ])('[FR-012-05] connexion en échec (%s) : message, bloc de connexion, pas de changement d’adresse', async (_cas, reponse) => {
    const { allerVers } = await ouvrir(service(NON_CONNECTE, { connecter: () => Promise.resolve(reponse) }));

    await userEvent.click(screen.getByTestId('tiers-connecter-banque'));

    expect((await screen.findByTestId('tiers-message-erreur')).textContent).toBe('Impossible d initialiser la connexion Powens.');
    expect(allerVers).not.toHaveBeenCalled();
    expect(screen.getByTestId('tiers-connecter-banque')).toBeTruthy();
  });

  it('[FR-012-05] sans jeton au moment du clic, la session se termine', async () => {
    const { routeur } = await ouvrir();
    localStorage.removeItem('nb.jeton');

    await userEvent.click(screen.getByTestId('tiers-connecter-banque'));

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });

  it('[FR-012-05] une connexion en cours ignore un second clic', async () => {
    let terminer: (reponse: ReponseTiers<string>) => void = () => undefined;
    const client = service(NON_CONNECTE, { connecter: () => new Promise((resolve) => (terminer = resolve)) });
    const { allerVers } = await ouvrir(client);

    await userEvent.click(screen.getByTestId('tiers-connecter-banque'));
    await userEvent.click(screen.getByTestId('tiers-connecter-banque'));
    terminer({ statut: 'ok', donnees: 'https://p.test/' });

    await waitFor(() => expect(allerVers).toHaveBeenCalledTimes(1));
    expect(client.connecter).toHaveBeenCalledTimes(1);
  });
});

describe('écran Comptes Tiers : connecté', () => {
  it('[FR-012-04] masque le bloc de connexion et montre statut, boutons, patrimoine, comptes et transactions', async () => {
    await ouvrir(service(connecte()));

    const contenu = texte();
    expect(contenu).not.toContain('Agregez vos comptes bancaires');
    for (const attendu of [
      'Connected via Powens',
      'Deconnecter',
      'Rafraichir',
      'Patrimoine total (tous comptes)',
      'via Powens Open Banking API',
      'Mes comptes externes (2)',
      'Transactions — Compte Courant',
      'Date',
      'Libelle',
      'Montant',
    ]) {
      expect(contenu).toContain(attendu);
    }
    expect(screen.queryByTestId('tiers-connecter-banque')).toBeNull();
    const boutons = screen.getByTestId('tiers-deconnecter').parentElement as HTMLElement;
    expect(within(boutons).getByTestId('tiers-rafraichir')).toBeTruthy();
  });

  it('[FR-012-10] le patrimoine total est celui de l’état, au format du legacy', async () => {
    await ouvrir(service(connecte({ patrimoineTotal: '1355.00' })));

    expect(screen.getByTestId('tiers-patrimoine').textContent).toContain('EUR1,355.00');
  });

  it('[FR-012-11] chaque carte porte « Banque externe », son nom, son type traduit, son solde et son IBAN s’il existe', async () => {
    const comptes = [
      compte(1, 'A', 'checking', '1234.50', 'FR76'),
      compte(2, 'B', 'savings', '1.00', ''),
      compte(3, 'C', 'card', '1.00', null),
      compte(4, 'D', 'loan', '1.00'),
      compte(5, 'E', 'market', '1.00'),
      compte(6, 'F', null, '1.00'),
      compte(7, 'G', 'autre', '1.00'),
    ];
    await ouvrir(service(connecte({ comptes, compteSelectionne: 1, transactions: [] })));

    const types = ['Compte courant', 'Epargne', 'Carte', 'Credit', 'Bourse', 'Compte', 'autre'];
    comptes.forEach((c, indice) => {
      const carte = screen.getByTestId(`tiers-compte-${c.id}`).textContent ?? '';
      expect(carte).toContain('Banque externe');
      expect(carte).toContain(c.nom);
      expect(carte).toContain(types[indice]);
    });
    expect(screen.getByTestId('tiers-compte-1').textContent).toContain('EUR1,234.50');
    expect(screen.getByTestId('tiers-compte-1').textContent).toContain('FR76');
    expect(screen.getByTestId('tiers-compte-2').textContent).toBe('Banque externeBEpargneEUR1.00');
    expect(screen.getByTestId('tiers-compte-3').textContent).toBe('Banque externeCCarteEUR1.00');
  });

  it('[FR-012-12][BUG-042] un solde absent est « Solde indisponible », un solde nul « EUR0.00 », et le total exclut l’absent', async () => {
    const comptes = [compte(1, 'Sans solde', 'checking', null), compte(2, 'Nul', 'savings', '0.00'), compte(3, 'Plein', 'card', '120.50')];
    await ouvrir(service(connecte({ comptes, compteSelectionne: 1, transactions: [], patrimoineTotal: '120.50' })));

    expect(screen.getByTestId('tiers-compte-1').textContent).toContain('Solde indisponible');
    expect(screen.getByTestId('tiers-compte-1').textContent).not.toContain('0.00');
    expect(screen.getByTestId('tiers-compte-2').textContent).toContain('EUR0.00');
    expect(screen.getByTestId('tiers-compte-3').textContent).toContain('EUR120.50');
    expect(screen.getByTestId('tiers-patrimoine').textContent).toContain('EUR120.50');
  });

  it('[FR-012-13] montre les transactions : crédit vert préfixé « + », débit rouge, date « jj/MM/aaaa »', async () => {
    await ouvrir(service(connecte()));

    const lignes = within(screen.getByTestId('tiers-transactions')).getAllByRole('row');
    expect(lignes).toHaveLength(3);
    const credit = within(lignes[1]).getAllByRole('cell');
    expect(credit.map((cellule) => cellule.textContent)).toEqual(['30/09/2026', 'Salaire', '+2,000.00']);
    expect(credit[2].className).toContain('ecran-tiers--credit');
    const debit = within(lignes[2]).getAllByRole('cell');
    expect(debit[2].textContent).toBe('-750.00');
    expect(debit[2].className).toContain('ecran-tiers--debit');
  });

  it('[FR-012-13] un montant nul est rouge, sans « + »', async () => {
    await ouvrir(service(connecte({ transactions: [transaction('Neutre', '0.00')] })));

    const cellule = within(screen.getByTestId('tiers-transactions')).getByText('0.00');
    expect(cellule.className).toContain('ecran-tiers--debit');
  });

  it('[FR-012-13] au plus 20 transactions sont affichées', async () => {
    const transactions = Array.from({ length: 25 }, (_, indice) => transaction(`Ligne ${indice}`, '1.00'));
    await ouvrir(service(connecte({ transactions })));

    expect(within(screen.getByTestId('tiers-transactions')).getAllByRole('row')).toHaveLength(21);
  });

  it('[FR-012-13] sans transaction : « Aucune transaction disponible. »', async () => {
    await ouvrir(service(connecte({ transactions: [] })));

    expect(screen.getByTestId('tiers-transactions').textContent).toContain('Aucune transaction disponible.');
    expect(screen.queryByRole('table')).toBeNull();
  });

  it('[FR-012-13] un clic sur un autre compte ne met à jour que la section des transactions', async () => {
    const apres = connecte({ compteSelectionne: 12, transactions: [transaction('Interets', '3.20')] });
    const client = service(connecte(), { selectionner: () => Promise.resolve(ok(apres)) });
    await ouvrir(client);
    const carteAvant = screen.getByTestId('tiers-compte-11');

    await userEvent.click(screen.getByTestId('tiers-compte-12'));

    expect(await screen.findByText('Transactions — Livret A')).toBeTruthy();
    expect(screen.getByText('Interets')).toBeTruthy();
    expect(screen.queryByText('Salaire')).toBeNull();
    expect(screen.getByTestId('tiers-compte-11')).toBe(carteAvant);
    expect(client.selectionner).toHaveBeenCalledWith(expect.any(String), 12);
    expect(screen.getByTestId('tiers-compte-12').getAttribute('aria-pressed')).toBe('true');
  });

  it('[FR-012-13] un clic sur le compte déjà sélectionné n’appelle rien', async () => {
    const client = service(connecte());
    await ouvrir(client);

    await userEvent.click(screen.getByTestId('tiers-compte-11'));

    expect(client.selectionner).not.toHaveBeenCalled();
  });

  it('[FR-012-14][BUG-043] une sélection en 503 affiche l’indisponibilité, jamais « Aucune transaction disponible. »', async () => {
    const client = service(connecte(), { selectionner: () => Promise.resolve({ statut: 'indisponible' }) });
    await ouvrir(client);

    await userEvent.click(screen.getByTestId('tiers-compte-12'));

    await waitFor(() => expect(screen.getByTestId('tiers-transactions').textContent).toContain(INDISPONIBLE));
    expect(screen.getByTestId('tiers-transactions').textContent).toContain('Transactions — Livret A');
    expect(texte()).not.toContain('Aucune transaction disponible.');
    expect(screen.getByTestId('tiers-message-erreur').textContent).toBe(INDISPONIBLE);
  });

  it('[FR-012-14] une sélection en panne technique annonce l’erreur technique', async () => {
    await ouvrir(service(connecte(), { selectionner: () => Promise.resolve({ statut: 'technique' }) }));

    await userEvent.click(screen.getByTestId('tiers-compte-12'));

    expect((await screen.findByTestId('tiers-message-erreur')).textContent).toBe('Une erreur technique est survenue');
  });

  it('[FR-012-14] après une indisponibilité, un nouveau clic réussi remet les transactions et efface le message', async () => {
    const selectionner = vi
      .fn<ClientComptesTiers['selectionner']>()
      .mockResolvedValueOnce({ statut: 'indisponible' })
      .mockResolvedValueOnce(ok(connecte({ compteSelectionne: 12, transactions: [transaction('Interets', '3.20')] })));
    await ouvrir(service(connecte(), { selectionner }));

    await userEvent.click(screen.getByTestId('tiers-compte-12'));
    await screen.findByTestId('tiers-message-erreur');
    await userEvent.click(screen.getByTestId('tiers-compte-12'));

    expect(await screen.findByText('Interets')).toBeTruthy();
    expect(screen.queryByTestId('tiers-message-erreur')).toBeNull();
  });
});

describe('écran Comptes Tiers : messages de l’état', () => {
  it.each([
    ['[FR-012-07][BUG-040][BUG-044] retour en erreur', 'La connexion a votre banque n a pas abouti (user_cancelled). Veuillez recommencer la connexion.'],
    ['[FR-012-08] session expirée', 'Session expiree. Veuillez recommencer la connexion.'],
    ['[FR-012-09] retour incomplet', 'Callback Powens incomplet. Parametres recus: [code]'],
    ['[BUG-043] Powens indisponible au retour', INDISPONIBLE],
  ])('%s : message tel quel au-dessus du bloc de connexion', async (_cas, message) => {
    await ouvrir(service({ ...NON_CONNECTE, message }));

    expect(screen.getByTestId('tiers-message-erreur').textContent).toBe(message);
    expect(texte()).toContain('Agregez vos comptes bancaires');
    expect(texte()).not.toContain('Mes comptes externes (0)');
    expect(texte()).not.toContain('Connexion reussie');
  });

  it('[FR-012-09] connecté sans compte : « Mes comptes externes (0) » et le message', async () => {
    const message = 'Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes.';
    await ouvrir(service(connecte({ comptes: [], compteSelectionne: null, transactions: [], message, patrimoineTotal: '0.00' })));

    expect(texte()).toContain('Mes comptes externes (0)');
    expect(screen.getByTestId('tiers-message-erreur').textContent).toBe(message);
    expect(screen.queryByTestId('tiers-transactions')).toBeNull();
  });
});

describe('écran Comptes Tiers : rafraîchir et déconnecter', () => {
  it('[FR-012-16] « Rafraichir » remplace l’état sans reconnexion', async () => {
    const apres = connecte({ comptes: [COURANT, LIVRET, compte(13, 'Nouveau', 'card', '5.00')], patrimoineTotal: '1360.00' });
    const client = service(connecte(), { rafraichir: () => Promise.resolve(ok(apres)) });
    await ouvrir(client);

    await userEvent.click(screen.getByTestId('tiers-rafraichir'));

    expect(await screen.findByText('Mes comptes externes (3)')).toBeTruthy();
    expect(screen.getByTestId('tiers-patrimoine').textContent).toContain('EUR1,360.00');
    expect(screen.queryByTestId('tiers-connecter-banque')).toBeNull();
  });

  it('[FR-012-16][BUG-043] un rafraîchissement en 503 affiche l’indisponibilité et garde l’état', async () => {
    await ouvrir(service(connecte(), { rafraichir: () => Promise.resolve({ statut: 'indisponible' }) }));

    await userEvent.click(screen.getByTestId('tiers-rafraichir'));

    expect((await screen.findByTestId('tiers-message-erreur')).textContent).toBe(INDISPONIBLE);
    expect(texte()).toContain('Mes comptes externes (2)');
    expect(texte()).toContain('Salaire');
  });

  it('[FR-012-15] « Deconnecter » revient au bloc de connexion, sans comptes, transactions ni message', async () => {
    const client = service(connecte({ message: 'Un message' }));
    await ouvrir(client);

    await userEvent.click(screen.getByTestId('tiers-deconnecter'));

    expect(await screen.findByTestId('tiers-connecter-banque')).toBeTruthy();
    expect(client.deconnecter).toHaveBeenCalledWith(expect.any(String));
    expect(screen.queryByTestId('tiers-message-erreur')).toBeNull();
    expect(texte()).not.toContain('Compte Courant');
    expect(texte()).not.toContain('Salaire');
    expect(texte()).toContain('Agregez vos comptes bancaires');
  });

  it('[FR-012-15] une déconnexion en panne garde l’état et l’annonce', async () => {
    await ouvrir(service(connecte(), { deconnecter: () => Promise.resolve({ statut: 'technique' }) }));

    await userEvent.click(screen.getByTestId('tiers-deconnecter'));

    expect((await screen.findByTestId('tiers-message-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(texte()).toContain('Mes comptes externes (2)');
  });

  it('[FR-012-16] un 401 au rafraîchissement mène à /login', async () => {
    const { routeur } = await ouvrir(service(connecte(), { rafraichir: () => Promise.resolve({ statut: 'nonAuthentifie' }) }));

    await userEvent.click(screen.getByTestId('tiers-rafraichir'));

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });
});

describe('écran Comptes Tiers : confidentialité', () => {
  it('[FR-012-04] rien de l’état Powens n’est écrit dans localStorage ni sessionStorage, aucun jeton Powens affiché', async () => {
    const jetonSession = jetonDeTest({ sub: '1', roles: ['CLIENT'] });
    await ouvrir(service(connecte({ message: 'Un message' })));
    await userEvent.click(screen.getByTestId('tiers-compte-12'));

    expect(Object.keys(localStorage)).toEqual(['nb.jeton']);
    expect(localStorage.getItem('nb.jeton')).toBe(jetonSession);
    expect(sessionStorage.length).toBe(0);
    expect(texte().toLowerCase()).not.toContain('token');
  });
});
