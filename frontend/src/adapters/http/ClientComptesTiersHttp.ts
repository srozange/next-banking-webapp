import type { ClientComptesTiers, ReponseTiers } from '../../application/comptesTiers';
import type { CompteExterne, EtatComptesTiers, TransactionExterne } from '../../domain/comptesTiers';

export const CHEMIN_POWENS = '/api/open-banking-conseiller/powens';
export const CHEMIN_CONNEXION = `${CHEMIN_POWENS}/connexion`;
export const CHEMIN_RAFRAICHISSEMENT = `${CHEMIN_POWENS}/rafraichissement`;

const TECHNIQUE = { statut: 'technique' } as const;
const NON_AUTHENTIFIE = { statut: 'nonAuthentifie' } as const;
const INDISPONIBLE = { statut: 'indisponible' } as const;
const MONTANT = /^-?\d+\.\d{2}$/;

const enregistrement = (valeur: unknown): Record<string, unknown> | null =>
  typeof valeur === 'object' && valeur !== null && !Array.isArray(valeur) ? (valeur as Record<string, unknown>) : null;

const tousPresents = <T>(liste: readonly (T | null)[]): liste is T[] => liste.every((element) => element !== null);

const texteOuNul = (valeur: unknown): valeur is string | null | undefined =>
  valeur === null || valeur === undefined || typeof valeur === 'string';

function compteDepuis(brut: unknown): CompteExterne | null {
  const champs = enregistrement(brut);
  if (
    champs === null ||
    typeof champs.id !== 'number' ||
    typeof champs.nom !== 'string' ||
    !texteOuNul(champs.type) ||
    !texteOuNul(champs.iban) ||
    typeof champs.soldeAbsent !== 'boolean' ||
    !(champs.solde === null || champs.solde === undefined || (typeof champs.solde === 'string' && MONTANT.test(champs.solde)))
  ) {
    return null;
  }
  return {
    id: champs.id,
    nom: champs.nom,
    type: champs.type ?? null,
    solde: champs.soldeAbsent ? null : (champs.solde ?? null),
    soldeAbsent: champs.soldeAbsent || champs.solde === null || champs.solde === undefined,
    iban: champs.iban ?? null,
  };
}

function transactionDepuis(brut: unknown): TransactionExterne | null {
  const champs = enregistrement(brut);
  if (
    champs === null ||
    typeof champs.date !== 'string' ||
    typeof champs.libelle !== 'string' ||
    typeof champs.montant !== 'string' ||
    !MONTANT.test(champs.montant)
  ) {
    return null;
  }
  return { date: champs.date, libelle: champs.libelle, montant: champs.montant };
}

function etatDepuis(brut: unknown): EtatComptesTiers | null {
  const champs = enregistrement(brut);
  if (
    champs === null ||
    typeof champs.connecte !== 'boolean' ||
    !Array.isArray(champs.comptes) ||
    !Array.isArray(champs.transactions) ||
    !(champs.compteSelectionne === null || champs.compteSelectionne === undefined || typeof champs.compteSelectionne === 'number') ||
    !texteOuNul(champs.message) ||
    typeof champs.patrimoineTotal !== 'string' ||
    !MONTANT.test(champs.patrimoineTotal)
  ) {
    return null;
  }
  const comptes = champs.comptes.map(compteDepuis);
  const transactions = champs.transactions.map(transactionDepuis);
  if (!tousPresents(comptes) || !tousPresents(transactions)) {
    return null;
  }
  return {
    connecte: champs.connecte,
    comptes,
    compteSelectionne: champs.compteSelectionne ?? null,
    transactions,
    message: champs.message ?? '',
    patrimoineTotal: champs.patrimoineTotal,
  };
}

/**
 * Adaptateur HTTP du service open-banking-conseiller (intégration Powens), par `fetchAuthentifie` (tout 401 supprime
 * déjà le jeton). 401 : non authentifié ; 503 : Powens indisponible ; tout le reste (autre statut, réseau coupé, corps
 * illisible ou inattendu) : erreur technique. Le jeton n'est jamais journalisé.
 */
export function creerClientComptesTiersHttp(appeler: typeof fetch = (entree, options) => fetch(entree, options)): ClientComptesTiers {
  /** Appel dont la réponse `attendu` est convertie (corps JSON, sauf `null` : aucun corps attendu). */
  async function lire<T>(
    entree: string,
    methode: 'GET' | 'POST' | 'DELETE',
    jeton: string,
    attendu: number,
    convertir: ((corps: unknown) => T | null) | null,
  ): Promise<ReponseTiers<T>> {
    try {
      const reponse = await appeler(entree, { method: methode, headers: { Authorization: `Bearer ${jeton}`, Accept: 'application/json' } });
      if (reponse.status === 401) {
        return NON_AUTHENTIFIE;
      }
      if (reponse.status === 503) {
        return INDISPONIBLE;
      }
      if (reponse.status !== attendu) {
        return TECHNIQUE;
      }
      if (convertir === null) {
        return { statut: 'ok', donnees: null as T };
      }
      const donnees = convertir(await reponse.json());
      return donnees === null ? TECHNIQUE : { statut: 'ok', donnees };
    } catch {
      return TECHNIQUE;
    }
  }

  return {
    lireEtat: (jeton) => lire(CHEMIN_POWENS, 'GET', jeton, 200, etatDepuis),
    connecter: (jeton) =>
      lire(CHEMIN_CONNEXION, 'POST', jeton, 200, (corps) => {
        const adresse = enregistrement(corps)?.adresse;
        return typeof adresse === 'string' && adresse !== '' ? adresse : null;
      }),
    selectionner: (jeton, compteId) =>
      lire(`${CHEMIN_POWENS}/comptes/${encodeURIComponent(String(compteId))}/selection`, 'POST', jeton, 200, etatDepuis),
    rafraichir: (jeton) => lire(CHEMIN_RAFRAICHISSEMENT, 'POST', jeton, 200, etatDepuis),
    deconnecter: (jeton) => lire<null>(CHEMIN_CONNEXION, 'DELETE', jeton, 204, null),
  };
}
