/**
 * Règles pures de l'écran Produits (FEAT-011) : mise en forme du catalogue et du journal des traitements nocturnes.
 * Taux, rendements et LTV arrivent en chaînes décimales du service ("0.0300") ; tout calcul se fait en entiers,
 * jamais en flottant.
 */
import { centimesDepuisChaine } from './montant';

export const ABSENT = '—';
export const DUREE_ILLIMITEE = 'Illimite';
export const RISQUE_INCONNU = 'N/A';

/** Produit d'épargne, tel que rendu par le service open-banking-conseiller. */
export interface ProduitEpargne {
  id: number;
  nom: string;
  description: string | null;
  taux: string | null;
  depotMin: string | null;
  capitalGaranti: boolean;
  dureeMaxMois: number | null;
}

export interface ProduitCredit {
  id: number;
  nom: string;
  description: string | null;
  taux: string | null;
  dureeMaxMois: number | null;
  ltvMax: string | null;
  assuranceRequise: boolean;
}

export interface ProduitInvestissement {
  id: number;
  nom: string;
  description: string | null;
  secteur: string | null;
  niveauRisque: number | null;
  rendementAttendu: string | null;
}

/** Catalogue : trois groupes, dans l'ordre du service (tri par nom). */
export interface Catalogue {
  epargne: readonly ProduitEpargne[];
  credit: readonly ProduitCredit[];
  investissement: readonly ProduitInvestissement[];
}

/** Exécution d'un traitement nocturne, telle que rendue par le cœur bancaire (début et fin ISO-8601 avec décalage). */
export interface Traitement {
  job: string;
  debut: string | null;
  fin: string | null;
  dureeMs: number;
  enregistrements: number | null;
  statut: string | null;
  details: string | null;
}

/** Taux décimal : entiers, puis au plus quatre décimales (format du service). */
export const FORMAT_TAUX = /^\d+(?:\.\d{1,4})?$/;
/** Instant ISO-8601 avec décalage ; les secondes sont omises par le service quand elles valent zéro. */
export const FORMAT_INSTANT = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})$/;

const deuxChiffres = (nombre: number) => String(nombre).padStart(2, '0');

/** Valeur absente : « — ». */
export function valeurOuAbsent(valeur: string | null): string {
  return valeur === null || valeur === '' ? ABSENT : valeur;
}

/** BUG-039, FR-011-05 : "0.0300" donne "3,00 %" (dix-millièmes entiers, puis division entière), jamais "0.03". */
export function formaterPourcentage(taux: string | null): string {
  if (taux === null) {
    return ABSENT;
  }
  if (!FORMAT_TAUX.test(taux)) {
    throw new Error(`Taux invalide : ${taux}`);
  }
  const [entiers, decimales = ''] = taux.split('.');
  const dixMilliemes = Number(entiers) * 10000 + Number(decimales.padEnd(4, '0'));
  return `${Math.floor(dixMilliemes / 100)},${deuxChiffres(dixMilliemes % 100)} %`;
}

/** FR-011-04 : dépôt minimal "300.00" donne "300,00 EUR", par les centimes entiers. */
export function formaterDepotMin(depot: string | null): string {
  if (depot === null) {
    return ABSENT;
  }
  const centimes = centimesDepuisChaine(depot);
  return `${Math.floor(centimes / 100)},${deuxChiffres(centimes % 100)} EUR`;
}

/** FR-011-03 : « Oui » ou « Non ». */
export function formaterBooleen(valeur: boolean): string {
  return valeur ? 'Oui' : 'Non';
}

/** FR-011-03, FR-011-04 : « {n} mois » ; absente, « Illimite » pour l'épargne et « — » pour le crédit. */
export function formaterDuree(mois: number | null, absente: string): string {
  return mois === null ? absente : `${mois} mois`;
}

const LIBELLES_RISQUE = ['Tres faible', 'Faible', 'Moyen', 'Eleve', 'Tres eleve'] as const;

/** FR-011-06 : « {libellé} ({score}/5) » de 1 à 5, sinon « N/A ». */
export function formaterRisque(niveau: number | null): string {
  const libelle = niveau === null ? undefined : LIBELLES_RISQUE[niveau - 1];
  return niveau === null || libelle === undefined ? RISQUE_INCONNU : `${libelle} (${niveau}/5)`;
}

/** FR-011-09 : "2026-10-06T02:00:05+02:00" donne "06/10/2026 02:00:05" (déjà au fuseau Europe/Paris) ; absent, vide. */
export function formaterInstant(instant: string | null): string {
  if (instant === null) {
    return '';
  }
  const decoupe = FORMAT_INSTANT.exec(instant);
  if (decoupe === null) {
    throw new Error(`Instant invalide : ${instant}`);
  }
  const [, annee, mois, jour, heure, minute, seconde = '00'] = decoupe;
  return `${jour}/${mois}/${annee} ${heure}:${minute}:${seconde}`;
}

/** Nombre absent : cellule vide. */
export function formaterNombre(valeur: number | null): string {
  return valeur === null ? '' : String(valeur);
}
