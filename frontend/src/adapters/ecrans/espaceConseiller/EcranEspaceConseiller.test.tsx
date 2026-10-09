// @vitest-environment jsdom
import { act, cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ClientPortefeuille, ReponsePortefeuille } from '../../../application/consulterPortefeuille';
import type { ReponseConnexion } from '../../../application/seConnecter';
import type { ClientDuPortefeuille, ComptesDuClient, PretsDuClient } from '../../../domain/portefeuille';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const JEAN: ClientDuPortefeuille = {
  clientId: 1, nomComplet: 'Jean Dupont', login: 'user1', email: 'jean.dupont@example.com', telephone: '+33601020304', actif: true,
};
const MARIE: ClientDuPortefeuille = {
  clientId: 2, nomComplet: 'Marie Lebrun', login: 'user2', email: 'marie.lebrun@example.com', telephone: null, actif: true,
};
const COMPTES: readonly ComptesDuClient[] = [
  {
    clientId: 1,
    comptes: [
      { compteId: 10, numero: 'FR76-0001-0001', type: 'COURANT', solde: '2130.00' },
      { compteId: 11, numero: 'FR76-0001-0002', type: 'EPARGNE', solde: '15200.00' },
    ],
  },
  { clientId: 2, comptes: [] },
];
const PRETS: readonly PretsDuClient[] = [
  {
    clientId: 1,
    prets: [
      { pretId: 5, type: 'CONSOMMATION', montant: '5000.00', mensualite: '146.51', statut: 'EN_ATTENTE' },
      { pretId: 6, type: 'TRAVAUX', montant: '5000.00', mensualite: '218.63', statut: 'EN_COURS' },
      { pretId: 7, type: 'IMMOBILIER', montant: '120000.50', mensualite: '850.00', statut: 'EN_COURS' },
    ],
  },
  { clientId: 2, prets: [] },
];

const ok = <T,>(donnees: T): Promise<ReponsePortefeuille<T>> => Promise.resolve({ statut: 'ok', donnees });

const PORTEFEUILLE_SOPHIE: Partial<ClientPortefeuille> = {
  lireIdentites: () => ok([JEAN, MARIE]),
  lireComptes: () => ok(COMPTES),
  lirePrets: () => ok(PRETS),
};

const PROFIL_SOPHIE = () => Promise.resolve({ statut: 'profil' as const, nomComplet: 'Sophie Durand' });

function ouvrir(
  portefeuille: Partial<ClientPortefeuille> = PORTEFEUILLE_SOPHIE,
  role = 'CONSEILLER',
  repondre?: () => Promise<ReponseConnexion>,
) {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles: [role] }));
  return rendreApplication(
    '/espace-conseiller',
    repondre,
    PROFIL_SOPHIE,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    portefeuille,
  );
}

async function charge() {
  await waitFor(() => expect(screen.getByTestId('conseiller-contenu').getAttribute('aria-busy')).toBeNull());
  await screen.findByTestId('nav-salutation');
}

