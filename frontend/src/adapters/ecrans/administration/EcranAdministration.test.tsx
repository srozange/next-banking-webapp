// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ClientAdministration, ReponseAdministration } from '../../../application/consulterAdministration';
import type { ClientAdministre, UtilisateurAdministre } from '../../../domain/administration';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const client = (
  clientId: number,
  nomComplet: string,
  login: string,
  email: string,
  nomConseiller: string | null,
  actif = true,
): ClientAdministre => ({
  clientId,
  nomComplet,
  login,
  email,
  telephone: `+3360102030${clientId + 3}`,
  role: 'CLIENT',
  actif,
  nomConseiller,
  dateCreation: clientId === 1 ? '2024-02-01' : '2023-11-20',
});

// Jeu équivalent aux données de référence : 7 utilisateurs, dont 4 clients (3 actifs).
const JEAN = client(1, 'Jean Dupont', 'user1', 'jean.dupont@example.com', 'Sophie Durand');
const LUCIE = client(4, 'Lucie Martin', 'user4', 'lucie.martin@example.com', 'Marc Lefevre', false);
const MARIE = client(2, 'Marie Lebrun', 'user2', 'marie.lebrun@example.com', 'Sophie Durand');
const PIERRE = { ...client(3, 'Pierre Bernard', 'user3', 'pierre.bernard@example.com', null), telephone: null };
const CLIENTS = [JEAN, LUCIE, MARIE, PIERRE];

const utilisateur = (utilisateurId: number, nomComplet: string, login: string, role: string, actif = true): UtilisateurAdministre => ({
  utilisateurId,
  nomComplet,
  login,
  email: `${login}@demobank.fr`,
  role,
  actif,
});
const UTILISATEURS = [
  utilisateur(10, 'Alice Martin', 'admin', 'ADMIN'),
  utilisateur(1, 'Jean Dupont', 'user1', 'CLIENT'),
  utilisateur(4, 'Lucie Martin', 'user4', 'CLIENT', false),
  utilisateur(12, 'Marc Lefevre', 'marc', 'CONSEILLER'),
  utilisateur(2, 'Marie Lebrun', 'user2', 'CLIENT'),
  utilisateur(3, 'Pierre Bernard', 'user3', 'CLIENT'),
  utilisateur(11, 'Sophie Durand', 'sophie', 'CONSEILLER'),
];

const ok = <T,>(donnees: T): Promise<ReponseAdministration<T>> => Promise.resolve({ statut: 'ok', donnees });

const REFERENCE: ClientAdministration = {
  lireIndicateursIdentite: () => ok({ totalClients: 4, clientsActifs: 3, conseillers: 2, totalUtilisateurs: 7 }),
  lireIndicateursPrets: () => ok({ pretsEnAttente: 2, montantEnAttente: '15000.00' }),
  lireClients: () => ok(CLIENTS),
  lireUtilisateurs: () => ok(UTILISATEURS),
};

function ouvrir(administration: Partial<ClientAdministration> = {}, role = 'ADMIN') {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles: [role] }));
  return rendreApplication(
    '/administration',
    undefined,
    () => Promise.resolve({ statut: 'profil' as const, nomComplet: 'Alice Martin' }),
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
    undefined,
    { ...REFERENCE, ...administration },
  );
}

const valeur = (cle: string) => screen.getByTestId(`admin-indicateur-${cle}-valeur`).textContent;
const sousLibelle = (cle: string) => screen.getByTestId(`admin-indicateur-${cle}-sous-libelle`).textContent;
const lignesClients = () => within(screen.getByTestId('admin-clients')).getAllByRole('row').slice(1);

