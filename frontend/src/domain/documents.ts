/**
 * Règles pures de la page Documents (FEAT-008) : mois en français, 12 mois les plus récents et deux années fiscales
 * calculés depuis `aujourdhui` (chaîne yyyy-MM-dd fournie par le service, lue en entiers : jamais l'horloge du navigateur),
 * paramètres de téléchargement et identifiants de test.
 */

/** Libellés de mois du legacy (avec accents), de janvier à décembre. */
export const LIBELLES_MOIS = [
  'Janvier',
  'Février',
  'Mars',
  'Avril',
  'Mai',
  'Juin',
  'Juillet',
  'Août',
  'Septembre',
  'Octobre',
  'Novembre',
  'Décembre',
] as const;

const FORMAT_DATE = /^(\d{4})-(\d{2})-(\d{2})$/;

/** Année et mois (1 à 12) de `aujourdhui`, ou null si la chaîne n'est pas une date yyyy-MM-dd. */
export function lireAujourdhui(aujourdhui: string): { annee: number; mois: number } | null {
  const trouve = FORMAT_DATE.exec(aujourdhui);
  if (trouve === null) {
    return null;
  }
  const annee = Number(trouve[1]);
  const mois = Number(trouve[2]);
  const jour = Number(trouve[3]);
  return mois >= 1 && mois <= 12 && jour >= 1 && jour <= 31 ? { annee, mois } : null;
}

/** Un mois proposé en relevé. */
export interface MoisReleve {
  annee: number;
  /** 1 à 12. */
  mois: number;
  /** « Novembre 2026 ». */
  libelle: string;
}

/** Les 12 mois les plus récents, du plus récent (le mois de `aujourdhui`) au plus ancien. */
export function moisRecents(annee: number, mois: number): MoisReleve[] {
  return Array.from({ length: 12 }, (_, decalage) => {
    const rang = annee * 12 + (mois - 1) - decalage;
    const anneeDuMois = Math.floor(rang / 12);
    const moisDuMois = (rang % 12) + 1;
    return { annee: anneeDuMois, mois: moisDuMois, libelle: `${LIBELLES_MOIS[moisDuMois - 1]} ${anneeDuMois}` };
  });
}

/** Les deux années fiscales précédant l'année de `aujourdhui` : N-1 puis N-2. */
export function anneesIfu(annee: number): [number, number] {
  return [annee - 1, annee - 2];
}

/** Document demandé : RIB et attestation d'un compte, relevé d'un compte et d'un mois, IFU d'une année. */
export type DemandeDocument =
  | { type: 'RIB'; compteId: number }
  | { type: 'ATTESTATION'; compteId: number }
  | { type: 'RELEVE'; compteId: number; annee: number; mois: number }
  | { type: 'IFU'; annee: number };

/** Paramètres de la requête de téléchargement : jamais d'identifiant de client. */
export function parametresTelechargement(demande: DemandeDocument): Record<string, string> {
  switch (demande.type) {
    case 'RIB':
    case 'ATTESTATION':
      return { type: demande.type, compteId: String(demande.compteId) };
    case 'RELEVE':
      return { type: 'RELEVE', compteId: String(demande.compteId), annee: String(demande.annee), mois: String(demande.mois) };
    case 'IFU':
      return { type: 'IFU', annee: String(demande.annee) };
  }
}

const deuxChiffres = (mois: number) => String(mois).padStart(2, '0');

/** data-testid du lien d'un document (`documents-releve-{compteId}-{aaaa}-{mm}`, `documents-ifu-{annee}`…). */
export function testIdLien(demande: DemandeDocument): string {
  switch (demande.type) {
    case 'RIB':
      return `documents-rib-${demande.compteId}`;
    case 'ATTESTATION':
      return `documents-attestation-${demande.compteId}`;
    case 'RELEVE':
      return `documents-releve-${demande.compteId}-${demande.annee}-${deuxChiffres(demande.mois)}`;
    case 'IFU':
      return `documents-ifu-${demande.annee}`;
  }
}
