import type { ClientPortefeuille, ReponsePortefeuille } from '../../application/consulterPortefeuille';
import type {
  ClientDuPortefeuille,
  ComptesDuClient,
  CompteDuPortefeuille,
  PretDuPortefeuille,
  PretsDuClient,
} from '../../domain/portefeuille';
import { TEXTES } from '../textes';

export const CHEMIN_PORTEFEUILLE_IDENTITES = '/api/identite-clients/conseiller/portefeuille';
export const CHEMIN_PORTEFEUILLE_COMPTES = '/api/coeur-bancaire/conseiller/portefeuille';
export const CHEMIN_PORTEFEUILLE_PRETS = '/api/credits/conseiller/portefeuille';

const NON_AUTHENTIFIE = { statut: 'nonAuthentifie' } as const;
const TECHNIQUE = { statut: 'erreur', message: TEXTES.erreurTechnique } as const;
const MONTANT = /^-?\d+\.\d{2}$/;

type Champs = Record<string, unknown>;

const enregistrement = (valeur: unknown): Champs | null =>
  typeof valeur === 'object' && valeur !== null && !Array.isArray(valeur) ? (valeur as Champs) : null;

const texte = (valeur: unknown): valeur is string => typeof valeur === 'string';
const texteOuNul = (valeur: unknown): valeur is string | null | undefined => valeur === null || valeur === undefined || texte(valeur);
const entier = (valeur: unknown): valeur is number => Number.isInteger(valeur);
const montant = (valeur: unknown): valeur is string => texte(valeur) && MONTANT.test(valeur);

/** Liste dont chaque élément est converti ; un seul élément illisible rend le tout illisible. */
function listeDepuis<T>(brut: unknown, convertir: (element: unknown) => T | null): T[] | null {
  if (!Array.isArray(brut)) {
    return null;
  }
  const elements = brut.map(convertir);
  return elements.every((element) => element !== null) ? (elements as T[]) : null;
}

function identiteDepuis(brut: unknown): ClientDuPortefeuille | null {
  const c = enregistrement(brut);
  if (
    c === null || !entier(c.clientId) || !texte(c.nomComplet) || !texte(c.login) || !texteOuNul(c.email) ||
    !texteOuNul(c.telephone) || typeof c.actif !== 'boolean'
  ) {
    return null;
  }
  return {
    clientId: c.clientId, nomComplet: c.nomComplet, login: c.login, email: c.email ?? null, telephone: c.telephone ?? null,
    actif: c.actif,
  };
}

function compteDepuis(brut: unknown): CompteDuPortefeuille | null {
  const c = enregistrement(brut);
  if (c === null || !entier(c.compteId) || !texte(c.numero) || !texte(c.type) || !montant(c.solde)) {
    return null;
  }
  return { compteId: c.compteId, numero: c.numero, type: c.type, solde: c.solde };
}

function pretDepuis(brut: unknown): PretDuPortefeuille | null {
  const c = enregistrement(brut);
  if (
    c === null || !entier(c.pretId) || !texteOuNul(c.type) || !montant(c.montant) || !montant(c.mensualite) || !texte(c.statut)
  ) {
    return null;
  }
  return { pretId: c.pretId, type: c.type ?? null, montant: c.montant, mensualite: c.mensualite, statut: c.statut };
}

function comptesDuClientDepuis(brut: unknown): ComptesDuClient | null {
  const c = enregistrement(brut);
  const comptes = listeDepuis(c?.comptes, compteDepuis);
  return c === null || !entier(c.clientId) || comptes === null ? null : { clientId: c.clientId, comptes };
}

function pretsDuClientDepuis(brut: unknown): PretsDuClient | null {
  const c = enregistrement(brut);
  const prets = listeDepuis(c?.prets, pretDepuis);
  return c === null || !entier(c.clientId) || prets === null ? null : { clientId: c.clientId, prets };
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
 * Adaptateur HTTP du portefeuille du conseiller (identite-clients, coeur-bancaire, credits), par `fetchAuthentifie`
 * (tout 401 supprime déjà le jeton) ; aucun paramètre de requête n'est envoyé. 401 : non authentifié ; 200 au
 * contrat : ok ; tout autre statut : le `message` texte du corps, sinon l'erreur technique ; réseau coupé ou corps
 * hors contrat : l'erreur technique.
 */
export function creerClientPortefeuilleHttp(appeler: typeof fetch = (entree, options) => fetch(entree, options)): ClientPortefeuille {
  async function lire<T>(chemin: string, convertir: (element: unknown) => T | null): Promise<ReponsePortefeuille<readonly T[]>> {
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
      const donnees = listeDepuis(corps, convertir);
      return donnees === null ? TECHNIQUE : { statut: 'ok', donnees };
    } catch {
      return TECHNIQUE;
    }
  }

  return {
    lireIdentites: () => lire(CHEMIN_PORTEFEUILLE_IDENTITES, identiteDepuis),
    lireComptes: () => lire(CHEMIN_PORTEFEUILLE_COMPTES, comptesDuClientDepuis),
    lirePrets: () => lire(CHEMIN_PORTEFEUILLE_PRETS, pretsDuClientDepuis),
  };
}
