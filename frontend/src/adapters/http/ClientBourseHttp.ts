import type { ClientBourse, DemandeAjout, ReponseAjout, ReponseBourse } from '../../application/consulterBourse';
import type { HistoriqueCours, PointCotation, ValeurWatchlist } from '../../domain/bourse';

export const CHEMIN_WATCHLIST = '/api/bourse/watchlist';
export const CHEMIN_HISTORIQUE = '/api/bourse/historique';

const TECHNIQUE = { statut: 'technique' } as const;
const NON_AUTHENTIFIE = { statut: 'nonAuthentifie' } as const;
const PRIX = /^\d+(\.\d+)?$/;
const DATE = /^\d{4}-\d{2}-\d{2}$/;

const enregistrement = (valeur: unknown): Record<string, unknown> | null =>
  typeof valeur === 'object' && valeur !== null && !Array.isArray(valeur) ? (valeur as Record<string, unknown>) : null;

const tousPresents = <T>(liste: readonly (T | null)[]): liste is T[] => liste.every((element) => element !== null);

const prix = (valeur: unknown): valeur is string => typeof valeur === 'string' && PRIX.test(valeur);

function valeurDepuis(brut: unknown): ValeurWatchlist | null {
  const champs = enregistrement(brut);
  if (
    champs === null ||
    typeof champs.id !== 'number' ||
    typeof champs.symbole !== 'string' ||
    typeof champs.nom !== 'string' ||
    typeof champs.marche !== 'string' ||
    typeof champs.devise !== 'string'
  ) {
    return null;
  }
  return { id: champs.id, symbole: champs.symbole, nom: champs.nom, marche: champs.marche, devise: champs.devise };
}

function pointDepuis(brut: unknown): PointCotation | null {
  const champs = enregistrement(brut);
  if (
    champs === null ||
    typeof champs.date !== 'string' ||
    !DATE.test(champs.date) ||
    !prix(champs.ouverture) ||
    !prix(champs.haut) ||
    !prix(champs.bas) ||
    !prix(champs.cloture) ||
    typeof champs.volume !== 'number'
  ) {
    return null;
  }
  return {
    date: champs.date,
    ouverture: champs.ouverture,
    haut: champs.haut,
    bas: champs.bas,
    cloture: champs.cloture,
    volume: champs.volume,
  };
}

function historiqueDepuis(brut: unknown): HistoriqueCours | null {
  const champs = enregistrement(brut);
  if (
    champs === null ||
    typeof champs.symbole !== 'string' ||
    typeof champs.periode !== 'string' ||
    !Array.isArray(champs.points) ||
    (champs.graphique !== undefined && champs.graphique !== null && typeof champs.graphique !== 'string')
  ) {
    return null;
  }
  const points = champs.points.map(pointDepuis);
  if (!tousPresents(points)) {
    return null;
  }
  return {
    symbole: champs.symbole,
    periode: champs.periode,
    points,
    graphique: champs.graphique ?? null,
  };
}

/**
 * Adaptateur HTTP du service bourse, par `fetchAuthentifie` (tout 401 de tout service supprime déjà le jeton).
 * 401 : non authentifié ; ajout refusé (400 `{"message"}`) : refus et son texte ; tout le reste (autre statut,
 * réseau coupé, corps illisible ou inattendu) : erreur technique. Le jeton n'est jamais journalisé.
 */
export function creerClientBourseHttp(appeler: typeof fetch = (entree, options) => fetch(entree, options)): ClientBourse {
  const entetes = (jeton: string): Record<string, string> => ({ Authorization: `Bearer ${jeton}`, Accept: 'application/json' });

  /** Appel dont le corps JSON de la réponse `attendu` est converti ; le reste est une erreur technique. */
  async function lire<T>(
    entree: string,
    options: RequestInit,
    attendu: number,
    convertir: (corps: unknown) => T | null,
  ): Promise<ReponseBourse<T>> {
    try {
      const reponse = await appeler(entree, options);
      if (reponse.status === 401) {
        return NON_AUTHENTIFIE;
      }
      if (reponse.status !== attendu) {
        return TECHNIQUE;
      }
      const donnees = convertir(await reponse.json());
      return donnees === null ? TECHNIQUE : { statut: 'ok', donnees };
    } catch {
      return TECHNIQUE;
    }
  }

  return {
    lireWatchlist: (jeton) =>
      lire(CHEMIN_WATCHLIST, { method: 'GET', headers: entetes(jeton) }, 200, (corps) => {
        const valeurs = Array.isArray(corps) ? corps.map(valeurDepuis) : null;
        return valeurs !== null && tousPresents(valeurs) ? valeurs : null;
      }),

    lireHistorique: (jeton, symbole, periode) =>
      lire(
        `${CHEMIN_HISTORIQUE}?symbole=${encodeURIComponent(symbole)}&periode=${encodeURIComponent(periode)}`,
        { method: 'GET', headers: entetes(jeton) },
        200,
        historiqueDepuis,
      ),

    async ajouter(jeton: string, demande: DemandeAjout): Promise<ReponseAjout> {
      try {
        const reponse = await appeler(CHEMIN_WATCHLIST, {
          method: 'POST',
          headers: { ...entetes(jeton), 'Content-Type': 'application/json' },
          body: JSON.stringify(demande),
        });
        if (reponse.status === 401) {
          return NON_AUTHENTIFIE;
        }
        if (reponse.status === 400) {
          const message = enregistrement(await reponse.json())?.message;
          return typeof message === 'string' && message !== '' ? { statut: 'refusee', message } : TECHNIQUE;
        }
        if (reponse.status !== 201) {
          return TECHNIQUE;
        }
        const valeur = valeurDepuis(await reponse.json());
        return valeur === null ? TECHNIQUE : { statut: 'ok', donnees: valeur };
      } catch {
        return TECHNIQUE;
      }
    },

    async retirer(jeton, id) {
      try {
        const reponse = await appeler(`${CHEMIN_WATCHLIST}/${encodeURIComponent(String(id))}`, {
          method: 'DELETE',
          headers: entetes(jeton),
        });
        if (reponse.status === 401) {
          return NON_AUTHENTIFIE;
        }
        // 404 : déjà retirée (par un autre utilisateur), la watchlist relue le montrera.
        return reponse.status === 204 || reponse.status === 404 ? { statut: 'ok', donnees: null } : TECHNIQUE;
      } catch {
        return TECHNIQUE;
      }
    },
  };
}
