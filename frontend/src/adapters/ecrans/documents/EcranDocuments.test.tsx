// @vitest-environment jsdom
import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ClientDocuments, ReponsePageDocuments, ReponseTelechargement } from '../../../application/consulterDocuments';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';
import { EcranDocuments, ouvrirPdfDansUnNouvelOnglet } from './EcranDocuments';

const COMPTE_1 = { compteId: 1, numero: 'FR76-0001-0001', type: 'COURANT' };
const COMPTE_2 = { compteId: 2, numero: 'FR76-0001-0002', type: 'EPARGNE' };

const page = (aujourdhui: string, comptes = [COMPTE_1]) => (): Promise<ReponsePageDocuments> =>
  Promise.resolve({ statut: 'page', aujourdhui, comptes });
const pdf = (): Promise<ReponseTelechargement> => Promise.resolve({ statut: 'pdf', contenu: new Blob(['%PDF']) });

let ouvertures: Blob[];

beforeEach(() => {
  ouvertures = [];
  URL.createObjectURL = vi.fn(() => 'blob:pdf');
  URL.revokeObjectURL = vi.fn();
  vi.spyOn(window, 'open').mockImplementation(() => null);
});

afterEach(() => {
  cleanup();
  localStorage.clear();
  vi.restoreAllMocks();
  vi.useRealTimers();
});

function ouvrir(client: Partial<ClientDocuments>, roles = ['CLIENT']) {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles }));
  return rendreApplication('/documents', undefined, undefined, undefined, undefined, undefined, undefined, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, client);
}

