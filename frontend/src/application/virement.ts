import { centimesDepuisSaisie, chaineDepuisCentimes } from '../domain/montant';
import {
  champsVides,
  comptesIdentiques,
  type ChampVirement,
  type SaisieVirement,
  type VirementSuivi,
} from '../domain/virement';

/** Compte du client, tel que rendu par le service. */
export interface Compte {
  id: number;
  type: string;
  numero: string;
  /** Solde actuel, chaîne à point et deux décimales ("2130.00"). */
  solde: string;
}

export interface CommandeVirement {
  compteSourceId: number;
  compteDestinataireId: number;
  /** Chaîne à point et deux décimales ; une saisie illisible est transmise telle quelle, le service la refuse. */
  montant: string;
  motif: string;
}

/**
 * Réponse du service, traduite par l'adaptateur HTTP : `suspendu` porte l'heure butoir (ISO-8601 UTC) et l'écho du
 * montant et du compte destinataire ; `refuse` est le refus pour erreur du contrôle, avec sa référence de trace ;
 * pour `erreur`, `message` est celui de la réponse, ou null s'il manque.
 */
export type ReponseVirement =
  | { statut: 'execute' }
  | { statut: 'suspendu'; heureButoir: string; montant: string; compteDestinataireId: number }
  | { statut: 'refuse'; reference: string }
  | { statut: 'erreur'; message: string | null };

export type ReponseComptes = { statut: 'comptes'; comptes: readonly Compte[] } | { statut: 'technique' };

export type ReponseSuivi = { statut: 'suivi'; virements: readonly VirementSuivi[] } | { statut: 'technique' };

/** Port sortant : comptes du client, soumission du virement et suivi de ses virements passés en revue. */
export interface ClientVirement {
  lireComptes(): Promise<ReponseComptes>;
  executer(commande: CommandeVirement): Promise<ReponseVirement>;
  lireSuivi(): Promise<ReponseSuivi>;
}

export type ResultatVirement =
  | { type: 'champs'; champs: ChampVirement[] }
  | { type: 'identiques' }
  | { type: 'execute' }
  | { type: 'suspendu'; heureButoir: string; montant: string; compteDestinataireId: number }
  | { type: 'refuse'; reference: string }
  | { type: 'erreur'; message: string | null };

/** Cas d'usage : comptes proposés, ou null si le service est indisponible. */
export async function chargerComptes(client: ClientVirement): Promise<readonly Compte[] | null> {
  try {
    const reponse = await client.lireComptes();
    return reponse.statut === 'comptes' ? reponse.comptes : null;
  } catch {
    return null;
  }
}

/**
 * Cas d'usage : contrôles locaux sans appel (champs obligatoires, comptes distincts), puis soumission directe,
 * sans confirmation (FR-003-08). Les autres contrôles appartiennent au service.
 */
export async function executerVirement(saisie: SaisieVirement, client: ClientVirement): Promise<ResultatVirement> {
  const champs = champsVides(saisie);
  if (champs.length > 0) {
    return { type: 'champs', champs };
  }
  if (comptesIdentiques(saisie)) {
    return { type: 'identiques' };
  }
  const centimes = centimesDepuisSaisie(saisie.montant);
  try {
    const reponse = await client.executer({
      compteSourceId: Number(saisie.compteSource),
      compteDestinataireId: Number(saisie.compteDestinataire),
      montant: centimes === null ? saisie.montant.trim() : chaineDepuisCentimes(centimes),
      motif: saisie.motif,
    });
    switch (reponse.statut) {
      case 'execute':
        return { type: 'execute' };
      case 'suspendu':
        return {
          type: 'suspendu',
          heureButoir: reponse.heureButoir,
          montant: reponse.montant,
          compteDestinataireId: reponse.compteDestinataireId,
        };
      case 'refuse':
        return { type: 'refuse', reference: reponse.reference };
      case 'erreur':
        return { type: 'erreur', message: reponse.message };
    }
  } catch {
    return { type: 'erreur', message: null };
  }
}

/** Cas d'usage : virements passés en revue du client, ou null si le service est indisponible. */
export async function chargerSuivi(client: ClientVirement): Promise<readonly VirementSuivi[] | null> {
  try {
    const reponse = await client.lireSuivi();
    return reponse.statut === 'suivi' ? reponse.virements : null;
  } catch {
    return null;
  }
}
