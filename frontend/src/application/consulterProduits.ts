import type { Catalogue, Traitement } from '../domain/produits';
import { sessionOuverteOuFin, type SessionLocale } from './session';

/** Réponse d'un appel à un service, traduite par l'adaptateur HTTP (jamais d'exception). */
export type ReponseProduits<T> = { statut: 'ok'; donnees: T } | { statut: 'nonAuthentifie' } | { statut: 'technique' };

/** Port sortant : catalogue (open-banking-conseiller) et journal des traitements nocturnes (cœur bancaire). */
export interface ClientProduits {
  lireCatalogue(): Promise<ReponseProduits<Catalogue>>;
  lireTraitements(): Promise<ReponseProduits<readonly Traitement[]>>;
}

/** Ce que montre l'écran ; une partie absente (service en panne) n'est pas affichée, la panne est annoncée. */
export interface EtatProduits {
  catalogue: Catalogue | null;
  traitements: readonly Traitement[] | null;
  technique: boolean;
}

export type ResultatProduits = { type: 'etat'; etat: EtatProduits } | { type: 'deconnecte' };

const DECONNECTE: ResultatProduits = { type: 'deconnecte' };

/**
 * Cas d'usage (FR-011-02) : lit le catalogue et le journal en parallèle, jamais en cascade ; sert aussi au
 * « Rafraichir ». Un 401 de l'un des deux termine la session (session terminée) ; la panne de l'un n'empêche pas
 * d'afficher l'autre.
 */
export async function chargerProduits(client: ClientProduits, session: SessionLocale): Promise<ResultatProduits> {
  if (!sessionOuverteOuFin(session)) {
    return DECONNECTE;
  }
  const [catalogue, traitements] = await Promise.all([client.lireCatalogue(), client.lireTraitements()]);
  if (catalogue.statut === 'nonAuthentifie' || traitements.statut === 'nonAuthentifie') {
    session.terminer();
    return DECONNECTE;
  }
  return {
    type: 'etat',
    etat: {
      catalogue: catalogue.statut === 'ok' ? catalogue.donnees : null,
      traitements: traitements.statut === 'ok' ? traitements.donnees : null,
      technique: catalogue.statut === 'technique' || traitements.statut === 'technique',
    },
  };
}
