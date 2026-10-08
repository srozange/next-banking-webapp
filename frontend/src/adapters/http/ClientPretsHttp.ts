import type { Echeance, LigneEcheancier, Pret, SaisieDemande, SaisieSimulation, Simulation, StatutPret, TypePret } from '../../domain/pret';
import type { ClientPrets, ReponseAction, ReponseDepot, ReponseEcheances, ReponsePrets, ReponseSimulation } from '../../application/prets';

export const CHEMIN_SIMULATIONS = '/api/credits/simulations';
export const CHEMIN_PRETS = '/api/credits/prets';

const TYPES: readonly string[] = ['IMMOBILIER', 'AUTO', 'CONSOMMATION', 'TRAVAUX', 'ETUDIANT'];
const STATUTS: readonly string[] = ['EN_ATTENTE', 'EN_COURS', 'REMBOURSE', 'REFUSE', 'DEFAUT'];
const MONTANT = /^-?\d+\.\d{2}$/;
const TAUX = /^\d+(\.\d+)?$/;

type Objet = Record<string, unknown>;

const estObjet = (valeur: unknown): valeur is Objet => typeof valeur === 'object' && valeur !== null;
const estMontant = (valeur: unknown): valeur is string => typeof valeur === 'string' && MONTANT.test(valeur);

function ligne(valeur: unknown): LigneEcheancier | null {
  if (!estObjet(valeur) || !Number.isInteger(valeur.numero)) {
    return null;
  }
  const { numero, capital, interets, total, capitalRestant } = valeur;
  return estMontant(capital) && estMontant(interets) && estMontant(total) && estMontant(capitalRestant)
    ? { numero: numero as number, capital, interets, total, capitalRestant }
    : null;
}

function simulation(corps: unknown): Simulation | null {
  if (!estObjet(corps) || !Array.isArray(corps.echeancier)) {
    return null;
  }
  const { mensualite, coutTotal, totalInterets } = corps;
  const lignes = corps.echeancier.map(ligne);
  if (!estMontant(mensualite) || !estMontant(coutTotal) || !estMontant(totalInterets) || lignes.includes(null)) {
    return null;
  }
  return { mensualite, coutTotal, totalInterets, echeancier: lignes as LigneEcheancier[] };
}

function pret(valeur: unknown): Pret | null {
  if (!estObjet(valeur)) {
    return null;
  }
  const { id, type, montant, tauxAnnuel, dureeMois, mensualite, coutTotal, totalInterets, objet, statut, dateDebut, dateFin } = valeur;
  const valide =
    Number.isInteger(id) &&
    Number.isInteger(dureeMois) &&
    typeof type === 'string' &&
    TYPES.includes(type) &&
    typeof statut === 'string' &&
    STATUTS.includes(statut) &&
    estMontant(montant) &&
    estMontant(mensualite) &&
    estMontant(coutTotal) &&
    estMontant(totalInterets) &&
    typeof tauxAnnuel === 'string' &&
    TAUX.test(tauxAnnuel) &&
    (objet === null || typeof objet === 'string') &&
    typeof dateDebut === 'string' &&
    typeof dateFin === 'string';
  return valide
    ? {
        id: id as number,
        type: type as TypePret,
        montant,
        tauxAnnuel,
        dureeMois: dureeMois as number,
        mensualite,
        coutTotal,
        totalInterets,
        objet,
        statut: statut as StatutPret,
        dateDebut,
        dateFin,
      }
    : null;
}

const DATE = /^\d{4}-\d{2}-\d{2}$/;

function echeance(valeur: unknown): Echeance | null {
  if (!estObjet(valeur)) {
    return null;
  }
  const { id, numero, dateEcheance, capital, interets, total, capitalRestant, paye, datePaiement } = valeur;
  const valide =
    Number.isInteger(id) &&
    Number.isInteger(numero) &&
    typeof dateEcheance === 'string' &&
    DATE.test(dateEcheance) &&
    estMontant(capital) &&
    estMontant(interets) &&
    estMontant(total) &&
    estMontant(capitalRestant) &&
    typeof paye === 'boolean' &&
    (datePaiement === null || typeof datePaiement === 'string');
  return valide
    ? { id: id as number, numero: numero as number, dateEcheance, capital, interets, total, capitalRestant, paye, datePaiement }
    : null;
}

function entetes(avecCorps: boolean): Record<string, string> {
  const communs = { Accept: 'application/json' };
  return avecCorps ? { ...communs, 'Content-Type': 'application/json' } : communs;
}

