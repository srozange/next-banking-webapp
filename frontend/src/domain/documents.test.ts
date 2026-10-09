import { describe, expect, it } from 'vitest';
import { anneesIfu, LIBELLES_MOIS, lireAujourdhui, moisRecents, parametresTelechargement, testIdLien } from './documents';

describe('règles de la page Documents', () => {
  it('[FR-008-05] donne les mois en français avec accents', () => {
    expect(LIBELLES_MOIS).toEqual(['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre']);
  });

  it('[FR-008-05] lit année et mois de aujourdhui en entiers', () => {
    expect(lireAujourdhui('2026-11-17')).toEqual({ annee: 2026, mois: 11 });
  });

  it.each(['', '17/11/2026', '2026-13-01', '2026-00-10', '2026-11-00', '2026-11-32', '2026-1-10'])('[FR-008-05] refuse la date %s', (date) => {
    expect(lireAujourdhui(date)).toBeNull();
  });

  it('[FR-008-05] calcule les 12 mois du plus récent au plus ancien, aujourdhui inclus', () => {
    const mois = moisRecents(2026, 11);

    expect(mois).toHaveLength(12);
    expect(mois[0]).toEqual({ annee: 2026, mois: 11, libelle: 'Novembre 2026' });
    expect(mois[11]).toEqual({ annee: 2025, mois: 12, libelle: 'Décembre 2025' });
  });

  it('[FR-008-05] passe l’année en février', () => {
    const mois = moisRecents(2026, 2);

    expect(mois.map((m) => m.libelle)).toEqual([
      'Février 2026',
      'Janvier 2026',
      'Décembre 2025',
      'Novembre 2025',
      'Octobre 2025',
      'Septembre 2025',
      'Août 2025',
      'Juillet 2025',
      'Juin 2025',
      'Mai 2025',
      'Avril 2025',
      'Mars 2025',
    ]);
    expect(mois[2]).toMatchObject({ annee: 2025, mois: 12 });
  });

  it('[FR-008-05] donne les années d’IFU N-1 puis N-2', () => {
    expect(anneesIfu(2026)).toEqual([2025, 2024]);
  });

  it('[FR-008-06] construit les paramètres de chaque type, sans identifiant de client', () => {
    expect(parametresTelechargement({ type: 'RIB', compteId: 1 })).toEqual({ type: 'RIB', compteId: '1' });
    expect(parametresTelechargement({ type: 'ATTESTATION', compteId: 2 })).toEqual({ type: 'ATTESTATION', compteId: '2' });
    expect(parametresTelechargement({ type: 'RELEVE', compteId: 1, annee: 2026, mois: 3 })).toEqual({ type: 'RELEVE', compteId: '1', annee: '2026', mois: '3' });
    expect(parametresTelechargement({ type: 'IFU', annee: 2025 })).toEqual({ type: 'IFU', annee: '2025' });
  });

  it('[FR-008-06] nomme les liens documents-{type}-{id} avec le mois sur deux chiffres', () => {
    expect(testIdLien({ type: 'RIB', compteId: 1 })).toBe('documents-rib-1');
    expect(testIdLien({ type: 'ATTESTATION', compteId: 1 })).toBe('documents-attestation-1');
    expect(testIdLien({ type: 'RELEVE', compteId: 1, annee: 2026, mois: 3 })).toBe('documents-releve-1-2026-03');
    expect(testIdLien({ type: 'RELEVE', compteId: 1, annee: 2025, mois: 12 })).toBe('documents-releve-1-2025-12');
    expect(testIdLien({ type: 'IFU', annee: 2025 })).toBe('documents-ifu-2025');
  });
});
