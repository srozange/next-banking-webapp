import { describe, expect, it, vi } from 'vitest';
import { agirSurVirementProgramme, champConnu, chargerVirementsProgrammes, creerVirementProgramme, type ClientVirementsProgrammes } from './gererVirementsProgrammes';
import { FORMULAIRE_VIREMENT_PROGRAMME_VIDE, type FormulaireVirementProgramme, type LigneVirementProgramme } from '../domain/virementsProgrammes';

const LIGNE: LigneVirementProgramme = {
  id: 1,
  compteSourceId: 1,
  numeroCompteSource: 'FR76-0001-0001',
  numeroBeneficiaire: 'FR76-0001-0002',
  libelleBeneficiaire: 'Epargne',
  montant: '50.00',
  frequence: 'MENSUEL',
  motif: null,
  prochaineExecution: '2027-01-01',
  actif: true,
  nbExecutions: 0,
};
const FORMULAIRE: FormulaireVirementProgramme = {
  ...FORMULAIRE_VIREMENT_PROGRAMME_VIDE,
  compteSourceId: '1',
  montant: ' 50 ',
  numeroBeneficiaire: ' FR76-0001-0002 ',
  libelleBeneficiaire: ' Epargne ',
  datePremiereExecution: ' 2027-01-01 ',
  motif: '  ',
};

function client(surcharge: Partial<ClientVirementsProgrammes> = {}): ClientVirementsProgrammes {
  return {
    lister: vi.fn(() => Promise.resolve({ statut: 'liste' as const, virements: [LIGNE] })),
    creer: vi.fn(() => Promise.resolve({ statut: 'cree' as const })),
    suspendre: vi.fn(() => Promise.resolve({ statut: 'ok' as const })),
    reactiver: vi.fn(() => Promise.resolve({ statut: 'ok' as const })),
    supprimer: vi.fn(() => Promise.resolve({ statut: 'ok' as const })),
    ...surcharge,
  };
}

describe('chargerVirementsProgrammes', () => {
  it('[FR-004-03] rend la liste du service', async () => {
    expect(await chargerVirementsProgrammes(client())).toEqual([LIGNE]);
  });

  it('[FR-004-03] rend null si le service est indisponible ou lève', async () => {
    expect(await chargerVirementsProgrammes(client({ lister: () => Promise.resolve({ statut: 'technique' }) }))).toBeNull();
    expect(await chargerVirementsProgrammes(client({ lister: () => Promise.reject(new Error('coupure')) }))).toBeNull();
  });
});

describe('creerVirementProgramme', () => {
  it('[FR-004-04] refuse la saisie invalide sans appeler le service', async () => {
    const c = client();

    expect(await creerVirementProgramme(FORMULAIRE_VIREMENT_PROGRAMME_VIDE, c)).toEqual({ statut: 'invalide', refus: 'compteSourceObligatoire' });
    expect(c.creer).not.toHaveBeenCalled();
  });

  it('[FR-004-07] envoie la saisie normalisée (montant à deux décimales, textes rognés, motif vide nul) puis recharge la liste', async () => {
    const c = client();

    expect(await creerVirementProgramme(FORMULAIRE, c)).toEqual({ statut: 'cree', liste: [LIGNE] });
    expect(c.creer).toHaveBeenCalledWith({
      compteSourceId: 1,
      numeroBeneficiaire: 'FR76-0001-0002',
      libelleBeneficiaire: 'Epargne',
      montant: '50.00',
      frequence: 'MENSUEL',
      motif: null,
      datePremiereExecution: '2027-01-01',
    });
    expect(c.lister).toHaveBeenCalledOnce();
  });

  it('[FR-004-07] garde un motif renseigné et signale une relecture en échec', async () => {
    const c = client({ lister: () => Promise.resolve({ statut: 'technique' }) });

    expect(await creerVirementProgramme({ ...FORMULAIRE, motif: ' loyer ' }, c)).toEqual({ statut: 'cree', liste: null });
    expect(c.creer).toHaveBeenCalledWith(expect.objectContaining({ motif: 'loyer' }));
  });

  it('[FR-004-06] rend le refus du service sans recharger', async () => {
    const refus = { statut: 'refuse' as const, message: 'Compte destinataire introuvable', champ: 'numeroBeneficiaire' as const };
    const c = client({ creer: () => Promise.resolve(refus) });

    expect(await creerVirementProgramme(FORMULAIRE, c)).toEqual(refus);
    expect(c.lister).not.toHaveBeenCalled();
  });

  it('[FR-004-07] une panne ou une exception est technique', async () => {
    expect(await creerVirementProgramme(FORMULAIRE, client({ creer: () => Promise.resolve({ statut: 'technique' }) }))).toEqual({ statut: 'technique' });
    expect(await creerVirementProgramme(FORMULAIRE, client({ creer: () => Promise.reject(new Error('coupure')) }))).toEqual({ statut: 'technique' });
  });
});

describe('champConnu', () => {
  it('[FR-004-04] reconnaît les champs du corps JSON et ignore le reste', () => {
    expect(champConnu('montant')).toBe('montant');
    expect(champConnu('inconnu')).toBeNull();
    expect(champConnu(undefined)).toBeNull();
  });
});

describe('agirSurVirementProgramme', () => {
  it.each(['suspendre', 'reactiver', 'supprimer'] as const)('[FR-004-09] %s appelle le service puis recharge la liste', async (action) => {
    const c = client();

    expect(await agirSurVirementProgramme(action, 1, c)).toEqual({ statut: 'ok', liste: [LIGNE] });
    expect(c[action]).toHaveBeenCalledWith(1);
    expect(c.lister).toHaveBeenCalledOnce();
  });

  it('[FR-004-09] une relecture en échec après un succès rend une liste nulle', async () => {
    const c = client({ lister: () => Promise.resolve({ statut: 'technique' }) });

    expect(await agirSurVirementProgramme('suspendre', 1, c)).toEqual({ statut: 'ok', liste: null });
  });

  it('[FR-004-10] introuvable et technique sont rendus tels quels, sans relecture', async () => {
    const introuvable = client({ supprimer: () => Promise.resolve({ statut: 'introuvable' }) });
    const technique = client({ reactiver: () => Promise.resolve({ statut: 'technique' }) });

    expect(await agirSurVirementProgramme('supprimer', 1, introuvable)).toEqual({ statut: 'introuvable' });
    expect(await agirSurVirementProgramme('reactiver', 1, technique)).toEqual({ statut: 'technique' });
    expect(introuvable.lister).not.toHaveBeenCalled();
    expect(technique.lister).not.toHaveBeenCalled();
  });

  it('[FR-004-09] une exception du port est une panne technique', async () => {
    const c = client({ suspendre: () => Promise.reject(new Error('coupure')) });

    expect(await agirSurVirementProgramme('suspendre', 1, c)).toEqual({ statut: 'technique' });
    expect(c.lister).not.toHaveBeenCalled();
  });
});