describe('EcranAdministration', () => {
  it('[FR-016-02] affiche les quatre indicateurs de référence, libellés et sous-libellés du legacy', async () => {
    ouvrir();

    await screen.findByTestId('admin-indicateur-total-clients');
    expect(valeur('total-clients')).toBe('4');
    expect(sousLibelle('total-clients')).toBe('3 actifs');
    expect(valeur('conseillers')).toBe('2');
    expect(sousLibelle('conseillers')).toBe('actifs');
    expect(valeur('prets-en-attente')).toBe('2');
    expect(sousLibelle('prets-en-attente')).toBe('EUR15,000 a traiter');
    expect(valeur('total-utilisateurs')).toBe('7');
    expect(sousLibelle('total-utilisateurs')).toBe('tous roles confondus');
    const libelles = within(screen.getByTestId('admin-indicateurs'))
      .getAllByText(/^(Total clients|Conseillers|Prets en attente|Total utilisateurs)$/)
      .map((element) => element.textContent);
    expect(libelles).toEqual(['Total clients', 'Conseillers', 'Prets en attente', 'Total utilisateurs']);
  });

  it('[FR-016-02] sans prêt en attente, la carte affiche 0 et EUR0 a traiter', async () => {
    ouvrir({ lireIndicateursPrets: () => ok({ pretsEnAttente: 0, montantEnAttente: '0.00' }) });

    await screen.findByTestId('admin-indicateur-prets-en-attente');
    expect(valeur('prets-en-attente')).toBe('0');
    expect(sousLibelle('prets-en-attente')).toBe('EUR0 a traiter');
  });

  it('[FR-016-02] les quatre appels partent avant qu’aucun ne soit résolu', async () => {
    const enAttente = () => new Promise<never>(() => {});
    const { administration } = ouvrir({
      lireIndicateursIdentite: enAttente,
      lireIndicateursPrets: enAttente,
      lireClients: enAttente,
      lireUtilisateurs: enAttente,
    });

    await waitFor(() => expect(administration.lireUtilisateurs).toHaveBeenCalled());
    expect(administration.lireIndicateursIdentite).toHaveBeenCalled();
    expect(administration.lireIndicateursPrets).toHaveBeenCalled();
    expect(administration.lireClients).toHaveBeenCalled();
    expect(screen.getByTestId('admin-ecran').getAttribute('aria-busy')).toBe('true');
  });

  it('[FR-016-02] une panne de credits affiche son message dans les indicateurs, le reste de la page s’affiche', async () => {
    ouvrir({ lireIndicateursPrets: () => Promise.resolve({ statut: 'erreur', message: 'Service crédits indisponible' }) });

    const indicateurs = screen.getByTestId('admin-indicateurs');
    expect((await within(indicateurs).findByTestId('admin-message-erreur')).textContent).toBe('Service crédits indisponible');
    expect(screen.getAllByTestId('admin-message-erreur')).toHaveLength(1);
    expect(screen.queryByTestId('admin-indicateur-prets-en-attente')).toBeNull();
    expect(valeur('total-clients')).toBe('4');
    expect(valeur('conseillers')).toBe('2');
    expect(valeur('total-utilisateurs')).toBe('7');
    expect(lignesClients()).toHaveLength(4);
    expect(within(screen.getByTestId('admin-utilisateurs')).getAllByRole('row')).toHaveLength(8);
  });

  it('[FR-016-02] une panne d’identité ne laisse que la carte des prêts', async () => {
    ouvrir({ lireIndicateursIdentite: () => Promise.resolve({ statut: 'erreur', message: 'Une erreur technique est survenue' }) });

    await screen.findByTestId('admin-indicateur-prets-en-attente');
    expect(within(screen.getByTestId('admin-indicateurs')).getByTestId('admin-message-erreur').textContent).toBe(
      'Une erreur technique est survenue',
    );
    expect(screen.queryByTestId('admin-indicateur-total-clients')).toBeNull();
    expect(screen.queryByTestId('admin-indicateur-total-utilisateurs')).toBeNull();
  });

  it('[FR-016-08] la panne des clients ou des utilisateurs s’affiche dans sa seule section', async () => {
    ouvrir({
      lireClients: () => Promise.resolve({ statut: 'erreur', message: 'Clients indisponibles' }),
      lireUtilisateurs: () => Promise.resolve({ statut: 'erreur', message: 'Utilisateurs indisponibles' }),
    });

    const clients = screen.getByTestId('admin-clients-section');
    expect((await within(clients).findByTestId('admin-message-erreur')).textContent).toBe('Clients indisponibles');
    expect(within(screen.getByTestId('admin-utilisateurs-section')).getByTestId('admin-message-erreur').textContent).toBe(
      'Utilisateurs indisponibles',
    );
    expect(screen.queryByTestId('admin-clients')).toBeNull();
    expect(screen.queryByTestId('admin-recherche')).toBeNull();
    expect(screen.queryByTestId('admin-utilisateurs')).toBeNull();
    expect(valeur('total-clients')).toBe('4');
  });

  it('[FR-016-08] la gestion des clients liste les 4 clients avec rôle, statut, conseiller et bouton Detail', async () => {
    ouvrir();

    await screen.findByTestId('admin-clients');
    const entetes = within(screen.getByTestId('admin-clients')).getAllByRole('columnheader').map((th) => th.textContent);
    expect(entetes).toEqual(['Nom', 'Login', 'Email', 'Role', 'Statut', 'Conseiller', 'Actions']);
    expect(lignesClients().map((ligne) => within(ligne).getAllByRole('cell')[0]?.textContent)).toEqual([
      'Jean Dupont',
      'Lucie Martin',
      'Marie Lebrun',
      'Pierre Bernard',
    ]);
    const jean = within(screen.getByTestId('admin-client-1')).getAllByRole('cell').map((cellule) => cellule.textContent);
    expect(jean).toEqual(['Jean Dupont', 'user1', 'jean.dupont@example.com', 'CLIENT', 'Actif', 'Sophie Durand', 'Detail']);
    expect(screen.getByTestId('admin-role-1').textContent).toBe('CLIENT');
    expect(screen.getByTestId('admin-statut-4').textContent).toBe('Inactif');
    expect(within(screen.getByTestId('admin-client-3')).getAllByRole('cell')[5]?.textContent).toBe('');
    expect(within(screen.getByTestId('admin-client-1')).getAllByRole('button').map((bouton) => bouton.textContent)).toEqual(['Detail']);
    expect(screen.queryByText('Administrateur')).toBeNull();
  });

  it('[FR-016-08] le filtre s’applique au clic sur Filtrer : DUPONT, user, puis zzz', async () => {
    const utilisateur = userEvent.setup();
    ouvrir();
    const recherche = await screen.findByTestId('admin-recherche');
    expect(recherche.getAttribute('aria-label')).toBe('Rechercher par nom, login, email...');
    expect(recherche.getAttribute('placeholder')).toBe('Rechercher par nom, login, email...');

    await utilisateur.type(recherche, 'DUPONT');
    expect(lignesClients()).toHaveLength(4);
    await utilisateur.click(screen.getByTestId('admin-filtrer'));
    expect(lignesClients().map((ligne) => ligne.getAttribute('data-testid'))).toEqual(['admin-client-1']);

    await utilisateur.clear(recherche);
    await utilisateur.type(recherche, 'user');
    await utilisateur.click(screen.getByTestId('admin-filtrer'));
    expect(lignesClients()).toHaveLength(4);

    await utilisateur.clear(recherche);
    await utilisateur.type(recherche, 'zzz');
    await utilisateur.click(screen.getByTestId('admin-filtrer'));
    expect(lignesClients().map((ligne) => ligne.textContent)).toEqual(['Aucun client trouve']);
    expect(within(screen.getByTestId('admin-clients')).getAllByRole('columnheader')).toHaveLength(7);
  });

  it('[FR-016-08] Entrée dans le champ filtre comme Filtrer ; un terme vide rend tous les clients', async () => {
    const utilisateur = userEvent.setup();
    ouvrir();
    const recherche = await screen.findByTestId('admin-recherche');

    await utilisateur.type(recherche, '  lebrun {Enter}');
    expect(lignesClients().map((ligne) => ligne.getAttribute('data-testid'))).toEqual(['admin-client-2']);

    await utilisateur.clear(recherche);
    await utilisateur.type(recherche, '{Enter}');
    expect(lignesClients()).toHaveLength(4);
  });

  it('[FR-016-09][FR-016-10][BUG-055] Detail ouvre le panneau inline, focus au titre, champs et Client depuis', async () => {
    const utilisateur = userEvent.setup();
    ouvrir();

    await utilisateur.click(await screen.findByTestId('admin-detail-1'));

    const titre = screen.getByRole('heading', { level: 3, name: 'Detail : Jean Dupont' });
    expect(document.activeElement).toBe(titre);
    expect(titre.getAttribute('tabindex')).toBe('-1');
    const panneau = screen.getByTestId('admin-panneau-detail');
    const libelles = within(panneau).getAllByRole('term').map((dt) => dt.textContent);
    expect(libelles).toEqual(['Nom complet', 'Identifiant', 'Email', 'Telephone', 'Role', 'Statut', 'Conseiller assigne', 'Client depuis']);
    const valeurs = within(panneau).getAllByRole('definition').map((dd) => dd.textContent);
    expect(valeurs).toEqual(['Jean Dupont', 'user1', 'jean.dupont@example.com', '+33601020304', 'CLIENT', 'Actif', 'Sophie Durand', '01/02/2024']);
    expect(screen.getByTestId('admin-panneau-client-depuis').textContent).toBe('01/02/2024');
    expect(document.querySelector('dialog')).toBeNull();
    expect(screen.getByTestId('admin-clients').compareDocumentPosition(panneau) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('[FR-016-09][BUG-055] un client sans conseiller n’a ni libellé ni valeur « Conseiller assigne »', async () => {
    const utilisateur = userEvent.setup();
    ouvrir();

    await utilisateur.click(await screen.findByTestId('admin-detail-3'));

    const panneau = screen.getByTestId('admin-panneau-detail');
    expect(within(panneau).queryByText('Conseiller assigne')).toBeNull();
    expect(screen.queryByTestId('admin-panneau-conseiller')).toBeNull();
    expect(screen.getByTestId('admin-panneau-telephone').textContent).toBe('');
    expect(screen.getByTestId('admin-panneau-client-depuis').textContent).toBe('20/11/2023');
  });

  it('[FR-016-09] Fermer ou Échap referme le panneau et rend le focus au bouton Detail déclencheur', async () => {
    const utilisateur = userEvent.setup();
    ouvrir();

    await utilisateur.click(await screen.findByTestId('admin-detail-1'));
    await utilisateur.click(screen.getByTestId('admin-fermer-detail'));
    expect(screen.queryByTestId('admin-panneau-detail')).toBeNull();
    expect(document.activeElement).toBe(screen.getByTestId('admin-detail-1'));

    await utilisateur.click(screen.getByTestId('admin-detail-2'));
    expect(screen.getByRole('heading', { level: 3 }).textContent).toBe('Detail : Marie Lebrun');
    await utilisateur.keyboard('{Escape}');
    expect(screen.queryByTestId('admin-panneau-detail')).toBeNull();
    expect(document.activeElement).toBe(screen.getByTestId('admin-detail-2'));

    await utilisateur.keyboard('{Escape}');
    expect(screen.queryByTestId('admin-panneau-detail')).toBeNull();
  });

  it('[FR-016-09] un nouveau filtrage ferme le panneau', async () => {
    const utilisateur = userEvent.setup();
    ouvrir();

    await utilisateur.click(await screen.findByTestId('admin-detail-1'));
    await utilisateur.click(screen.getByTestId('admin-filtrer'));

    expect(screen.queryByTestId('admin-panneau-detail')).toBeNull();
  });

  it('[FR-016-13] liste les 7 utilisateurs de tous rôles, rôle par son code et statut', async () => {
    ouvrir();

    const tableau = await screen.findByTestId('admin-utilisateurs');
    expect(within(tableau).getAllByRole('columnheader').map((th) => th.textContent)).toEqual(['Nom', 'Login', 'Email', 'Role', 'Statut']);
    const lignes = within(tableau).getAllByRole('row').slice(1);
    expect(lignes).toHaveLength(7);
    expect(within(lignes[0] as HTMLElement).getAllByRole('cell').map((cellule) => cellule.textContent)).toEqual([
      'Alice Martin',
      'admin',
      'admin@demobank.fr',
      'ADMIN',
      'Actif',
    ]);
    expect(screen.getByTestId('admin-utilisateur-role-12').textContent).toBe('CONSEILLER');
    expect(screen.getByTestId('admin-utilisateur-statut-4').textContent).toBe('Inactif');
  });

  it('[FR-016-13][FR-016-08] les sections suivent l’ordre figé et les libellés clés tiennent dans les 5 000 premiers caractères', async () => {
    ouvrir();

    await screen.findByTestId('admin-utilisateurs');
    await screen.findByText('Virements suspendus — En attente de revue');
    const texte = screen.getByTestId('application-racine').textContent ?? '';
    const indices = [
      'Total clients',
      'Pipeline Prets — En attente d approbation',
      'Gestion des clients',
      'Historique de tous les prets',
      'Tous les utilisateurs du systeme',
      'Virements suspendus — En attente de revue',
    ].map((libelle) => texte.indexOf(libelle));
    expect(indices.every((indice) => indice >= 0)).toBe(true);
    expect([...indices].sort((a, b) => a - b)).toEqual(indices);
    const debut = texte.slice(0, 5000);
    for (const libelle of ['Gestion des clients', 'Jean Dupont', 'user1']) {
      expect(debut, libelle).toContain(libelle);
    }
  });

  it('[FR-016-08] un 401 termine la session et ramène à la connexion', async () => {
    const { routeur } = ouvrir({ lireClients: () => Promise.resolve({ statut: 'nonAuthentifie' }) });

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    expect(localStorage.getItem('nb.jeton')).toBeNull();
  });

  it.each(['CLIENT', 'CONSEILLER'])('[FR-016-01] un %s qui ouvre /administration est redirigé sans appel d’administration', async (role) => {
    const { routeur, administration } = ouvrir({}, role);

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/tableau-de-bord'));
    expect(administration.lireIndicateursIdentite).not.toHaveBeenCalled();
    expect(administration.lireIndicateursPrets).not.toHaveBeenCalled();
    expect(administration.lireClients).not.toHaveBeenCalled();
    expect(administration.lireUtilisateurs).not.toHaveBeenCalled();
  });
});
