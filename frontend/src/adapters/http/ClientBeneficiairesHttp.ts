import type {
  ClientBeneficiaires,
  ReponseAjout,
  ReponseBeneficiaires,
  ReponseSuppression,
  ReponseVerificationIban,
} from '../../application/gererBeneficiaires';
import { CHAMPS_BENEFICIAIRE, type BeneficiaireBrut, type ChampBeneficiaire, type VerificationIban } from '../../domain/beneficiaires';

export const CHEMIN_BENEFICIAIRES = '/api/coeur-bancaire/beneficiaires';

const estTexteOuNul = (valeur: unknown): valeur is string | null => valeur === null || valeur === undefined || typeof valeur === 'string';

const estBeneficiaire = (valeur: unknown): valeur is BeneficiaireBrut => {
  const b = valeur as Partial<Record<keyof BeneficiaireBrut, unknown>> | null;
  return (
    typeof b?.id === 'number' &&
    typeof b.libelle === 'string' &&
    typeof b.titulaire === 'string' &&
    typeof b.iban === 'string' &&
    estTexteOuNul(b.bic) &&
    estTexteOuNul(b.banque) &&
    typeof b.zoneSepa === 'boolean'
  );
};

/** BIC et banque absents du corps (champ omis) deviennent nuls. */
const normaliser = (b: BeneficiaireBrut): BeneficiaireBrut => ({ ...b, bic: b.bic ?? null, banque: b.banque ?? null });

const estChamp = (valeur: unknown): valeur is ChampBeneficiaire => CHAMPS_BENEFICIAIRE.includes(valeur as ChampBeneficiaire);

const JSON_ENTETES = { Accept: 'application/json', 'Content-Type': 'application/json' };

/** Résultat de vérification lu du corps ; nul s'il n'a pas la forme attendue. */
function lireVerification(corps: unknown): VerificationIban | null {
  const v = corps as { statut?: unknown; codePays?: unknown; zoneSepa?: unknown } | null;
  if (v?.statut === 'vide' || v?.statut === 'invalide') {
    return { statut: v.statut };
  }
  return v?.statut === 'valide' && typeof v.codePays === 'string' && typeof v.zoneSepa === 'boolean'
    ? { statut: 'valide', codePays: v.codePays, zoneSepa: v.zoneSepa }
    : null;
}

/**
 * Adaptateur HTTP des bénéficiaires (cœur bancaire). `appeler` porte le jeton et la règle du 401 (fetchAuthentifie).
 * Suppression : 204 supprimé ; 403 (bénéficiaire d'un autre client) et 404 (inexistant, déjà supprimé) refusée ;
 * tout autre statut et toute coupure, technique. Vérification : 200 avec statut vide, invalide ou valide, sinon technique.
 * Ajout : 201 ajouté ; 400 avec `message` refusé (le `champ` désigne le champ fautif) ; tout autre statut, technique. Liste : tout statut autre que 200, corps illisible ou coupure, technique.
 */
export function creerClientBeneficiairesHttp(appeler: typeof fetch): ClientBeneficiaires {
  return {
    async lister(): Promise<ReponseBeneficiaires> {
      try {
        const reponse = await appeler(CHEMIN_BENEFICIAIRES, { method: 'GET', headers: { Accept: 'application/json' } });
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        return Array.isArray(corps) && corps.every(estBeneficiaire) ? { statut: 'liste', beneficiaires: corps.map(normaliser) } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
    async supprimer(beneficiaireId): Promise<ReponseSuppression> {
      try {
        const reponse = await appeler(`${CHEMIN_BENEFICIAIRES}/${beneficiaireId}`, { method: 'DELETE', headers: { Accept: 'application/json' } });
        if (reponse.status === 204) {
          return { statut: 'supprime' };
        }
        return reponse.status === 403 || reponse.status === 404 ? { statut: 'refuse' } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
    async verifierIban(iban): Promise<ReponseVerificationIban> {
      try {
        const reponse = await appeler(`${CHEMIN_BENEFICIAIRES}/verification-iban`, { method: 'POST', headers: JSON_ENTETES, body: JSON.stringify({ iban }) });
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const verification = lireVerification(await reponse.json());
        return verification === null ? { statut: 'technique' } : { statut: 'verifiee', verification };
      } catch {
        return { statut: 'technique' };
      }
    },
    async ajouter(formulaire): Promise<ReponseAjout> {
      try {
        const reponse = await appeler(CHEMIN_BENEFICIAIRES, { method: 'POST', headers: JSON_ENTETES, body: JSON.stringify(formulaire) });
        if (reponse.status === 201) {
          return { statut: 'ajoute' };
        }
        if (reponse.status !== 400) {
          return { statut: 'technique' };
        }
        const corps = (await reponse.json()) as { message?: unknown; champ?: unknown } | null;
        return typeof corps?.message === 'string'
          ? { statut: 'refuse', message: corps.message, champ: estChamp(corps.champ) ? corps.champ : null }
          : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
  };
}