describe('écran Documents', () => {
  it.each(['/documents', '/mon-conseiller', '/espace-conseiller', '/administration'])(
    '[FR-030-06][FR-008-02] sans jeton, %s mène à /login sans afficher l’écran',
    async (chemin) => {
      const { routeur, lirePageDocuments } = rendreApplication(chemin);

      await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
      expect(screen.queryByTestId('documents-ecran')).toBeNull();
      expect(lirePageDocuments).not.toHaveBeenCalled();
    },
  );

  it.each([['CLIENT'], ['CONSEILLER'], ['ADMIN']])('[FR-008-01] %s : titre d’onglet et même contenu', async (role) => {
    ouvrir({ lirePage: page('2026-11-17') }, [role]);

    await screen.findByTestId('documents-rib-1');
    expect(document.title).toBe('BankApp Demo - Documents');
    expect(screen.getByTestId('documents-section-compte-1').textContent).toContain('Compte COURANT - FR76-0001-0001');
    expect(screen.getAllByRole('link', { name: /Telecharger PDF/ }).length).toBeGreaterThan(0);
  });

  it('[FR-008-03] affiche une section par compte du connecté avec RIB, attestation et liste des relevés', async () => {
    ouvrir({ lirePage: page('2026-11-17', [COMPTE_1, COMPTE_2]) });
    await screen.findByTestId('documents-section-compte-2');

    const section = within(screen.getByTestId('documents-section-compte-1'));
    expect(section.getByText('Compte COURANT - FR76-0001-0001')).toBeTruthy();
    expect(section.getByTestId('documents-etiquette-rib-1').textContent).toBe('Bancaire');
    expect(section.getByRole('heading', { name: 'RIB' })).toBeTruthy();
    expect(section.getByText('Releve d Identite Bancaire')).toBeTruthy();
    expect(section.getByTestId('documents-etiquette-attestation-1').textContent).toBe('Attestation');
    expect(section.getByRole('heading', { name: 'Attestation de solde' })).toBeTruthy();
    expect(section.getByText('Solde au jour de la demande')).toBeTruthy();
    expect(section.getByRole('heading', { name: 'Releves mensuels — FR76-0001-0001' })).toBeTruthy();
    expect(within(screen.getByTestId('documents-section-compte-2')).getByText('Compte EPARGNE - FR76-0001-0002')).toBeTruthy();
    expect(screen.getByTestId('documents-rib-2')).toBeTruthy();
    expect(screen.getByTestId('documents-attestation-2')).toBeTruthy();
  });

  it('[FR-008-05] aujourdhui = 2026-11-17 : 12 relevés de Novembre 2026 à Décembre 2025, IFU 2025 et 2024', async () => {
    ouvrir({ lirePage: page('2026-11-17') });
    await screen.findByTestId('documents-releve-1-2026-11');

    const releves = within(screen.getByTestId('documents-section-compte-1'))
      .getAllByRole('listitem')
      .map((element) => element.querySelector('a')?.getAttribute('data-testid'));
    expect(releves).toHaveLength(12);
    expect(releves[0]).toBe('documents-releve-1-2026-11');
    expect(releves[11]).toBe('documents-releve-1-2025-12');
    expect(screen.getByTestId('documents-releve-1-2026-11').textContent).toBe('Novembre 2026 (nouvel onglet)');
    expect(screen.getByTestId('documents-releve-1-2025-12').textContent).toBe('Décembre 2025 (nouvel onglet)');
    const ifu = within(screen.getByTestId('documents-section-ifu'));
    expect(ifu.getByText('Documents fiscaux')).toBeTruthy();
    expect(ifu.getByRole('heading', { name: 'Imprime Fiscal Unique (IFU)' })).toBeTruthy();
    expect(ifu.getByRole('heading', { name: 'IFU 2025' })).toBeTruthy();
    expect(ifu.getByRole('heading', { name: 'IFU 2024' })).toBeTruthy();
    expect(ifu.getAllByText('Revenus de capitaux mobiliers')).toHaveLength(2);
    expect(screen.getByTestId('documents-etiquette-ifu-2025').textContent).toBe('Fiscal');
  });

  it('[FR-008-05] aujourdhui = 2026-02-05 : relevés de Février 2026 à Mars 2025', async () => {
    ouvrir({ lirePage: page('2026-02-05') });
    await screen.findByTestId('documents-releve-1-2026-02');

    expect(screen.getByTestId('documents-releve-1-2025-03').textContent).toBe('Mars 2025 (nouvel onglet)');
    expect(screen.queryByTestId('documents-releve-1-2025-02')).toBeNull();
    expect(screen.getByTestId('documents-ifu-2025')).toBeTruthy();
    expect(screen.getByTestId('documents-ifu-2024')).toBeTruthy();
  });

  it('[FR-008-01] sans compte : pas de section compte, la section IFU reste', async () => {
    ouvrir({ lirePage: page('2026-11-17', []) });
    await screen.findByTestId('documents-ifu-2025');

    expect(screen.queryByTestId('documents-section-compte-1')).toBeNull();
    expect(screen.queryByTestId('documents-message-erreur')).toBeNull();
  });

  it('[FR-008-01] le DOM garde l’ordre du legacy : documents fiscaux, IFU, RIB, attestation, relevés', async () => {
    ouvrir({ lirePage: page('2026-11-17') });
    await screen.findByTestId('documents-rib-1');

    const texte = screen.getByTestId('documents-ecran').textContent ?? '';
    const positions = ['Documents fiscaux', 'Imprime Fiscal Unique (IFU)', 'RIB', 'Attestation de solde', 'Releves mensuels'].map((mot) => texte.indexOf(mot));
    expect(positions.every((position) => position >= 0)).toBe(true);
    expect([...positions].sort((a, b) => a - b)).toEqual(positions);
  });

  it('[FR-008-01] page indisponible : texte technique, aucune section', async () => {
    ouvrir({ lirePage: () => Promise.resolve({ statut: 'technique' }) });

    expect((await screen.findByTestId('documents-message-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(screen.queryByTestId('documents-section-ifu')).toBeNull();
    expect(screen.queryByTestId('documents-section-compte-1')).toBeNull();
  });

  it.each([
    ['documents-rib-1', { type: 'RIB', compteId: 1 }],
    ['documents-attestation-1', { type: 'ATTESTATION', compteId: 1 }],
    ['documents-releve-1-2026-03', { type: 'RELEVE', compteId: 1, annee: 2026, mois: 3 }],
    ['documents-ifu-2025', { type: 'IFU', annee: 2025 }],
  ])('[FR-008-06] le lien %s demande le document et ouvre le PDF dans un nouvel onglet', async (testId, demande) => {
    const { telechargerDocumentSimule } = ouvrir({ lirePage: page('2026-11-17'), telecharger: pdf });
    const lien = await screen.findByTestId(testId);

    await userEvent.setup().click(lien);

    await waitFor(() => expect(window.open).toHaveBeenCalledWith('blob:pdf', '_blank'));
    expect(telechargerDocumentSimule).toHaveBeenCalledWith(demande);
    expect(screen.queryByTestId('documents-message-erreur')).toBeNull();
  });

  it('[FR-008-06] le nom accessible du lien contient « Telecharger PDF » suivi de « (nouvel onglet) »', async () => {
    ouvrir({ lirePage: page('2026-11-17') });
    const lien = await screen.findByTestId('documents-rib-1');

    expect(lien.textContent).toBe('Telecharger PDF (nouvel onglet)');
    expect(screen.getAllByRole('link', { name: 'Telecharger PDF (nouvel onglet)' })).toHaveLength(4);
  });

  it('[FR-008-06] une réponse autre que le PDF affiche le texte technique, sans onglet, puis un clic réussi l’efface', async () => {
    let appels = 0;
    ouvrir({ lirePage: page('2026-11-17'), telecharger: () => (++appels === 1 ? Promise.resolve({ statut: 'technique' }) : pdf()) });
    const lien = await screen.findByTestId('documents-rib-1');
    const utilisateur = userEvent.setup();

    await utilisateur.click(lien);

    expect((await screen.findByTestId('documents-message-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(window.open).not.toHaveBeenCalled();

    await utilisateur.click(lien);

    await waitFor(() => expect(window.open).toHaveBeenCalledTimes(1));
    expect(screen.queryByTestId('documents-message-erreur')).toBeNull();
  });

  it('[FR-008-06] un 401 au clic ramène à /login, jeton supprimé, sans ouvrir d’onglet', async () => {
    let rendu: ReturnType<typeof ouvrir> | undefined;
    rendu = ouvrir({
      lirePage: page('2026-11-17'),
      telecharger: () => {
        localStorage.removeItem('nb.jeton');
        rendu?.signal.declencher();
        return Promise.resolve({ statut: 'technique' });
      },
    });
    const lien = await screen.findByTestId('documents-ifu-2025');

    await userEvent.setup().click(lien);

    await waitFor(() => expect(rendu?.routeur.state.location.pathname).toBe('/login'));
    expect(localStorage.getItem('nb.jeton')).toBeNull();
    expect(window.open).not.toHaveBeenCalled();
  });

  it('[FR-008-06] ne stocke rien dans localStorage ni sessionStorage hors le jeton', async () => {
    ouvrir({ lirePage: page('2026-11-17'), telecharger: pdf });
    await userEvent.setup().click(await screen.findByTestId('documents-rib-1'));
    await waitFor(() => expect(window.open).toHaveBeenCalled());

    expect(Object.keys(localStorage)).toEqual(['nb.jeton']);
    expect(sessionStorage.length).toBe(0);
  });

  it('[FR-008-06] ouvre le PDF par la fonction substituable ouvrirPdf', async () => {
    const ouvrirPdf = vi.fn((contenu: Blob) => ouvertures.push(contenu));
    const client: ClientDocuments = { lirePage: page('2026-11-17'), telecharger: pdf };
    render(<EcranDocuments client={client} ouvrirPdf={ouvrirPdf} />);

    await userEvent.setup().click(await screen.findByTestId('documents-rib-1'));

    await waitFor(() => expect(ouvertures).toHaveLength(1));
    expect(window.open).not.toHaveBeenCalled();
  });

  it('[FR-008-06] ouvrirPdfDansUnNouvelOnglet ouvre une URL d’objet et la libère après un délai', () => {
    vi.useFakeTimers();

    ouvrirPdfDansUnNouvelOnglet(new Blob(['%PDF']));

    expect(window.open).toHaveBeenCalledWith('blob:pdf', '_blank');
    expect(URL.revokeObjectURL).not.toHaveBeenCalled();
    vi.advanceTimersByTime(60_000);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:pdf');
  });
});
