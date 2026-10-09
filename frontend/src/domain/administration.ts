import { centimesDepuisChaine } from './montant';
import { formaterEurosArrondi } from './portefeuille';
import { formaterDateEcheance } from './pret';
import { formaterDate, formaterHeure } from './virement';

/** Indicateurs globaux lus dans identite-clients (FR-016-02). */
export interface IndicateursIdentite {
  totalClients: number;
  clientsActifs: number;
  conseillers: number;
  totalUtilisateurs: number;
}

/** Indicateur des prêts EN_ATTENTE lu dans credits (FR-016-02) : nombre et cumul en chaîne à 2 décimales. */
export interface IndicateursPrets {
  pretsEnAttente: number;
  montantEnAttente: string;
}

/**
 * Client de la gestion des clients (FR-016-08, FR-016-09) : rôle tel que renvoyé (`CLIENT`), téléphone et conseiller
 * absents possibles, date de création de la fiche `aaaa-MM-jj` (BUG-055 : jamais la date d'affectation).
 */
export interface ClientAdministre {
  clientId: number;
  nomComplet: string;
  login: string;
  email: string;
  telephone: string | null;
  role: string;
  actif: boolean;
  nomConseiller: string | null;
  dateCreation: string;
}

/** Utilisateur de la liste « Tous les utilisateurs du systeme » (FR-016-13), rôle tel que renvoyé. */
export interface UtilisateurAdministre {
  utilisateurId: number;
  nomComplet: string;
  login: string;
  email: string;
  role: string;
  actif: boolean;
}

/**
 * Filtre de la gestion des clients (règle du legacy) : terme rogné, recherché sans casse et en partie dans le nom
 * complet, le login ou l'email ; un terme vide rend tous les clients, dans leur ordre.
 */
export function filtrerClients(clients: readonly ClientAdministre[], terme: string): readonly ClientAdministre[] {
  const cherche = terme.trim().toLowerCase();
  if (cherche === '') {
    return clients;
  }
  return clients.filter((client) =>
    [client.nomComplet, client.login, client.email].some((valeur) => valeur.toLowerCase().includes(cherche)),
  );
}

/** Montant des prêts en attente au format legacy `EUR15,000` (`EUR0` sans prêt), calculé en centimes entiers. */
export function montantATraiter(indicateurs: IndicateursPrets): string {
  return formaterEurosArrondi(centimesDepuisChaine(indicateurs.montantEnAttente));
}

/** « Client depuis » au format `jj/MM/aaaa`, depuis la date de création de la fiche (FR-016-10, BUG-055). */
export function clientDepuis(client: ClientAdministre): string {
  return formaterDateEcheance(client.dateCreation);
}

/**
 * Alerte de fraude vue par l'ADMIN (FR-016-14, BUG-056 : tous statuts). Règle et statut en codes métier tels que
 * renvoyés ; `nomClient` et `nomRelecteur` lus dans la projection du cœur bancaire (null si absents) ; `revuPar` est
 * l'identifiant brut de l'auteur de la revue (null tant que l'alerte est ouverte) ; `creeLe` en ISO-8601.
 */
export interface AlerteFraudeAdministree {
  alerteId: number;
  clientId: number;
  nomClient: string | null;
  regle: string;
  score: number;
  statut: string;
  description: string | null;
  creeLe: string;
  revuPar: string | null;
  nomRelecteur: string | null;
}

/** Vrai si l'alerte attend encore une revue : seules celles-ci offrent « Confirmer » et « Faux positif ». */
export function alerteOuverte(alerte: AlerteFraudeAdministree): boolean {
  return alerte.statut === 'OPEN';
}

/** Nombre d'alertes OPEN, affiché en badge seulement s'il est positif (MSG-016-091). */
export function compterAlertesOuvertes(alertes: readonly AlerteFraudeAdministree[]): number {
  return alertes.filter(alerteOuverte).length;
}

/** Score de risque au format legacy `{score}/100` (MSG-016-094). */
export function scoreAlerte(alerte: AlerteFraudeAdministree): string {
  return `${alerte.score}/100`;
}

/** Auteur de la revue affiché : nom complet de la projection, sinon l'identifiant enregistré, sinon rien. */
export function relecteurAlerte(alerte: AlerteFraudeAdministree): string {
  return alerte.nomRelecteur ?? alerte.revuPar ?? '';
}

/** Date de création `dd/MM/yyyy HH:mm` en Europe/Paris, comme la revue des virements. */
export function dateAlerte(alerte: AlerteFraudeAdministree): string {
  return `${formaterDate(alerte.creeLe)} ${formaterHeure(alerte.creeLe)}`;
}
