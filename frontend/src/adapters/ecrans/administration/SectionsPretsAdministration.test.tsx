// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { ClientPipelinePrets, ReponseDecisionPret, ReponseListePrets } from '../../../application/pipelinePrets';
import type { PretAdministration } from '../../../domain/pret';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const PRET: PretAdministration = {
  id: 42,
  clientId: 1,
  nomClient: 'Jean Dupont',
  type: 'CONSOMMATION',
  montant: '10000.00',
  statut: 'EN_ATTENTE',
  dureeMois: 36,
  tauxAnnuel: '3.5000',
  mensualite: '293.02',
  objet: 'Voiture',
  dateDebut: '2026-10-01',
};
const SANS_TYPE: PretAdministration = { ...PRET, id: 43, nomClient: '2', clientId: 2, type: null, objet: null };
const EN_COURS: PretAdministration = { ...PRET, id: 41, statut: 'EN_COURS', dateDebut: '2026-08-01' };

const liste = (prets: readonly PretAdministration[]) => () => Promise.resolve<ReponseListePrets>({ statut: 'liste', prets });

function ouvrir(client: Partial<ClientPipelinePrets> = {}, role = 'ADMIN') {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles: [role] }));
  return rendreApplication(
    '/administration',
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
    { lireEnAttente: liste([PRET, SANS_TYPE]), lireHistorique: liste([PRET, EN_COURS]), ...client },
  );
}

