import { centimesDepuisChaine } from '../domain/montant';

/** Port sortant : fournit un montant au format JSON du back ("100.00"). */
export interface SourceMontant {
  lire(): Promise<string>;
}

/** Cas d'usage : lit un montant par le port et le rend en centimes entiers. */
export async function lireMontantEnCentimes(source: SourceMontant): Promise<number> {
  return centimesDepuisChaine(await source.lire());
}
