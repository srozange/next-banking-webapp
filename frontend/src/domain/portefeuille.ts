import { centimesDepuisChaine } from './montant';

/** Règles pures de l'Espace Conseiller (FEAT-015) : jointure des trois réponses, indicateurs, formats legacy. */

/** Client du portefeuille, tel que rendu par identite-clients (déjà trié par nom complet). */
export interface ClientDuPortefeuille {
  clientId: number;
  nomComplet: string;
  login: string;
  email: string | null;
  telephone: string | null;
  actif: boolean;
}

/** Compte d'un client du portefeuille (cœur bancaire) ; `solde` est une chaîne à deux décimales. */
export interface CompteDuPortefeuille {
  compteId: number;
  numero: string;
  type: string;
  solde: string;
}

export interface ComptesDuClient {
  clientId: number;
  comptes: readonly CompteDuPortefeuille[];
}

/** Prêt d'un client du portefeuille (crédits) ; codes renvoyés tels quels, `type` peut être nul. */
export interface PretDuPortefeuille {
  pretId: number;
  type: string | null;
  montant: string;
  mensualite: string;
  statut: string;
}

export interface PretsDuClient {
  clientId: number;
  prets: readonly PretDuPortefeuille[];
}

/** Indicateur de prêts d'une carte : les prêts en attente l'emportent sur les prêts en cours (FR-015-06). */
export type IndicateurPrets = { type: 'attente'; nombre: number } | { type: 'cours'; nombre: number } | { type: 'aucun' };

/** Carte d'un client : ses données jointes par `clientId` et ses statistiques. */
export interface CarteClient {
  client: ClientDuPortefeuille;
  comptes: readonly CompteDuPortefeuille[];
  prets: readonly PretDuPortefeuille[];
  soldeTotalCentimes: number;
  nombreComptes: number;
  pretsEnCours: number;
  pretsEnAttente: number;
  indicateur: IndicateurPrets;
}

/** Portefeuille affiché : cartes dans l'ordre de l'identité, puis les trois indicateurs (FR-015-03). */
export interface Portefeuille {
  cartes: readonly CarteClient[];
  nombreClients: number;
  encoursCentimes: number;
  pretsEnAttente: number;
}

export const STATUT_EN_ATTENTE = 'EN_ATTENTE';
export const STATUT_EN_COURS = 'EN_COURS';

/** Statuts de prêt affichés en badge ; DEFAUT et tout autre statut restent en texte brut (BUG-053 conservé). */
const STATUTS_AVEC_BADGE: readonly string[] = [STATUT_EN_ATTENTE, STATUT_EN_COURS, 'REFUSE', 'REMBOURSE'];

const compter = (prets: readonly PretDuPortefeuille[], statut: string) => prets.filter((pret) => pret.statut === statut).length;

function indicateurDe(enAttente: number, enCours: number): IndicateurPrets {
  if (enAttente > 0) {
    return { type: 'attente', nombre: enAttente };
  }
  return enCours > 0 ? { type: 'cours', nombre: enCours } : { type: 'aucun' };
}

function carteDe(client: ClientDuPortefeuille, comptes: readonly CompteDuPortefeuille[], prets: readonly PretDuPortefeuille[]): CarteClient {
  const pretsEnCours = compter(prets, STATUT_EN_COURS);
  const pretsEnAttente = compter(prets, STATUT_EN_ATTENTE);
  return {
    client,
    comptes,
    prets,
    soldeTotalCentimes: comptes.reduce((somme, compte) => somme + centimesDepuisChaine(compte.solde), 0),
    nombreComptes: comptes.length,
    pretsEnCours,
    pretsEnAttente,
    indicateur: indicateurDe(pretsEnAttente, pretsEnCours),
  };
}

/**
 * Joint les trois réponses par `clientId` (comptes ou prêts absents : listes vides), dans l'ordre de l'identité, puis
 * calcule les indicateurs en centimes entiers : nombre de clients, encours total, prêts EN_ATTENTE (FR-015-03, FR-015-05).
 */
export function assemblerPortefeuille(
  identites: readonly ClientDuPortefeuille[],
  comptes: readonly ComptesDuClient[],
  prets: readonly PretsDuClient[],
): Portefeuille {
  const comptesParClient = new Map(comptes.map((element) => [element.clientId, element.comptes]));
  const pretsParClient = new Map(prets.map((element) => [element.clientId, element.prets]));
  const cartes = identites.map((client) =>
    carteDe(client, comptesParClient.get(client.clientId) ?? [], pretsParClient.get(client.clientId) ?? []),
  );
  return {
    cartes,
    nombreClients: cartes.length,
    encoursCentimes: cartes.reduce((somme, carte) => somme + carte.soldeTotalCentimes, 0),
    pretsEnAttente: cartes.reduce((somme, carte) => somme + carte.pretsEnAttente, 0),
  };
}

/**
 * Montant au format legacy `EUR{N,NNN}` (`f:convertNumber type="currency" minFractionDigits="0"`) : milliers séparés
 * par une virgule, décimales seulement si non nulles, zéro final retiré (« EUR17,330 », « EUR17,330.5 », « EUR0 ») ;
 * négatif : « -EUR… ». Calcul en centimes entiers.
 */
export function formaterEurosArrondi(centimes: number): string {
  const absolu = Math.abs(centimes);
  const entiers = String(Math.floor(absolu / 100)).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  const reste = absolu % 100;
  const decimales = reste === 0 ? '' : `.${String(reste).padStart(2, '0').replace(/0$/, '')}`;
  return `${centimes < 0 ? '-' : ''}EUR${entiers}${decimales}`;
}

/** Initiale de l'avatar : premier caractère du nom complet (RG-015-010). */
export function initiale(nomComplet: string): string {
  return nomComplet.charAt(0);
}

/** Vrai si le statut de prêt s'affiche en badge (FR-015-09) ; DEFAUT sans badge (BUG-053 conservé). */
export function statutAvecBadge(statut: string): boolean {
  return STATUTS_AVEC_BADGE.includes(statut);
}

/** Sous-ligne du panneau de détail : `@{login} · {email} · {telephone}`, parties nulles omises (FR-015-07). */
export function coordonnees(client: ClientDuPortefeuille): string {
  return [`@${client.login}`, client.email, client.telephone].filter((partie) => partie !== null).join(' · ');
}

/** Carte d'un client du portefeuille, ou null s'il n'y figure pas. */
export function carteDuClient(portefeuille: Portefeuille, clientId: number): CarteClient | null {
  return portefeuille.cartes.find((carte) => carte.client.clientId === clientId) ?? null;
}