describe('sections du pipeline et de l’historique des prêts', () => {
  it('[FR-016-03] liste le pipeline sous son titre, avec ses colonnes, le nom complet du client et les deux boutons', async () => {
    ouvrir();

    const pipeline = await screen.findByTestId('admin-pipeline');
    expect(screen.getByRole('heading', { name: 'Pipeline Prets — En attente d approbation' })).toBeTruthy();
    expect(Array.from(pipeline.querySelectorAll('th')).map((th) => th.textContent)).toEqual([
      'ID',
      'Client',
      'Type',
      'Montant',
      'Duree',
      'Taux',
      'Mensualite',
      'Motif',
      'Actions',
    ]);
    const lignes = pipeline.querySelectorAll('tbody tr');
    expect(lignes).toHaveLength(2);
    expect(Array.from(lignes[0]?.querySelectorAll('td') ?? []).slice(0, 8).map((td) => td.textContent)).toEqual([
      '42',
      'Jean Dupont',
      'Consommation',
      '10,000.00',
      '36 mois',
      '3.5000%',
      '293.02',
      'Voiture',
    ]);
    // Type et motif absents : cellules vides, repli du client sur son identifiant.
    expect(Array.from(lignes[1]?.querySelectorAll('td') ?? []).map((td) => td.textContent).slice(0, 3)).toEqual(['43', '2', '']);
    expect(screen.getByTestId('admin-approuver-42').textContent).toBe('Approuver');
    expect(screen.getByTestId('admin-refuser-42').textContent).toBe('Refuser');
    expect(screen.getByTestId('admin-approuver-43')).toBeTruthy();
  });

  it('[FR-016-03] sans prêt en attente : « Aucun pret en attente » sous les en-têtes conservés', async () => {
    ouvrir({ lireEnAttente: liste([]) });

    expect(await screen.findByText('Aucun pret en attente')).toBeTruthy();
    expect(screen.getByTestId('admin-pipeline').querySelectorAll('th')).toHaveLength(9);
  });

  it('[FR-016-03] pipeline indisponible : message technique, sans bouton, l’historique reste lisible', async () => {
    ouvrir({ lireEnAttente: () => Promise.resolve({ statut: 'technique' }) });

    expect((await screen.findByTestId('admin-pipeline-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(screen.queryByTestId('admin-pipeline')).toBeNull();
    expect(screen.getByTestId('admin-historique')).toBeTruthy();
  });

  it('[FR-016-12] l’historique liste tous les prêts avec ses colonnes, statuts en clair, en lecture seule', async () => {
    ouvrir();

    const historique = await screen.findByTestId('admin-historique');
    expect(screen.getByRole('heading', { name: 'Historique de tous les prets' })).toBeTruthy();
    expect(Array.from(historique.querySelectorAll('th')).map((th) => th.textContent)).toEqual([
      'ID',
      'Client',
      'Type',
      'Montant',
      'Statut',
      'Duree',
      'Mensualite',
      'Date debut',
    ]);
    const lignes = historique.querySelectorAll('tbody tr');
    expect(Array.from(lignes[1]?.querySelectorAll('td') ?? []).map((td) => td.textContent)).toEqual([
      '41',
      'Jean Dupont',
      'Consommation',
      '10,000.00',
      'En cours',
      '36 mois',
      '293.02',
      '01/08/2026',
    ]);
    expect(historique.querySelectorAll('button')).toHaveLength(0);
  });

  it('[FR-016-12] sans prêt en base : « Aucun pret en base » ; historique indisponible : message technique', async () => {
    ouvrir({ lireHistorique: liste([]) });
    expect(await screen.findByText('Aucun pret en base')).toBeTruthy();
    cleanup();

    ouvrir({ lireHistorique: () => Promise.reject(new Error('réseau')) });
    expect((await screen.findByTestId('admin-historique-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(screen.queryByTestId('admin-historique')).toBeNull();
  });

  it('[FR-016-03][FR-016-12] les deux sections précèdent la file de revue des virements suspendus', async () => {
    ouvrir();

    await screen.findByTestId('admin-pipeline');
    const ordre = ['admin-pipeline-section', 'admin-historique-section', 'revue-section'].map((id) => screen.getByTestId(id));
    expect(ordre[0]?.compareDocumentPosition(ordre[1] as Node)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
    expect(ordre[1]?.compareDocumentPosition(ordre[2] as Node)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
  });

  it('[FR-016-04] « Approuver » agit sans confirmation, affiche « Prêt approuvé » et relit le pipeline et l’historique', async () => {
    const decider = vi.fn((): Promise<ReponseDecisionPret> => Promise.resolve({ statut: 'effectuee' }));
    const lireEnAttente = vi.fn(liste([PRET]));
    const lireHistorique = vi.fn(liste([PRET]));
    const utilisateur = userEvent.setup();
    ouvrir({ decider, lireEnAttente, lireHistorique });

    await utilisateur.click(await screen.findByTestId('admin-approuver-42'));

    expect((await screen.findByTestId('admin-message-succes')).textContent).toBe('Prêt approuvé');
    expect(decider).toHaveBeenCalledWith('approuver', 42);
    expect(screen.queryByRole('dialog')).toBeNull();
    await waitFor(() => expect(lireEnAttente).toHaveBeenCalledTimes(2));
    expect(lireHistorique).toHaveBeenCalledTimes(2);
    expect(screen.queryByTestId('admin-message-erreur')).toBeNull();
  });

  it('[FR-016-05] « Refuser » agit sans confirmation et affiche « Prêt refusé »', async () => {
    const decider = vi.fn((): Promise<ReponseDecisionPret> => Promise.resolve({ statut: 'effectuee' }));
    const utilisateur = userEvent.setup();
    ouvrir({ decider });

    await utilisateur.click(await screen.findByTestId('admin-refuser-43'));

    expect((await screen.findByTestId('admin-message-succes')).textContent).toBe('Prêt refusé');
    expect(decider).toHaveBeenCalledWith('refuser', 43);
  });

  it('[FR-016-07] toute erreur de décision affiche le texte fixe dans admin-message-erreur, sans détail', async () => {
    const utilisateur = userEvent.setup();
    const decider = vi
      .fn<(action: string, pretId: number) => Promise<ReponseDecisionPret>>()
      .mockResolvedValueOnce({ statut: 'technique' })
      .mockRejectedValueOnce(new Error('NullPointerException'));
    ouvrir({ decider });

    await utilisateur.click(await screen.findByTestId('admin-approuver-42'));
    const premiere = await screen.findByTestId('admin-message-erreur');
    expect(premiere.textContent).toBe('Erreur lors du traitement du prêt. Veuillez réessayer.');
    expect(screen.queryByTestId('admin-message-succes')).toBeNull();

    await utilisateur.click(screen.getByTestId('admin-refuser-42'));
    await waitFor(() => expect(decider).toHaveBeenCalledTimes(2));
    expect((await screen.findByTestId('admin-message-erreur')).textContent).toBe('Erreur lors du traitement du prêt. Veuillez réessayer.');
    expect(document.body.textContent).not.toContain('NullPointerException');
  });

  it('[FR-016-04][FR-016-07] le succès remplace l’erreur précédente et le focus va au message', async () => {
    const utilisateur = userEvent.setup();
    const decider = vi
      .fn<(action: string, pretId: number) => Promise<ReponseDecisionPret>>()
      .mockResolvedValueOnce({ statut: 'technique' })
      .mockResolvedValueOnce({ statut: 'effectuee' });
    ouvrir({ decider });

    await utilisateur.click(await screen.findByTestId('admin-approuver-42'));
    await screen.findByTestId('admin-message-erreur');
    await utilisateur.click(screen.getByTestId('admin-approuver-42'));

    const succes = await screen.findByTestId('admin-message-succes');
    expect(screen.queryByTestId('admin-message-erreur')).toBeNull();
    await waitFor(() => expect(document.activeElement).toBe(succes));
  });

  it('[FR-016-04] les boutons sont désactivés pendant la décision', async () => {
    let terminer: (reponse: ReponseDecisionPret) => void = () => undefined;
    const decider = vi.fn(() => new Promise<ReponseDecisionPret>((resolve) => (terminer = resolve)));
    const utilisateur = userEvent.setup();
    ouvrir({ decider });

    await utilisateur.click(await screen.findByTestId('admin-approuver-42'));

    await waitFor(() => expect((screen.getByTestId('admin-refuser-42') as HTMLButtonElement).disabled).toBe(true));
    terminer({ statut: 'effectuee' });
    await screen.findByTestId('admin-message-succes');
    await waitFor(() => expect((screen.getByTestId('admin-refuser-42') as HTMLButtonElement).disabled).toBe(false));
  });

  it('[FR-016-04] l’écran garde ses boutons pour tout rôle : le serveur décide, et son refus est le texte fixe', async () => {
    const utilisateur = userEvent.setup();
    ouvrir({ decider: () => Promise.resolve({ statut: 'technique' }) }, 'ADMIN');

    await utilisateur.click(await screen.findByTestId('admin-approuver-42'));

    expect(within(await screen.findByTestId('admin-message-erreur')).getByText('Erreur lors du traitement du prêt. Veuillez réessayer.')).toBeTruthy();
  });
});