/**
 * Adaptateur HTTP du service credits. Simulation : 200 (simulation lisible) ; 400 `{"message"}` : refus avec le
 * message du service ; 401 : non authentifié ; tout le reste (autre statut, réseau coupé, JSON illisible ou de forme
 * inattendue) : erreur technique. Dépôt : 201 (prêt lisible) ; 400 et 503 `{"message"}` : refus avec le message du
 * service ; 401 ; tout le reste : erreur technique. Liste : 200 (tableau de prêts lisibles) ; 401 ; sinon erreur technique. Le jeton
 * n'est jamais journalisé.
 */
export const CHEMIN_ECHEANCES = '/api/credits/echeances';

export function creerClientPretsHttp(appeler: typeof fetch = (entree, options) => fetch(entree, options)): ClientPrets {
  return {
    async simuler(saisie: SaisieSimulation): Promise<ReponseSimulation> {
      try {
        const reponse = await appeler(CHEMIN_SIMULATIONS, {
          method: 'POST',
          headers: entetes(true),
          body: JSON.stringify(saisie),
        });
        if (reponse.status === 401) {
          return { statut: 'nonAuthentifie' };
        }
        const corps: unknown = await reponse.json();
        if (reponse.status === 400) {
          const message = estObjet(corps) ? corps.message : undefined;
          return typeof message === 'string' ? { statut: 'refusee', message } : { statut: 'technique' };
        }
        const lue = reponse.status === 200 ? simulation(corps) : null;
        return lue === null ? { statut: 'technique' } : { statut: 'simulation', simulation: lue };
      } catch {
        return { statut: 'technique' };
      }
    },

    async deposer(saisie: SaisieDemande): Promise<ReponseDepot> {
      try {
        const reponse = await appeler(CHEMIN_PRETS, {
          method: 'POST',
          headers: entetes(true),
          body: JSON.stringify(saisie),
        });
        if (reponse.status === 401) {
          return { statut: 'nonAuthentifie' };
        }
        const corps: unknown = await reponse.json();
        if (reponse.status === 400 || reponse.status === 503) {
          const message = estObjet(corps) ? corps.message : undefined;
          return typeof message === 'string' ? { statut: 'refusee', message } : { statut: 'technique' };
        }
        const lu = reponse.status === 201 ? pret(corps) : null;
        return lu === null ? { statut: 'technique' } : { statut: 'deposee', pret: lu };
      } catch {
        return { statut: 'technique' };
      }
    },

    async lister(): Promise<ReponsePrets> {
      try {
        const reponse = await appeler(CHEMIN_PRETS, { method: 'GET', headers: entetes(false) });
        if (reponse.status === 401) {
          return { statut: 'nonAuthentifie' };
        }
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        const prets = Array.isArray(corps) ? corps.map(pret) : [null];
        return prets.includes(null) ? { statut: 'technique' } : { statut: 'prets', prets: prets as Pret[] };
      } catch {
        return { statut: 'technique' };
      }
    },

    async echeances(pretId: number): Promise<ReponseEcheances> {
      try {
        const reponse = await appeler(`${CHEMIN_PRETS}/${pretId}/echeances`, { method: 'GET', headers: entetes(false) });
        if (reponse.status === 401) {
          return { statut: 'nonAuthentifie' };
        }
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        const echeances = Array.isArray(corps) ? corps.map(echeance) : [null];
        return echeances.includes(null) ? { statut: 'technique' } : { statut: 'echeances', echeances: echeances as Echeance[] };
      } catch {
        return { statut: 'technique' };
      }
    },

    async payer(echeanceId: number): Promise<ReponseAction> {
      try {
        const reponse = await appeler(`${CHEMIN_ECHEANCES}/${echeanceId}/paiement`, { method: 'POST', headers: entetes(false) });
        if (reponse.status === 401) {
          return { statut: 'nonAuthentifie' };
        }
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        return echeance(corps) === null ? { statut: 'technique' } : { statut: 'faite' };
      } catch {
        return { statut: 'technique' };
      }
    },

    async supprimer(pretId: number): Promise<ReponseAction> {
      try {
        const reponse = await appeler(`${CHEMIN_PRETS}/${pretId}`, { method: 'DELETE', headers: entetes(false) });
        if (reponse.status === 401) {
          return { statut: 'nonAuthentifie' };
        }
        return reponse.status === 204 ? { statut: 'faite' } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
  };
}
