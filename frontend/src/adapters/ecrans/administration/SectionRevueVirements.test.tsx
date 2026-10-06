// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ClientRevue, ReponseDecision } from '../../../application/revue';
import type { VirementEnRevue, VirementTraite } from '../../../domain/virement';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const VIREMENT: VirementEnRevue = {
  virementId: 12,
  clientId: 7,
  nomClient: 'Claire Martin',
  montant: '12000.00',
  compteDestinataire: 'FR76-0001-0002',
  motifSuspension: 'Score cumulé 130 - signaux : LARGE_AMOUNT, NEW_BENEFICIARY',
  heureButoir: '2026-11-06T09:15:00Z',
};
const AUTRE: VirementEnRevue = {
  ...VIREMENT,
  virementId: 13,
  nomClient: null,
  montant: '10000.00',
  motifSuspension: 'Montant supérieur ou égal au seuil anti-fraude (10000 EUR)',
  heureButoir: '2026-11-06T10:00:00Z',
};

function ouvrir(client: Partial<ClientRevue> = {}) {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['ADMIN'] }));
  return rendreApplication('/administration', undefined, undefined, undefined, undefined, undefined, {
    lireFile: () => Promise.resolve({ statut: 'liste', virements: [VIREMENT, AUTRE] }),
    ...client,
  });
}

const motif = () => screen.getByTestId('revue-motif') as HTMLInputElement;

