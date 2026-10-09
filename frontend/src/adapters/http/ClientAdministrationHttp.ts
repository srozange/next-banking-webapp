import type { ClientAdministration, IssueRevueAlerte, ReponseAdministration } from '../../application/consulterAdministration';
import type {
  AlerteFraudeAdministree,
  ClientAdministre,
  IndicateursIdentite,
  IndicateursPrets,
  UtilisateurAdministre,
} from '../../domain/administration';
import { TEXTES } from '../textes';

export const CHEMIN_ADMIN_INDICATEURS_IDENTITE = '/api/identite-clients/administration/indicateurs';
export const CHEMIN_ADMIN_INDICATEURS_PRETS = '/api/credits/administration/indicateurs';
export const CHEMIN_ADMIN_CLIENTS = '/api/identite-clients/administration/clients';
export const CHEMIN_ADMIN_UTILISATEURS = '/api/identite-clients/administration/utilisateurs';
export const CHEMIN_ADMIN_ALERTES_FRAUDE = '/api/coeur-bancaire/administration/alertes-fraude';

/** Revue d'une alerte de fraude par l'ADMIN : confirmation ou faux positif. */
export const cheminRevueAlerte = (alerteId: number, issue: IssueRevueAlerte) => `${CHEMIN_ADMIN_ALERTES_FRAUDE}/${alerteId}/${issue}`;

/** API ADMIN d'identite-clients (story 2.6) : désactivation si `actif` est faux, activation sinon. */
export const cheminEtatClient = (clientId: number, actif: boolean) =>
  `/api/identite-clients/clients/${clientId}/${actif ? 'activation' : 'desactivation'}`;

const NON_AUTHENTIFIE = { statut: 'nonAuthentifie' } as const;
const TECHNIQUE = { statut: 'erreur', message: TEXTES.erreurTechnique } as const;
const MONTANT = /^\d+\.\d{2}$/;
const DATE = /^\d{4}-\d{2}-\d{2}$/;

type Champs = Record<string, unknown>;

const enregistrement = (valeur: unknown): Champs | null =>
  typeof valeur === 'object' && valeur !== null && !Array.isArray(valeur) ? (valeur as Champs) : null;

const texte = (valeur: unknown): valeur is string => typeof valeur === 'string';
const texteOuNul = (valeur: unknown): valeur is string | null | undefined => valeur === null || valeur === undefined || texte(valeur);
const entier = (valeur: unknown): valeur is number => Number.isInteger(valeur);

/** Liste dont chaque élément est converti ; un seul élément illisible rend le tout illisible. */
function listeDepuis<T>(brut: unknown, convertir: (element: unknown) => T | null): T[] | null {
  if (!Array.isArray(brut)) {
    return null;
  }
  const elements = brut.map(convertir);
  return elements.every((element) => element !== null) ? (elements as T[]) : null;
}

function indicateursIdentiteDepuis(brut: unknown): IndicateursIdentite | null {
  const c = enregistrement(brut);
  if (c === null || !entier(c.totalClients) || !entier(c.clientsActifs) || !entier(c.conseillers) || !entier(c.totalUtilisateurs)) {
    return null;
  }
  return { totalClients: c.totalClients, clientsActifs: c.clientsActifs, conseillers: c.conseillers, totalUtilisateurs: c.totalUtilisateurs };
}

function indicateursPretsDepuis(brut: unknown): IndicateursPrets | null {
  const c = enregistrement(brut);
  if (c === null || !entier(c.pretsEnAttente) || !texte(c.montantEnAttente) || !MONTANT.test(c.montantEnAttente)) {
    return null;
  }
  return { pretsEnAttente: c.pretsEnAttente, montantEnAttente: c.montantEnAttente };
}

function clientDepuis(brut: unknown): ClientAdministre | null {
  const c = enregistrement(brut);
  if (
    c === null || !entier(c.clientId) || !texte(c.nomComplet) || !texte(c.login) || !texte(c.email) || !texteOuNul(c.telephone) ||
    !texte(c.role) || typeof c.actif !== 'boolean' || !texteOuNul(c.nomConseiller) || !texte(c.dateCreation) || !DATE.test(c.dateCreation)
  ) {
    return null;
  }
  return {
    clientId: c.clientId, nomComplet: c.nomComplet, login: c.login, email: c.email, telephone: c.telephone ?? null, role: c.role,
    actif: c.actif, nomConseiller: c.nomConseiller ?? null, dateCreation: c.dateCreation,
  };
}

