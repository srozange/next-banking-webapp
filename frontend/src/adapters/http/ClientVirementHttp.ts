import type { ClientVirement, Compte, ReponseComptes, ReponseSuivi, ReponseVirement } from '../../application/virement';
import type { VirementSuivi } from '../../domain/virement';

export const CHEMIN_COMPTES = '/api/coeur-bancaire/comptes';
export const CHEMIN_VIREMENTS = '/api/coeur-bancaire/virements';
export const CHEMIN_SUIVI = '/api/coeur-bancaire/virements/suivi';

const ETATS_SUIVIS = ['EXECUTE', 'SUSPENDU', 'LIBERE', 'REJETE'];

const estVirementSuivi = (valeur: unknown): valeur is VirementSuivi => {
  const virement = valeur as Partial<VirementSuivi> | null;
  return (
    typeof virement?.virementId === 'number' &&
    typeof virement.soumisLe === 'string' &&
    typeof virement.compteDestinataire === 'string' &&
    typeof virement.montant === 'string' &&
    typeof virement.etat === 'string' &&
    ETATS_SUIVIS.includes(virement.etat) &&
    typeof virement.heureButoir === 'string'
  );
};

const estCompte = (valeur: unknown): valeur is Compte => {
  const compte = valeur as Partial<Compte> | null;
  return typeof compte?.id === 'number' && typeof compte.type === 'string' && typeof compte.numero === 'string' && typeof compte.solde === 'string' && /^-?\d+\.\d{2}$/.test(compte.solde);
};

/**
 * Adaptateur HTTP du virement (cœur bancaire). `appeler` porte le jeton et la règle du 401 (fetchAuthentifie).
 * L'en-tête Idempotency-Key (UUID) est créé à la première soumission et gardé tant qu'aucune réponse définitive
 * (2xx ou 4xx) n'est reçue : une nouvelle tentative après une coupure ou un 5xx rejoue la même soumission.
 */
export function creerClientVirementHttp(
  appeler: typeof fetch,
  genererCle: () => string = () => crypto.randomUUID(),
): ClientVirement {
  let cle: string | null = null;
  return {
    async lireComptes(): Promise<ReponseComptes> {
      try {
        const reponse = await appeler(CHEMIN_COMPTES, { method: 'GET', headers: { Accept: 'application/json' } });
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        return Array.isArray(corps) && corps.every(estCompte) ? { statut: 'comptes', comptes: corps } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },

    async executer(commande): Promise<ReponseVirement> {
      cle ??= genererCle();
      const motif = commande.motif === '' ? {} : { motif: commande.motif };
      try {
        const reponse = await appeler(CHEMIN_VIREMENTS, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Accept: 'application/json', 'Idempotency-Key': cle },
          body: JSON.stringify({
            compteSourceId: commande.compteSourceId,
            compteDestinataireId: commande.compteDestinataireId,
            montant: commande.montant,
            ...motif,
          }),
        });
        const corps = await lireCorps(reponse);
        if (reponse.status === 201) {
          cle = null;
          return lireIssue(corps);
        }
        // Un refus pour erreur du contrôle (référence de trace) est définitif : une nouvelle demande est une
        // nouvelle soumission, sous une nouvelle clé.
        if (typeof corps?.reference === 'string') {
          cle = null;
          return { statut: 'refuse', reference: corps.reference };
        }
        if (reponse.status < 500) {
          cle = null;
        }
        return { statut: 'erreur', message: typeof corps?.message === 'string' ? corps.message : null };
      } catch {
        return { statut: 'erreur', message: null };
      }
    },

    async lireSuivi(): Promise<ReponseSuivi> {
      try {
        const reponse = await appeler(CHEMIN_SUIVI, { method: 'GET', headers: { Accept: 'application/json' } });
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        return Array.isArray(corps) && corps.every(estVirementSuivi)
          ? { statut: 'suivi', virements: corps }
          : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
  };
}

type CorpsReponse = { message?: unknown; reference?: unknown; etat?: unknown; heureButoir?: unknown; montant?: unknown; compteDestinataireId?: unknown };

/** Réponse 201 : SUSPENDU avec son heure butoir, sinon exécuté. */
function lireIssue(corps: CorpsReponse | null): ReponseVirement {
  if (
    corps?.etat === 'SUSPENDU' &&
    typeof corps.heureButoir === 'string' &&
    typeof corps.montant === 'string' &&
    typeof corps.compteDestinataireId === 'number'
  ) {
    return {
      statut: 'suspendu',
      heureButoir: corps.heureButoir,
      montant: corps.montant,
      compteDestinataireId: corps.compteDestinataireId,
    };
  }
  return { statut: 'execute' };
}

async function lireCorps(reponse: Response): Promise<CorpsReponse | null> {
  try {
    const corps: unknown = await reponse.json();
    return typeof corps === 'object' ? (corps as CorpsReponse | null) : null;
  } catch {
    return null;
  }
}
