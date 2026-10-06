import type { ClientProduits, ReponseProduits } from '../../application/consulterProduits';
import {
  FORMAT_INSTANT,
  FORMAT_TAUX,
  type Catalogue,
  type ProduitCredit,
  type ProduitEpargne,
  type ProduitInvestissement,
  type Traitement,
} from '../../domain/produits';

export const CHEMIN_CATALOGUE = '/api/open-banking-conseiller/produits';
export const CHEMIN_TRAITEMENTS = '/api/coeur-bancaire/traitements';
export const LIMITE_TRAITEMENTS = 50;

const TECHNIQUE = { statut: 'technique' } as const;
const NON_AUTHENTIFIE = { statut: 'nonAuthentifie' } as const;
const MONTANT = /^\d+\.\d{2}$/;

type Champs = Record<string, unknown>;

const enregistrement = (valeur: unknown): Champs | null =>
  typeof valeur === 'object' && valeur !== null && !Array.isArray(valeur) ? (valeur as Champs) : null;

const tousPresents = <T>(liste: readonly (T | null)[]): liste is T[] => liste.every((element) => element !== null);

const absent = (valeur: unknown): valeur is null | undefined => valeur === null || valeur === undefined;
const texte = (valeur: unknown): valeur is string => typeof valeur === 'string';
const entier = (valeur: unknown): valeur is number => Number.isInteger(valeur);
const decimal = (format: RegExp) => (valeur: unknown): valeur is string => texte(valeur) && format.test(valeur);

/** Champ facultatif : absent (null), ou valide selon `valide`. */
const ouAbsent = <T>(valide: (valeur: unknown) => valeur is T) => (valeur: unknown): valeur is T | null | undefined =>
  absent(valeur) || valide(valeur);

const taux = ouAbsent(decimal(FORMAT_TAUX));
const montant = ouAbsent(decimal(MONTANT));
const entierFacultatif = ouAbsent(entier);
const texteFacultatif = ouAbsent(texte);
const instant = ouAbsent(decimal(FORMAT_INSTANT));

const ou = <T>(valeur: T | null | undefined): T | null => valeur ?? null;

function epargneDepuis(brut: unknown): ProduitEpargne | null {
  const c = enregistrement(brut);
  if (
    c === null || !entier(c.id) || !texte(c.nom) || !texteFacultatif(c.description) || !taux(c.taux) ||
    !montant(c.depotMin) || typeof c.capitalGaranti !== 'boolean' || !entierFacultatif(c.dureeMaxMois)
  ) {
    return null;
  }
  return {
    id: c.id, nom: c.nom, description: ou(c.description), taux: ou(c.taux), depotMin: ou(c.depotMin),
    capitalGaranti: c.capitalGaranti, dureeMaxMois: ou(c.dureeMaxMois),
  };
}

function creditDepuis(brut: unknown): ProduitCredit | null {
  const c = enregistrement(brut);
  if (
    c === null || !entier(c.id) || !texte(c.nom) || !texteFacultatif(c.description) || !taux(c.taux) ||
    !entierFacultatif(c.dureeMaxMois) || !taux(c.ltvMax) || typeof c.assuranceRequise !== 'boolean'
  ) {
    return null;
  }
  return {
    id: c.id, nom: c.nom, description: ou(c.description), taux: ou(c.taux), dureeMaxMois: ou(c.dureeMaxMois),
    ltvMax: ou(c.ltvMax), assuranceRequise: c.assuranceRequise,
  };
}

function investissementDepuis(brut: unknown): ProduitInvestissement | null {
  const c = enregistrement(brut);
  if (
    c === null || !entier(c.id) || !texte(c.nom) || !texteFacultatif(c.description) || !texteFacultatif(c.secteur) ||
    !entierFacultatif(c.niveauRisque) || !taux(c.rendementAttendu)
  ) {
    return null;
  }
  return {
    id: c.id, nom: c.nom, description: ou(c.description), secteur: ou(c.secteur), niveauRisque: ou(c.niveauRisque),
    rendementAttendu: ou(c.rendementAttendu),
  };
}

/** Liste dont chaque élément est converti ; un seul élément illisible rend le tout illisible. */
function listeDepuis<T>(brut: unknown, convertir: (element: unknown) => T | null): T[] | null {
  const elements = Array.isArray(brut) ? brut.map(convertir) : null;
  return elements !== null && tousPresents(elements) ? elements : null;
}

function catalogueDepuis(brut: unknown): Catalogue | null {
  const c = enregistrement(brut);
  if (c === null) {
    return null;
  }
  const epargne = listeDepuis(c.epargne, epargneDepuis);
  const credit = listeDepuis(c.credit, creditDepuis);
  const investissement = listeDepuis(c.investissement, investissementDepuis);
  return epargne === null || credit === null || investissement === null ? null : { epargne, credit, investissement };
}

function traitementDepuis(brut: unknown): Traitement | null {
  const c = enregistrement(brut);
  if (
    c === null || !texte(c.job) || !instant(c.debut) || !instant(c.fin) || !entier(c.dureeMs) ||
    !entierFacultatif(c.enregistrements) || !texteFacultatif(c.statut) || !texteFacultatif(c.details)
  ) {
    return null;
  }
  return {
    job: c.job, debut: ou(c.debut), fin: ou(c.fin), dureeMs: c.dureeMs, enregistrements: ou(c.enregistrements),
    statut: ou(c.statut), details: ou(c.details),
  };
}

/**
 * Adaptateur HTTP du catalogue (open-banking-conseiller) et du journal des traitements (cœur bancaire), par
 * `fetchAuthentifie` (tout 401 supprime déjà le jeton). 401 : non authentifié ; tout le reste (autre statut, réseau
 * coupé, corps illisible ou hors contrat, taux hors format décimal) : erreur technique. Le jeton n'est jamais journalisé.
 */
export function creerClientProduitsHttp(appeler: typeof fetch = (entree, options) => fetch(entree, options)): ClientProduits {
  async function lire<T>(entree: string, jeton: string, convertir: (corps: unknown) => T | null): Promise<ReponseProduits<T>> {
    try {
      const reponse = await appeler(entree, {
        method: 'GET',
        headers: { Authorization: `Bearer ${jeton}`, Accept: 'application/json' },
      });
      if (reponse.status === 401) {
        return NON_AUTHENTIFIE;
      }
      if (reponse.status !== 200) {
        return TECHNIQUE;
      }
      const donnees = convertir(await reponse.json());
      return donnees === null ? TECHNIQUE : { statut: 'ok', donnees };
    } catch {
      return TECHNIQUE;
    }
  }

  return {
    lireCatalogue: (jeton) => lire(CHEMIN_CATALOGUE, jeton, catalogueDepuis),
    lireTraitements: (jeton) =>
      lire(`${CHEMIN_TRAITEMENTS}?limite=${LIMITE_TRAITEMENTS}`, jeton, (corps) => listeDepuis(corps, traitementDepuis)),
  };
}