function utilisateurDepuis(brut: unknown): UtilisateurAdministre | null {
  const c = enregistrement(brut);
  if (
    c === null || !entier(c.utilisateurId) || !texte(c.nomComplet) || !texte(c.login) || !texte(c.email) || !texte(c.role) ||
    typeof c.actif !== 'boolean'
  ) {
    return null;
  }
  return { utilisateurId: c.utilisateurId, nomComplet: c.nomComplet, login: c.login, email: c.email, role: c.role, actif: c.actif };
}

function alerteDepuis(brut: unknown): AlerteFraudeAdministree | null {
  const c = enregistrement(brut);
  if (
    c === null || !entier(c.alerteId) || !entier(c.clientId) || !texteOuNul(c.nomClient) || !texte(c.regle) || !entier(c.score) ||
    !texte(c.statut) || !texteOuNul(c.description) || !texte(c.creeLe) || Number.isNaN(Date.parse(c.creeLe)) || !texteOuNul(c.revuPar) ||
    !texteOuNul(c.nomRelecteur)
  ) {
    return null;
  }
  return {
    alerteId: c.alerteId, clientId: c.clientId, nomClient: c.nomClient ?? null, regle: c.regle, score: c.score, statut: c.statut,
    description: c.description ?? null, creeLe: c.creeLe, revuPar: c.revuPar ?? null, nomRelecteur: c.nomRelecteur ?? null,
  };
}

/** Corps JSON de la réponse, ou undefined s'il est illisible. */
async function corpsDe(reponse: Response): Promise<unknown> {
  try {
    return (await reponse.json()) as unknown;
  } catch {
    return undefined;
  }
}

/**
 * Adaptateur HTTP de la page d'administration (identite-clients et credits), par `fetchAuthentifie` (tout 401
 * supprime déjà le jeton) ; aucun paramètre de requête n'est envoyé. 401 : non authentifié ; 200 au contrat : ok ;
 * tout autre statut : le `message` texte du corps, sinon l'erreur technique ; réseau coupé ou corps hors contrat :
 * l'erreur technique. Activation, désactivation et revue d'une alerte : `POST` sans corps ni paramètre, tout 2xx vaut
 * succès (corps non lu).
 */
export function creerClientAdministrationHttp(appeler: typeof fetch = (entree, options) => fetch(entree, options)): ClientAdministration {
  async function lire<T>(chemin: string, convertir: (corps: unknown) => T | null): Promise<ReponseAdministration<T>> {
    try {
      const reponse = await appeler(chemin, { method: 'GET', headers: { Accept: 'application/json' } });
      if (reponse.status === 401) {
        return NON_AUTHENTIFIE;
      }
      const corps = await corpsDe(reponse);
      if (reponse.status !== 200) {
        const message = enregistrement(corps)?.message;
        return texte(message) ? { statut: 'erreur', message } : TECHNIQUE;
      }
      const donnees = convertir(corps);
      return donnees === null ? TECHNIQUE : { statut: 'ok', donnees };
    } catch {
      return TECHNIQUE;
    }
  }

  async function poster(chemin: string): Promise<ReponseAdministration<null>> {
    try {
      const reponse = await appeler(chemin, { method: 'POST', headers: { Accept: 'application/json' } });
      if (reponse.status === 401) {
        return NON_AUTHENTIFIE;
      }
      if (reponse.ok) {
        return { statut: 'ok', donnees: null };
      }
      const message = enregistrement(await corpsDe(reponse))?.message;
      return texte(message) ? { statut: 'erreur', message } : TECHNIQUE;
    } catch {
      return TECHNIQUE;
    }
  }

  return {
    changerEtatClient: (clientId, actif) => poster(cheminEtatClient(clientId, actif)),
    revoirAlerte: (alerteId, issue) => poster(cheminRevueAlerte(alerteId, issue)),
    lireAlertesFraude: () => lire(CHEMIN_ADMIN_ALERTES_FRAUDE, (corps) => listeDepuis(corps, alerteDepuis)),
    lireIndicateursIdentite: () => lire(CHEMIN_ADMIN_INDICATEURS_IDENTITE, indicateursIdentiteDepuis),
    lireIndicateursPrets: () => lire(CHEMIN_ADMIN_INDICATEURS_PRETS, indicateursPretsDepuis),
    lireClients: () => lire(CHEMIN_ADMIN_CLIENTS, (corps) => listeDepuis(corps, clientDepuis)),
    lireUtilisateurs: () => lire(CHEMIN_ADMIN_UTILISATEURS, (corps) => listeDepuis(corps, utilisateurDepuis)),
  };
}
