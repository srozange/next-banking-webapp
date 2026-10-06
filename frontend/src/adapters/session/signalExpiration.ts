/** Signal « la session est terminée » : émis par tout 401, écouté par le gabarit authentifié qui mène à /login. */
export interface SignalExpiration {
  declencher(): void;
  /** S'abonne ; rend la fonction de désabonnement. */
  ecouter(rappel: () => void): () => void;
}

export function creerSignalExpiration(): SignalExpiration {
  const rappels = new Set<() => void>();
  return {
    declencher() {
      [...rappels].forEach((rappel) => rappel());
    },
    ecouter(rappel) {
      rappels.add(rappel);
      return () => {
        rappels.delete(rappel);
      };
    },
  };
}
