import { describe, expect, it, vi } from 'vitest';
import type { BeneficiaireBrut } from '../domain/beneficiaires';
import { chargerBeneficiaires, supprimerBeneficiaire, type ClientBeneficiaires, type ReponseBeneficiaires, type ReponseSuppression } from './gererBeneficiaires';

const BRUT: BeneficiaireBrut = { id: 1, libelle: 'Loyer', titulaire: 'Marie', iban: 'FR76***0189', bic: ' ', banque: null, zoneSepa: true };
const LIGNE = { ...BRUT, bic: null };

const client = (lister: () => Promise<ReponseBeneficiaires>, supprimer: () => Promise<ReponseSuppression> = () => Promise.resolve({ statut: 'supprime' })) =>
  ({ lister: vi.fn(lister), supprimer: vi.fn(supprimer) }) satisfies ClientBeneficiaires;
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