describe('EcranEspaceConseiller', () => {
  it('[FR-015-01] un CLIENT arrive sur /tableau-de-bord sans message, sans aucun appel au portefeuille', async () => {
    const { routeur, portefeuille } = ouvrir(PORTEFEUILLE_SOPHIE, 'CLIENT');

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/tableau-de-bord'));
    expect(screen.queryByTestId('conseiller-message-erreur')).toBeNull();
    expect(screen.queryByTestId('conseiller-contenu')).toBeNull();
    expect(portefeuille.lireIdentites).not.toHaveBeenCalled();
    expect(portefeuille.lireComptes).not.toHaveBeenCalled();
    expect(portefeuille.lirePrets).not.toHaveBeenCalled();
  });

  it('[FR-015-02] les trois appels partent avant qu’aucun ne soit résolu, sans paramètre', async () => {
    const liberations: Array<() => void> = [];
    const differe = <T,>(donnees: T) => () =>
      new Promise<ReponsePortefeuille<T>>((resolu) => liberations.push(() => resolu({ statut: 'ok', donnees })));
    const { portefeuille } = ouvrir({ lireIdentites: differe([JEAN]), lireComptes: differe(COMPTES), lirePrets: differe(PRETS) });

    await waitFor(() => expect(portefeuille.lirePrets).toHaveBeenCalled());
    expect(portefeuille.lireIdentites).toHaveBeenCalledOnce();
    expect(portefeuille.lireComptes).toHaveBeenCalledOnce();
    expect(portefeuille.lirePrets).toHaveBeenCalledOnce();
    expect(portefeuille.lireIdentites).toHaveBeenCalledWith();
    expect(screen.getByTestId('conseiller-contenu').getAttribute('aria-busy')).toBe('true');

    act(() => liberations.forEach((liberer) => liberer()));
    await charge();
    expect(screen.getByText('Jean Dupont')).toBeTruthy();
  });

  it('[FR-015-02][FR-015-03] sophie : bandeau, sous-en-tête et indicateurs en card-muted', async () => {
    ouvrir();
    await charge();

    expect(screen.getByTestId('conseiller-bandeau').textContent).toBe('Espace Conseiller — Bonjour Sophie Durand, vous gerez 2 client(s)');
    expect(within(screen.getByTestId('conseiller-bandeau')).getByText('2 client(s)').tagName).toBe('STRONG');
    expect(within(screen.getByTestId('conseiller-bandeau')).getByText('Espace Conseiller').tagName).toBe('STRONG');
    expect(screen.getByTestId('nav-sous-mon-portefeuille').textContent).toBe('Mon portefeuille');
    expect(screen.getByTestId('nav-sous-conseiller-ia').textContent).toBe('Conseiller IA');
    expect(screen.getByTestId('nav-sous-conseiller-ia').getAttribute('href')).toBe('/mon-conseiller');

    const indicateurs = [
      ['clients', 'Mes clients', '2', 'clients dans mon portefeuille'],
      ['encours', 'Encours total', 'EUR17,330', 'solde cumulé mes clients'],
      ['prets-en-attente', 'Prets en attente', '1', 'a traiter'],
    ];
    for (const [cle, libelle, valeur, sousLibelle] of indicateurs) {
      const indicateur = screen.getByTestId(`conseiller-indicateur-${cle}`);
      expect(indicateur.textContent).toBe(`${libelle}${valeur}${sousLibelle}`);
      expect(indicateur.parentElement?.classList.contains('card-muted')).toBe(true);
    }
    expect(document.title).toBe('BankApp Demo - Espace Conseiller');
  });

  it('[FR-015-04] portefeuille vide : message legacy et indicateurs à zéro', async () => {
    ouvrir({ lireIdentites: () => ok([]), lireComptes: () => ok([]), lirePrets: () => ok([]) });
    await charge();

    expect(screen.getByTestId('conseiller-vide').textContent).toBe(
      'Aucun client dans votre portefeuille.Contactez l administrateur pour l affectation des clients.',
    );
    expect(screen.getByTestId('conseiller-indicateur-clients-valeur').textContent).toBe('0');
    expect(screen.getByTestId('conseiller-indicateur-encours-valeur').textContent).toBe('EUR0');
    expect(screen.getByTestId('conseiller-indicateur-prets-en-attente-valeur').textContent).toBe('0');
    expect(screen.getByTestId('conseiller-bandeau').textContent).toBe('Espace Conseiller — Bonjour Sophie Durand, vous gerez 0 client(s)');
  });

  it('[FR-015-05] une carte par client, dans l’ordre reçu : avatar, nom, @login, badge et statistiques', async () => {
    ouvrir({ ...PORTEFEUILLE_SOPHIE, lireIdentites: () => ok([JEAN, { ...MARIE, actif: false }]) });
    await charge();

    const cartes = screen.getAllByTestId(/^conseiller-carte-/);
    expect(cartes.map((carte) => carte.getAttribute('data-testid'))).toEqual(['conseiller-carte-1', 'conseiller-carte-2']);
    const jean = screen.getByTestId('conseiller-carte-1');
    expect(within(jean).getByText('J').getAttribute('aria-hidden')).toBe('true');
    expect(within(jean).getByText('@user1')).toBeTruthy();
    expect(screen.getByTestId('conseiller-statut-client-1').textContent).toBe('Actif');
    const statistiques = [...jean.querySelectorAll('dt')].map((dt) => `${dt.textContent}=${dt.nextElementSibling?.textContent}`);
    expect(statistiques).toEqual(['Solde total=EUR17,330', 'Comptes=2', 'Prets actifs=2', 'Prets en attente=1']);
    expect(within(jean).getByText('Voir detail')).toBeTruthy();
    expect(jean.classList.contains('ecran-espace-conseiller__carte--inactive')).toBe(false);

    const marie = screen.getByTestId('conseiller-carte-2');
    expect(marie.classList.contains('ecran-espace-conseiller__carte--inactive')).toBe(true);
    expect(screen.getByTestId('conseiller-statut-client-2').textContent).toBe('Inactif');
    expect([...marie.querySelectorAll('dd')].map((dd) => dd.textContent)).toEqual(['EUR0', '0', '0', '0']);
  });

  it('[FR-015-06] « ⚠ n pret(s) en attente » l’emporte sur « ✓ n pret(s) en cours » ; rien sans prêt', async () => {
    const troisieme: ClientDuPortefeuille = { ...MARIE, clientId: 3, nomComplet: 'Paul Martin', login: 'user3' };
    ouvrir({
      lireIdentites: () => ok([JEAN, MARIE, troisieme]),
      lireComptes: () => ok([]),
      lirePrets: () =>
        ok([
          ...PRETS,
          { clientId: 2, prets: [] },
          {
            clientId: 3,
            prets: [
              { pretId: 8, type: 'AUTO', montant: '1.00', mensualite: '1.00', statut: 'EN_COURS' },
              { pretId: 9, type: 'AUTO', montant: '1.00', mensualite: '1.00', statut: 'EN_COURS' },
            ],
          },
        ]),
    });
    await charge();

    expect(screen.getByTestId('conseiller-indicateur-prets-1').textContent).toBe('⚠ 1 pret(s) en attente');
    expect(screen.queryByTestId('conseiller-indicateur-prets-2')).toBeNull();
    expect(screen.getByTestId('conseiller-indicateur-prets-3').textContent).toBe('✓ 2 pret(s) en cours');
    expect(screen.getByTestId('conseiller-carte-1').textContent).not.toContain('pret(s) en cours');
  });

  it('[FR-015-07] « Voir detail » ouvre le panneau inline, focus au titre ; « Fermer » rend le focus au bouton', async () => {
    const utilisateur = userEvent.setup();
    const { routeur } = ouvrir();
    await charge();

    await utilisateur.click(screen.getByTestId('conseiller-voir-detail-1'));

    const detail = screen.getByTestId('conseiller-detail');
    const titre = within(detail).getByRole('heading', { level: 2 });
    expect(titre.textContent).toBe('Jean Dupont');
    expect(document.activeElement).toBe(titre);
    expect(within(detail).getByText('@user1 · jean.dupont@example.com · +33601020304')).toBeTruthy();
    expect(within(detail).queryByText(/Detail :|Client depuis/)).toBeNull();
    expect(detail.closest('dialog')).toBeNull();
    expect(routeur.state.location.pathname).toBe('/espace-conseiller');
    expect(within(detail).getByText('Comptes bancaires')).toBeTruthy();
    expect(within(detail).getByText('Prets')).toBeTruthy();
    const entetes = [...detail.querySelectorAll('th')].map((th) => th.textContent);
    expect(entetes).toEqual(['Numero', 'Type', 'Solde', 'Type', 'Montant', 'Mensualite', 'Statut']);

    await utilisateur.click(screen.getByTestId('conseiller-fermer-detail'));

    expect(screen.queryByTestId('conseiller-detail')).toBeNull();
    expect(document.activeElement).toBe(screen.getByTestId('conseiller-voir-detail-1'));
  });

  it('[FR-015-07] Échap ferme le panneau et rend le focus au bouton ; une autre touche ne fait rien', async () => {
    const utilisateur = userEvent.setup();
    ouvrir();
    await charge();

    await utilisateur.click(screen.getByTestId('conseiller-voir-detail-2'));
    await utilisateur.keyboard('a');
    expect(screen.getByTestId('conseiller-detail')).toBeTruthy();
    expect(within(screen.getByTestId('conseiller-detail')).getByText('@user2 · marie.lebrun@example.com')).toBeTruthy();

    await utilisateur.keyboard('{Escape}');

    expect(screen.queryByTestId('conseiller-detail')).toBeNull();
    expect(document.activeElement).toBe(screen.getByTestId('conseiller-voir-detail-2'));
  });

  it('[FR-015-08] client sans compte ni prêt : « Aucun compte », « Aucun pret »', async () => {
    const utilisateur = userEvent.setup();
    ouvrir();
    await charge();

    await utilisateur.click(screen.getByTestId('conseiller-voir-detail-2'));

    const detail = screen.getByTestId('conseiller-detail');
    expect(within(detail).getByText('Aucun compte')).toBeTruthy();
    expect(within(detail).getByText('Aucun pret')).toBeTruthy();
    expect(detail.querySelector('table')).toBeNull();
  });

  it('[FR-015-09][BUG-053] codes tels quels : badges de type et de statut, DEFAUT en texte brut, formats legacy', async () => {
    const utilisateur = userEvent.setup();
    ouvrir({
      ...PORTEFEUILLE_SOPHIE,
      lirePrets: () =>
        ok([
          {
            clientId: 1,
            prets: [
              { pretId: 5, type: 'CONSOMMATION', montant: '5000.00', mensualite: '146.51', statut: 'EN_ATTENTE' },
              { pretId: 6, type: 'AUTO', montant: '17330.50', mensualite: '1218.63', statut: 'EN_COURS' },
              { pretId: 7, type: null, montant: '1000.00', mensualite: '10.00', statut: 'REFUSE' },
              { pretId: 8, type: 'TRAVAUX', montant: '1000.00', mensualite: '10.00', statut: 'REMBOURSE' },
              { pretId: 9, type: 'IMMOBILIER', montant: '1000.00', mensualite: '10.00', statut: 'DEFAUT' },
            ],
          },
        ]),
    });
    await charge();
    await utilisateur.click(screen.getByTestId('conseiller-voir-detail-1'));

    for (const [id, type] of [[10, 'COURANT'], [11, 'EPARGNE']] as const) {
      const badge = screen.getByTestId(`conseiller-type-compte-${id}`);
      expect(badge.textContent).toBe(type);
      expect(badge.classList.contains('etiquette')).toBe(true);
    }
    for (const [id, statut] of [[5, 'EN_ATTENTE'], [6, 'EN_COURS'], [7, 'REFUSE'], [8, 'REMBOURSE']] as const) {
      const badge = screen.getByTestId(`conseiller-statut-pret-${id}`);
      expect(badge.textContent).toBe(statut);
      expect(badge.classList.contains('etiquette')).toBe(true);
    }
    const defaut = screen.getByTestId('conseiller-statut-pret-9');
    expect(defaut.textContent).toBe('DEFAUT');
    expect(defaut.classList.contains('etiquette')).toBe(false);

    const lignesComptes = [...screen.getByTestId('conseiller-detail-comptes').querySelectorAll('tbody tr')].map((tr) => tr.textContent);
    expect(lignesComptes).toEqual(['FR76-0001-0001COURANTEUR2,130.00', 'FR76-0001-0002EPARGNEEUR15,200.00']);
    const lignesPrets = [...screen.getByTestId('conseiller-detail-prets').querySelectorAll('tbody tr')].map((tr) =>
      [...tr.querySelectorAll('td')].map((td) => td.textContent),
    );
    expect(lignesPrets).toEqual([
      ['CONSOMMATION', 'EUR5,000', 'EUR146.51', 'EN_ATTENTE'],
      ['AUTO', 'EUR17,330.5', 'EUR1,218.63', 'EN_COURS'],
      ['', 'EUR1,000', 'EUR10.00', 'REFUSE'],
      ['TRAVAUX', 'EUR1,000', 'EUR10.00', 'REMBOURSE'],
      ['IMMOBILIER', 'EUR1,000', 'EUR10.00', 'DEFAUT'],
    ]);
  });

  it('[FR-015-02] une panne affiche le message reçu, sans indicateurs ni cartes ; le bandeau reste', async () => {
    ouvrir({
      ...PORTEFEUILLE_SOPHIE,
      lirePrets: () => Promise.resolve({ statut: 'erreur', message: 'Service crédits indisponible' }),
      lireComptes: () => Promise.resolve({ statut: 'erreur', message: 'Une erreur technique est survenue' }),
    });
    await charge();

    const erreur = screen.getByTestId('conseiller-message-erreur');
    expect([...erreur.querySelectorAll('p')].map((p) => p.textContent)).toEqual([
      'Une erreur technique est survenue',
      'Service crédits indisponible',
    ]);
    expect(screen.getByTestId('conseiller-bandeau').textContent).toBe('Espace Conseiller');
    expect(screen.queryByTestId('conseiller-indicateur-clients')).toBeNull();
    expect(screen.queryByTestId('conseiller-carte-1')).toBeNull();
  });

  it('[FR-015-02] un 401 termine la session : retour à /login', async () => {
    const { routeur } = ouvrir({ ...PORTEFEUILLE_SOPHIE, lireComptes: () => Promise.resolve({ statut: 'nonAuthentifie' }) });

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    expect(localStorage.getItem('nb.jeton')).toBeNull();
  });

  it('[FR-015-02] retour sur la route : aucun nouvel appel ; un résultat en erreur n’est pas gardé', async () => {
    let appel = 0;
    const { routeur, portefeuille } = ouvrir({
      ...PORTEFEUILLE_SOPHIE,
      lirePrets: () => {
        appel += 1;
        return appel === 1 ? Promise.resolve({ statut: 'erreur', message: 'Service crédits indisponible' }) : ok(PRETS);
      },
    });
    await screen.findByTestId('conseiller-message-erreur');

    await act(() => routeur.navigate('/produits'));
    await act(() => routeur.navigate('/espace-conseiller'));
    await screen.findByTestId('conseiller-carte-1');
    expect(portefeuille.lireIdentites).toHaveBeenCalledTimes(2);

    await act(() => routeur.navigate('/produits'));
    await act(() => routeur.navigate('/espace-conseiller'));
    await screen.findByTestId('conseiller-carte-1');
    expect(portefeuille.lireIdentites).toHaveBeenCalledTimes(2);
    expect(portefeuille.lireComptes).toHaveBeenCalledTimes(2);
    expect(portefeuille.lirePrets).toHaveBeenCalledTimes(2);
  });

  it('[FR-015-02] après déconnexion puis reconnexion, le portefeuille est rechargé', async () => {
    const utilisateur = userEvent.setup();
    const { routeur, portefeuille } = ouvrir(PORTEFEUILLE_SOPHIE, 'CONSEILLER', () =>
      Promise.resolve({ statut: 'acceptee', roles: ['CONSEILLER'] }),
    );
    await charge();
    expect(portefeuille.lireIdentites).toHaveBeenCalledOnce();

    await utilisateur.click(screen.getByTestId('nav-deconnexion'));
    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    await utilisateur.type(screen.getByTestId('login-identifiant'), 'sophie');
    await utilisateur.type(screen.getByTestId('login-mot-de-passe'), 'secret{Enter}');

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/espace-conseiller'));
    await screen.findByTestId('conseiller-carte-1');
    expect(portefeuille.lireIdentites).toHaveBeenCalledTimes(2);
    expect(portefeuille.lireComptes).toHaveBeenCalledTimes(2);
    expect(portefeuille.lirePrets).toHaveBeenCalledTimes(2);
  });

  it('[FR-015-01] un ADMIN voit l’Espace Conseiller ; profil indisponible : bandeau sans salutation', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['ADMIN'] }));
    rendreApplication('/espace-conseiller', undefined, () => Promise.resolve({ statut: 'technique' }));

    await waitFor(() => expect(screen.getByTestId('conseiller-contenu').getAttribute('aria-busy')).toBeNull());
    await waitFor(() => expect(screen.getByTestId('application-racine').getAttribute('aria-busy')).toBeNull());
    expect(screen.getByTestId('conseiller-bandeau').textContent).toBe('Espace Conseiller');
    expect(screen.getByTestId('conseiller-vide')).toBeTruthy();
  });
});