describe('[FR-016-19] section de revue des virements suspendus', () => {
  it('liste la file sous son titre, avec ses colonnes, le nom du client, le motif et l’heure butoir', async () => {
    ouvrir();

    const file = await screen.findByTestId('revue-file');
    expect(screen.getByRole('heading', { name: 'Virements suspendus — En attente de revue' })).toBeTruthy();
    expect(Array.from(file.querySelectorAll('th')).map((th) => th.textContent)).toEqual([
      'Client',
      'Montant',
      'Compte destinataire',
      'Motif de suspension',
      'Heure butoir',
      'Actions',
    ]);
    const lignes = file.querySelectorAll('tbody tr');
    expect(lignes).toHaveLength(2);
    expect(Array.from(lignes[0]?.querySelectorAll('td') ?? []).slice(0, 5).map((td) => td.textContent)).toEqual([
      'Claire Martin',
      '12000.00',
      'FR76-0001-0002',
      'Score cumulé 130 - signaux : LARGE_AMOUNT, NEW_BENEFICIARY',
      '06/11/2026 10:15',
    ]);
    expect(lignes[1]?.querySelectorAll('td')[0]?.textContent).toBe('7');
    expect(screen.getByTestId('revue-liberer-12').textContent).toBe('Libérer');
    expect(screen.getByTestId('revue-rejeter-12').textContent).toBe('Rejeter');
    expect(screen.getByTestId('revue-liberer-13')).toBeTruthy();
    expect(screen.queryByTestId('revue-historique')).toBeNull();
  });

  it('sans virement suspendu : « Aucun virement suspendu », en-têtes conservés, aucun historique', async () => {
    ouvrir({ lireFile: () => Promise.resolve({ statut: 'liste', virements: [] }) });

    expect(await screen.findByText('Aucun virement suspendu')).toBeTruthy();
    expect(screen.getByTestId('revue-file').querySelectorAll('th')).toHaveLength(6);
    expect(screen.queryByText('Virements traités')).toBeNull();
  });

  it('file indisponible : message technique, sans bouton', async () => {
    ouvrir({ lireFile: () => Promise.resolve({ statut: 'technique' }) });

    expect((await screen.findByTestId('revue-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(screen.queryByTestId('revue-file')).toBeNull();
  });

  it('l’historique n’apparaît que s’il contient un virement, avec ses issues en clair', async () => {
    const traites: VirementTraite[] = [
      { virement: VIREMENT, issue: 'LIBERE', dateIssue: '2026-11-05T10:00:00Z' },
      { virement: { ...VIREMENT, virementId: 14 }, issue: 'REJETE', dateIssue: '2026-11-05T09:00:00Z' },
      { virement: { ...VIREMENT, virementId: 15 }, issue: 'REJETE_HEURE_BUTOIR', dateIssue: '2026-11-05T08:00:00Z' },
      { virement: { ...VIREMENT, virementId: 16, nomClient: null }, issue: 'REJETE_SOLDE_AU_DEBIT', dateIssue: '2026-11-05T07:00:00Z' },
    ];
    ouvrir({ lireHistorique: () => Promise.resolve({ statut: 'liste', virements: traites }) });

    const historique = await screen.findByTestId('revue-historique');
    expect(screen.getByRole('heading', { name: 'Virements traités' })).toBeTruthy();
    expect(Array.from(historique.querySelectorAll('th')).map((th) => th.textContent)).toEqual([
      'Client',
      'Montant',
      'Compte destinataire',
      'Motif de suspension',
      'Heure butoir',
      'Issue',
      "Date de l'issue",
    ]);
    expect(screen.getByTestId('revue-issue-12').textContent).toBe('Libéré');
    expect(screen.getByTestId('revue-issue-14').textContent).toBe('Rejeté');
    expect(screen.getByTestId('revue-issue-15').textContent).toBe('Rejeté - heure butoir dépassée');
    expect(screen.getByTestId('revue-issue-16').textContent).toBe('Rejeté : solde insuffisant au moment du débit');
    expect(historique.querySelectorAll('tbody tr')[0]?.textContent).toContain('05/11/2026 11:00');
  });

  it('Libérer ouvre la modale « Libérer le virement de … » avec piège du focus, Échap la ferme et rend le focus au bouton', async () => {
    const utilisateur = userEvent.setup();
    const { decider } = ouvrir();
    const bouton = await screen.findByTestId('revue-liberer-12');

    await utilisateur.click(bouton);

    const modale = screen.getByRole('dialog');
    expect(modale.getAttribute('aria-modal')).toBe('true');
    expect(within(modale).getByRole('heading', { name: 'Libérer le virement de 12000.00 EUR vers le compte FR76-0001-0002' })).toBeTruthy();
    expect(modale.getAttribute('aria-labelledby')).toBe('revue-modale-titre');
    expect(screen.getByLabelText('Motif de la décision')).toBe(motif());
    expect(document.activeElement).toBe(motif());
    expect(screen.getByTestId('revue-valider').textContent).toBe('Valider');
    expect(screen.getByTestId('revue-annuler').textContent).toBe('Annuler');

    await utilisateur.tab();
    expect(document.activeElement).toBe(screen.getByTestId('revue-valider'));
    await utilisateur.tab();
    expect(document.activeElement).toBe(screen.getByTestId('revue-annuler'));
    await utilisateur.tab();
    expect(document.activeElement).toBe(motif());
    await utilisateur.tab({ shift: true });
    expect(document.activeElement).toBe(screen.getByTestId('revue-annuler'));

    await utilisateur.keyboard('{Escape}');

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(document.activeElement).toBe(bouton);
    expect(decider).not.toHaveBeenCalled();
  });

  it('Rejeter ouvre la modale « Rejeter le virement de … », Annuler la ferme', async () => {
    const utilisateur = userEvent.setup();
    ouvrir();

    await utilisateur.click(await screen.findByTestId('revue-rejeter-13'));
    expect(screen.getByRole('heading', { name: 'Rejeter le virement de 10000.00 EUR vers le compte FR76-0001-0002' })).toBeTruthy();
    await utilisateur.click(screen.getByTestId('revue-annuler'));

    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('une touche autre qu’Échap ou Tab ne ferme pas la modale', async () => {
    const utilisateur = userEvent.setup();
    ouvrir();

    await utilisateur.click(await screen.findByTestId('revue-liberer-12'));
    await utilisateur.keyboard('abc');

    expect(motif().value).toBe('abc');
    expect(screen.getByRole('dialog')).toBeTruthy();
  });

  it('un motif vide ou d’espaces reste dans la modale : champ aria-invalid, « Veuillez indiquer un motif », aucun appel', async () => {
    const utilisateur = userEvent.setup();
    const { decider } = ouvrir();
    await utilisateur.click(await screen.findByTestId('revue-liberer-12'));

    await utilisateur.click(screen.getByTestId('revue-valider'));
    expect(motif().getAttribute('aria-invalid')).toBe('true');
    expect(screen.getByText('Veuillez indiquer un motif')).toBeTruthy();
    expect(document.activeElement).toBe(motif());

    await utilisateur.type(motif(), '   ');
    await utilisateur.click(screen.getByTestId('revue-valider'));

    expect(motif().getAttribute('aria-invalid')).toBe('true');
    expect(screen.getByRole('dialog')).toBeTruthy();
    expect(decider).not.toHaveBeenCalled();
  });

  it('un refus de motif du serveur s’affiche de même dans la modale', async () => {
    const utilisateur = userEvent.setup();
    ouvrir({ decider: () => Promise.resolve<ReponseDecision>({ statut: 'motif' }) });
    await utilisateur.click(await screen.findByTestId('revue-rejeter-12'));
    await utilisateur.type(motif(), 'client injoignable');

    await utilisateur.click(screen.getByTestId('revue-valider'));

    expect(motif().getAttribute('aria-invalid')).toBe('true');
    expect(screen.getByRole('dialog')).toBeTruthy();
  });

  it('une libération acceptée affiche son message, ferme la modale, focalise le message et recharge la file', async () => {
    const utilisateur = userEvent.setup();
    let file: VirementEnRevue[] = [VIREMENT, AUTRE];
    const { decider, lireFile } = ouvrir({
      lireFile: () => Promise.resolve({ statut: 'liste', virements: file }),
      decider: () => {
        file = [AUTRE];
        return Promise.resolve<ReponseDecision>({ statut: 'enregistre' });
      },
    });
    await utilisateur.click(await screen.findByTestId('revue-liberer-12'));
    await utilisateur.type(motif(), '  vérifié par téléphone ');

    await utilisateur.click(screen.getByTestId('revue-valider'));

    const message = await screen.findByTestId('revue-message');
    expect(message.textContent).toBe('Libération enregistrée : le virement est accepté, son débit va être effectué.');
    expect(message.getAttribute('role')).toBe('status');
    expect(document.activeElement).toBe(message);
    expect(decider).toHaveBeenCalledWith('liberer', 12, 'vérifié par téléphone');
    expect(screen.queryByRole('dialog')).toBeNull();
    await waitFor(() => expect(screen.queryByTestId('revue-liberer-12')).toBeNull());
    expect(lireFile).toHaveBeenCalledTimes(2);
    expect(screen.getByTestId('revue-liberer-13')).toBeTruthy();
  });

  it('un rejet accepté affiche « Rejet enregistré : le virement est rejeté sans débit. »', async () => {
    const utilisateur = userEvent.setup();
    const { decider } = ouvrir();
    await utilisateur.click(await screen.findByTestId('revue-rejeter-13'));
    await utilisateur.type(motif(), 'client injoignable');

    await utilisateur.click(screen.getByTestId('revue-valider'));

    expect((await screen.findByTestId('revue-message')).textContent).toBe('Rejet enregistré : le virement est rejeté sans débit.');
    expect(decider).toHaveBeenCalledWith('rejeter', 13, 'client injoignable');
  });

  it.each<[ReponseDecision, string]>([
    [{ statut: 'refuse', issue: 'REFUSEE_SOLDE_INSUFFISANT' }, 'Libération refusée : solde insuffisant. Le virement est rejeté, sans aucun débit.'],
    [{ statut: 'refuse', issue: 'REFUSEE_HEURE_BUTOIR' }, 'Heure butoir dépassée : le virement est rejeté.'],
    [{ statut: 'erreur', message: 'Accès refusé' }, 'Accès refusé'],
    [{ statut: 'erreur', message: null }, 'Une erreur technique est survenue'],
  ])('un refus du service %#, en alert-error', async (reponse, texte) => {
    const utilisateur = userEvent.setup();
    ouvrir({ decider: () => Promise.resolve(reponse) });
    await utilisateur.click(await screen.findByTestId('revue-liberer-12'));
    await utilisateur.type(motif(), 'vérifié');

    await utilisateur.click(screen.getByTestId('revue-valider'));

    const message = await screen.findByTestId('revue-message-erreur');
    expect(message.textContent).toBe(texte);
    expect(message.getAttribute('role')).toBe('alert');
    expect(document.activeElement).toBe(message);
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('une seule décision part à la fois : le bouton Valider est occupé pendant l’envoi', async () => {
    const utilisateur = userEvent.setup();
    let terminer: (reponse: ReponseDecision) => void = () => {};
    const { decider } = ouvrir({ decider: () => new Promise<ReponseDecision>((resolve) => (terminer = resolve)) });
    await utilisateur.click(await screen.findByTestId('revue-liberer-12'));
    await utilisateur.type(motif(), 'vérifié');

    await utilisateur.click(screen.getByTestId('revue-valider'));
    await utilisateur.click(screen.getByTestId('revue-valider'));

    expect((screen.getByTestId('revue-valider') as HTMLButtonElement).getAttribute('aria-busy')).toBe('true');
    expect(decider).toHaveBeenCalledOnce();
    terminer({ statut: 'enregistre' });
    await screen.findByTestId('revue-message');
  });
});
