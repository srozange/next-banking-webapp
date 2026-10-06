import { centimesDepuisSaisie, chaineDepuisCentimes } from '../domain/montant';
import { champsVides, comptesIdentiques, type ChampVirement, type SaisieVirement } from '../domain/virement';

/** Compte du client, tel que rendu par le service. */
export interface Compte {
  id: number;
  type: string;
  numero: string;
}

export interface CommandeVirement {
  compteSourceId: number;
  compteDestinataireId: number;
  /** Chaîne à point et deux décimales ; une saisie illisible est transmise telle quelle, le service la refuse. */
  montant: string;
  motif: string;
}

/** Réponse du service, traduite par l'adaptateur HTTP : `message` est celui de la réponse, ou null s'il manque. */
export type ReponseVirement = { statut: 'execute' } | { statut: 'erreur'; message: string | null };

export type ReponseComptes = { statut: 'comptes'; comptes: readonly Compte[] } | { statut: 'technique' };

/** Port sortant : comptes du client et soumission du virement. */
export interface ClientVirement {
  lireComptes(): Promise<ReponseComptes>;
  executer(commande: CommandeVirement): Promise<ReponseVirement>;
}

export type ResultatVirement =
  | { type: 'champs'; champs: ChampVirement[] }
  | { type: 'identiques' }
  | { type: 'execute' }
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
    return reponse.statut === 'execute' ? { type: 'execute' } : { type: 'erreur', message: reponse.message };
  } catch {
    return { type: 'erreur', message: null };
  }
}
