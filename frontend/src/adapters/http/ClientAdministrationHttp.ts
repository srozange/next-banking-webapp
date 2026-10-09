import type { ClientAdministration, ReponseAdministration } from '../../application/consulterAdministration';
import type { ClientAdministre, IndicateursIdentite, IndicateursPrets, UtilisateurAdministre } from '../../domain/administration';
import { TEXTES } from '../textes';

export const CHEMIN_ADMIN_INDICATEURS_IDENTITE = '/api/identite-clients/administration/indicateurs';
export const CHEMIN_ADMIN_INDICATEURS_PRETS = '/api/credits/administration/indicateurs';
export const CHEMIN_ADMIN_CLIENTS = '/api/identite-clients/administration/clients';
export const CHEMIN_ADMIN_UTILISATEURS = '/api/identite-clients/administration/utilisateurs';

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
 * l'erreur technique.
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

  return {
    lireIndicateursIdentite: () => lire(CHEMIN_ADMIN_INDICATEURS_IDENTITE, indicateursIdentiteDepuis),
    lireIndicateursPrets: () => lire(CHEMIN_ADMIN_INDICATEURS_PRETS, indicateursPretsDepuis),
    lireClients: () => lire(CHEMIN_ADMIN_CLIENTS, (corps) => listeDepuis(corps, clientDepuis)),
    lireUtilisateurs: () => lire(CHEMIN_ADMIN_UTILISATEURS, (corps) => listeDepuis(corps, utilisateurDepuis)),
  };
}
