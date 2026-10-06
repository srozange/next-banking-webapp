/** Horloge injectable des adaptateurs : seule lecture du temps du front, remplaçable par une horloge simulée. */
export interface Horloge {
  /** Millisecondes depuis l'epoch. */
  maintenant(): number;
}

export const horlogeSysteme: Horloge = { maintenant: () => Date.now() };
