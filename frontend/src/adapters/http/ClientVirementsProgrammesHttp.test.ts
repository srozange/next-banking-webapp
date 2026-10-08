import { describe, expect, it, vi } from 'vitest';
import type { CommandeVirementProgramme } from '../../application/gererVirementsProgrammes';
import { creerFetchAuthentifie } from '../session/fetchAuthentifie';
import { creerSignalExpiration } from '../session/signalExpiration';
import type { StockageJeton } from '../stockage/StockageJetonLocal';
import { creerClientVirementsProgrammesHttp } from './ClientVirementsProgrammesHttp';

const json = (corps: unknown, status: number) => Promise.resolve(new Response(JSON.stringify(corps), { status }));
const virement = {
  id: 3,
  compteSourceId: 1,
  numeroCompteSource: 'FR76-0001-0001',
  numeroBeneficiaire: 'FR76-0001-0002',
  libelleBeneficiaire: 'Epargne',
  montant: '50.00',
  frequence: 'MENSUEL',
  motif: 'loyer',
  prochaineExecution: '2027-01-01',
  actif: true,
  nbExecutions: 0,
};
const COMMANDE: CommandeVirementProgramme = {
  compteSourceId: 1,
  numeroBeneficiaire: 'FR76-0001-0002',
  libelleBeneficiaire: 'Epargne',
  montant: '50.00',
  frequence: 'MENSUEL',
  motif: null,
  datePremiereExecution: '2027-01-01',
};
const http = (reponse: () => Promise<Response>) => creerClientVirementsProgrammesHttp(reponse as unknown as typeof fetch);

describe('ClientVirementsProgrammesHttp.lister', () => {
  it('[FR-004-03] lit les virements du client', async () => {
    const appeler = vi.fn((_e: unknown, _o?: unknown) => json([virement], 200));
    const client = creerClientVirementsProgrammesHttp(appeler as unknown as typeof fetch);

    expect(await client.lister()).toEqual({ statut: 'liste', virements: [virement] });
    expect(appeler).toHaveBeenCalledWith('/api/coeur-bancaire/virements-programmes', expect.objectContaining({ method: 'GET' }));
  });

  it('[FR-004-03] un motif absent devient nul et une liste vide est acceptée', async () => {
    const { motif: _motif, ...sans } = virement;

    expect(await http(() => json([sans], 200)).lister()).toEqual({ statut: 'liste', virements: [{ ...sans, motif: null }] });
    expect(await http(() => json([], 200)).lister()).toEqual({ statut: 'liste', virements: [] });
  });

  it.each([
    ['un 500', () => json({}, 500)],
    ['un corps qui n’est pas une liste', () => json({ a: 1 }, 200)],
    ['un élément nul', () => json([null], 200)],
    ['un identifiant texte', () => json([{ ...virement, id: '3' }], 200)],
    ['un compte source texte', () => json([{ ...virement, compteSourceId: '1' }], 200)],
    ['un numéro de compte source manquant', () => json([{ ...virement, numeroCompteSource: undefined }], 200)],
    ['un numéro de bénéficiaire manquant', () => json([{ ...virement, numeroBeneficiaire: 1 }], 200)],
    ['un libellé manquant', () => json([{ ...virement, libelleBeneficiaire: 1 }], 200)],
    ['un montant numérique', () => json([{ ...virement, montant: 50 }], 200)],
    ['une fréquence manquante', () => json([{ ...virement, frequence: 1 }], 200)],
    ['un motif numérique', () => json([{ ...virement, motif: 1 }], 200)],
    ['une date manquante', () => json([{ ...virement, prochaineExecution: null }], 200)],
    ['un état texte', () => json([{ ...virement, actif: 'oui' }], 200)],
    ['un nombre d’exécutions texte', () => json([{ ...virement, nbExecutions: '0' }], 200)],
    ['un corps illisible', () => Promise.resolve(new Response('pas du json', { status: 200 }))],
    ['une coupure', () => Promise.reject(new Error('coupure'))],
  ])('[FR-004-03] %s est une panne technique', async (_nom, reponse) => {
    expect(await http(reponse).lister()).toEqual({ statut: 'technique' });
  });
});

describe('ClientVirementsProgrammesHttp.creer', () => {
  it('[FR-004-07] envoie la commande en JSON et reconnaît le 201', async () => {
    const appeler = vi.fn((_e: unknown, _o?: unknown) => json(virement, 201));
    const client = creerClientVirementsProgrammesHttp(appeler as unknown as typeof fetch);

    expect(await client.creer(COMMANDE)).toEqual({ statut: 'cree' });
    expect(appeler).toHaveBeenCalledWith(
      '/api/coeur-bancaire/virements-programmes',
      expect.objectContaining({ method: 'POST', body: JSON.stringify(COMMANDE) }),
    );
  });

  it.each([400, 403, 404])('[FR-004-06] un %s avec son message est un refus, champ reconnu ou nul', async (statut) => {
    expect(await http(() => json({ message: 'Montant invalide', champ: 'montant' }, statut)).creer(COMMANDE)).toEqual({
      statut: 'refuse',
      message: 'Montant invalide',
      champ: 'montant',
    });
    expect(await http(() => json({ message: 'Requête invalide' }, statut)).creer(COMMANDE)).toEqual({
      statut: 'refuse',
      message: 'Requête invalide',
      champ: null,
    });
  });

  it.each([
    ['un 500', () => json({ message: 'x' }, 500)],
    ['un 409', () => json({ message: 'x' }, 409)],
    ['un refus sans message', () => json({}, 400)],
    ['un refus nul', () => json(null, 400)],
    ['un refus illisible', () => Promise.resolve(new Response('pas du json', { status: 400 }))],
    ['une coupure', () => Promise.reject(new Error('coupure'))],
  ])('[FR-004-07] %s est une panne technique', async (_nom, reponse) => {
    expect(await http(reponse).creer(COMMANDE)).toEqual({ statut: 'technique' });
  });

  it('[FR-004-01] un 401 supprime le jeton et signale la fin de session (règle de fetchAuthentifie, non dupliquée)', async () => {
    const stockage: StockageJeton = { lire: () => 'j', enregistrer: vi.fn(), supprimer: vi.fn() };
    const signal = creerSignalExpiration();
    const rappel = vi.fn();
    signal.ecouter(rappel);
    const client = creerClientVirementsProgrammesHttp(creerFetchAuthentifie(stockage, signal, () => json({ message: 'Session expirée, veuillez vous reconnecter.' }, 401)));

    expect(await client.creer(COMMANDE)).toEqual({ statut: 'technique' });
    expect(await client.lister()).toEqual({ statut: 'technique' });
    expect(stockage.supprimer).toHaveBeenCalledTimes(2);
    expect(rappel).toHaveBeenCalledTimes(2);
  });
});
