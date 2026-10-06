import { describe, expect, it, vi } from 'vitest';
import type { Catalogue, Traitement } from '../domain/produits';
import { chargerProduits, type ClientProduits, type ReponseProduits } from './consulterProduits';
import type { StockageJeton } from './seConnecter';

function stockageEnMemoire(initial: string | null = 'jeton') {
  let jeton = initial;
  return {
    lire: vi.fn(() => jeton),
    enregistrer: vi.fn((valeur: string) => {
      jeton = valeur;
    }),
    supprimer: vi.fn(() => {
      jeton = null;
    }),
  } satisfies StockageJeton;
}

const CATALOGUE: Catalogue = { epargne: [], credit: [], investissement: [] };
const TRAITEMENT: Traitement = {
  job: 'CLOTURE', debut: null, fin: null, dureeMs: 0, enregistrements: null, statut: null, details: null,
};

function client(
  catalogue: ReponseProduits<Catalogue> = { statut: 'ok', donnees: CATALOGUE },
  traitements: ReponseProduits<readonly Traitement[]> = { statut: 'ok', donnees: [TRAITEMENT] },
) {
  return {
    lireCatalogue: vi.fn<ClientProduits['lireCatalogue']>(() => Promise.resolve(catalogue)),
    lireTraitements: vi.fn<ClientProduits['lireTraitements']>(() => Promise.resolve(traitements)),
  };
}

describe('chargerProduits', () => {
  it('[FR-011-02] rend le catalogue et le journal', async () => {
    const service = client();

    await expect(chargerProduits(service, stockageEnMemoire())).resolves.toEqual({
      type: 'etat',
      etat: { catalogue: CATALOGUE, traitements: [TRAITEMENT], technique: false },
    });
    expect(service.lireCatalogue).toHaveBeenCalledWith('jeton');
    expect(service.lireTraitements).toHaveBeenCalledWith('jeton');
  });

  it('[FR-011-02] demande les deux services en parallèle, sans attendre le premier', async () => {
    const service = client();
    let liberer: (reponse: ReponseProduits<Catalogue>) => void = () => {};
    service.lireCatalogue.mockImplementation(() => new Promise((resolu) => (liberer = resolu)));

    const resultat = chargerProduits(service, stockageEnMemoire());
    await vi.waitFor(() => expect(service.lireTraitements).toHaveBeenCalledTimes(1));
    expect(service.lireCatalogue).toHaveBeenCalledTimes(1);
    liberer({ statut: 'ok', donnees: CATALOGUE });

    await expect(resultat).resolves.toMatchObject({ type: 'etat' });
  });

  it('[FR-010-01] sans jeton : session terminée, aucun appel', async () => {
    const service = client();
    const stockage = stockageEnMemoire(null);

    await expect(chargerProduits(service, stockage)).resolves.toEqual({ type: 'deconnecte' });
    expect(stockage.supprimer).toHaveBeenCalled();
    expect(service.lireCatalogue).not.toHaveBeenCalled();
    expect(service.lireTraitements).not.toHaveBeenCalled();
  });

  it.each([
    ['le catalogue', client({ statut: 'nonAuthentifie' })],
    ['le journal', client(undefined, { statut: 'nonAuthentifie' })],
  ])('[FR-010-01] un 401 de %s supprime le jeton et déconnecte', async (_nom, service) => {
    const stockage = stockageEnMemoire();

    await expect(chargerProduits(service, stockage)).resolves.toEqual({ type: 'deconnecte' });
    expect(stockage.supprimer).toHaveBeenCalled();
  });

  it('[FR-011-02] le catalogue en panne : le journal reste affiché, la panne est signalée', async () => {
    await expect(chargerProduits(client({ statut: 'technique' }), stockageEnMemoire())).resolves.toEqual({
      type: 'etat',
      etat: { catalogue: null, traitements: [TRAITEMENT], technique: true },
    });
  });

  it('[FR-011-02] le journal en panne : le catalogue reste affiché, la panne est signalée', async () => {
    await expect(chargerProduits(client(undefined, { statut: 'technique' }), stockageEnMemoire())).resolves.toEqual({
      type: 'etat',
      etat: { catalogue: CATALOGUE, traitements: null, technique: true },
    });
  });

  it('[FR-011-02] les deux en panne : rien à afficher, panne signalée', async () => {
    await expect(
      chargerProduits(client({ statut: 'technique' }, { statut: 'technique' }), stockageEnMemoire()),
    ).resolves.toEqual({ type: 'etat', etat: { catalogue: null, traitements: null, technique: true } });
  });
});
