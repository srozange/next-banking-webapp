import { describe, expect, it, vi } from 'vitest';
import { FORMULAIRE_BENEFICIAIRE_VIDE, type BeneficiaireBrut } from '../domain/beneficiaires';
import {
  ajouterBeneficiaire,
  chargerBeneficiaires,
  supprimerBeneficiaire,
  verifierIban,
  type ClientBeneficiaires,
  type ReponseAjout,
  type ReponseBeneficiaires,
  type ReponseSuppression,
  type ReponseVerificationIban,
} from './gererBeneficiaires';

const BRUT: BeneficiaireBrut = { id: 1, libelle: 'Loyer', titulaire: 'Marie', iban: 'FR76***0189', bic: ' ', banque: null, zoneSepa: true };
const LIGNE = { ...BRUT, bic: null };

const client = (
  lister: () => Promise<ReponseBeneficiaires>,
  supprimer: () => Promise<ReponseSuppression> = () => Promise.resolve({ statut: 'supprime' }),
  verifier: () => Promise<ReponseVerificationIban> = () => Promise.resolve({ statut: 'technique' }),
  ajouter: () => Promise<ReponseAjout> = () => Promise.resolve({ statut: 'technique' }),
) => ({ lister: vi.fn(lister), supprimer: vi.fn(supprimer), verifierIban: vi.fn(verifier), ajouter: vi.fn(ajouter) }) satisfies ClientBeneficiaires;
const liste = (...beneficiaires: BeneficiaireBrut[]) => () => Promise.resolve<ReponseBeneficiaires>({ statut: 'liste', beneficiaires });

describe('chargerBeneficiaires', () => {
  it('[FR-013-02] rend les lignes dans l’ordre du service, valeurs absentes normalisées', async () => {
    expect(await chargerBeneficiaires(client(liste(BRUT)))).toEqual([LIGNE]);
  });

  it('[FR-013-02] rend null quand le service est indisponible', async () => {
    expect(await chargerBeneficiaires(client(() => Promise.resolve({ statut: 'technique' })))).toBeNull();
    expect(await chargerBeneficiaires(client(() => Promise.reject(new Error('réseau'))))).toBeNull();
  });
});

describe('supprimerBeneficiaire', () => {
  it('[FR-013-09] supprime puis recharge la liste', async () => {
    const ordre: string[] = [];
    const cl = client(
      () => {
        ordre.push('liste');
        return Promise.resolve({ statut: 'liste', beneficiaires: [] });
      },
      () => {
        ordre.push('supprimer');
        return Promise.resolve({ statut: 'supprime' });
      },
    );

    expect(await supprimerBeneficiaire(7, cl)).toEqual({ statut: 'supprime', liste: [] });
    expect(cl.supprimer).toHaveBeenCalledWith(7);
    expect(ordre).toEqual(['supprimer', 'liste']);
  });

  it('[FR-013-10] un refus recharge aussi la liste', async () => {
    expect(await supprimerBeneficiaire(7, client(liste(BRUT), () => Promise.resolve({ statut: 'refuse' })))).toEqual({ statut: 'refuse', liste: [LIGNE] });
  });

  it('[FR-013-09] une relecture en échec après suppression rend une liste nulle', async () => {
    const cl = client(() => Promise.resolve({ statut: 'technique' }));

    expect(await supprimerBeneficiaire(7, cl)).toEqual({ statut: 'supprime', liste: null });
  });

  it('[FR-013-09] un échec technique ne recharge pas la liste', async () => {
    const cl = client(liste(BRUT), () => Promise.resolve({ statut: 'technique' }));

    expect(await supprimerBeneficiaire(7, cl)).toEqual({ statut: 'technique' });
    expect(cl.lister).not.toHaveBeenCalled();
  });

  it('[FR-013-09] une coupure rend technique', async () => {
    expect(await supprimerBeneficiaire(7, client(liste(), () => Promise.reject(new Error('réseau'))))).toEqual({ statut: 'technique' });
  });
});

describe('verifierIban', () => {
  it('[FR-013-04] rend le résultat du service, sans le recalculer', async () => {
    const cl = client(liste(), undefined, () => Promise.resolve({ statut: 'verifiee', verification: { statut: 'valide', codePays: 'TR', zoneSepa: false } }));

    expect(await verifierIban('TR33', cl)).toEqual({ statut: 'valide', codePays: 'TR', zoneSepa: false });
    expect(cl.verifierIban).toHaveBeenCalledWith('TR33');
  });

  it('[FR-013-04] rend technique quand le service est indisponible', async () => {
    expect(await verifierIban('x', client(liste()))).toEqual({ statut: 'technique' });
    expect(await verifierIban('x', client(liste(), undefined, () => Promise.reject(new Error('réseau'))))).toEqual({ statut: 'technique' });
  });
});

describe('ajouterBeneficiaire', () => {
  const formulaire = { ...FORMULAIRE_BENEFICIAIRE_VIDE, libelle: 'a', titulaire: 'b', iban: 'FR35' };

  it('[FR-013-08] envoie la saisie telle que tapée puis recharge la liste', async () => {
    const cl = client(liste(BRUT), undefined, undefined, () => Promise.resolve({ statut: 'ajoute' }));

    expect(await ajouterBeneficiaire(formulaire, cl)).toEqual({ statut: 'ajoute', liste: [LIGNE] });
    expect(cl.ajouter).toHaveBeenCalledWith(formulaire);
    expect(cl.lister).toHaveBeenCalledTimes(1);
  });

  it('[FR-013-08] un ajout dont la relecture échoue rend une liste nulle', async () => {
    const cl = client(() => Promise.resolve({ statut: 'technique' }), undefined, undefined, () => Promise.resolve({ statut: 'ajoute' }));

    expect(await ajouterBeneficiaire(formulaire, cl)).toEqual({ statut: 'ajoute', liste: null });
  });

  it('[FR-013-05] un refus rend le texte et le champ du service sans recharger la liste', async () => {
    const refus: ReponseAjout = { statut: 'refuse', message: 'Le libellé est obligatoire.', champ: 'libelle' };
    const cl = client(liste(), undefined, undefined, () => Promise.resolve(refus));

    expect(await ajouterBeneficiaire(formulaire, cl)).toEqual(refus);
    expect(cl.lister).not.toHaveBeenCalled();
  });

  it('[FR-013-08] une panne ou une coupure est technique', async () => {
    expect(await ajouterBeneficiaire(formulaire, client(liste()))).toEqual({ statut: 'technique' });
    expect(await ajouterBeneficiaire(formulaire, client(liste(), undefined, undefined, () => Promise.reject(new Error('réseau'))))).toEqual({ statut: 'technique' });
  });
});
