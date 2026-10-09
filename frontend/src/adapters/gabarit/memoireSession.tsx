import { createContext, useContext, useRef, type ReactNode } from 'react';

/**
 * Mémoire de la session de navigation : promesses rangées par clé, gardées tant que le gabarit authentifié reste
 * monté (il se démonte à la déconnexion ou à l'expiration). Jamais de stockage navigateur. Garder la promesse, et non
 * son résultat, évite un second appel quand l'écran se monte deux fois de suite (mode strict).
 */
export interface MemoireSession {
  /** Promesse déjà rangée sous `cle`, sinon celle de `charger()`, rangée pour les appels suivants. */
  obtenir<T>(cle: string, charger: () => Promise<T>): Promise<T>;
  /** Oublie la promesse rangée sous `cle` : le prochain `obtenir` recharge. */
  oublier(cle: string): void;
}

const ContexteMemoire = createContext<MemoireSession | null>(null);

function creerMemoireSession(): MemoireSession {
  const promesses = new Map<string, Promise<unknown>>();
  return {
    obtenir<T>(cle: string, charger: () => Promise<T>): Promise<T> {
      const rangee = promesses.get(cle) as Promise<T> | undefined;
      if (rangee !== undefined) {
        return rangee;
      }
      const promesse = charger();
      promesses.set(cle, promesse);
      return promesse;
    },
    oublier(cle) {
      promesses.delete(cle);
    },
  };
}

/** Fournit une mémoire neuve, vivant le temps du composant qui la porte. */
export function FournisseurMemoireSession({ children }: { children: ReactNode }) {
  const memoire = useRef<MemoireSession | null>(null);
  memoire.current ??= creerMemoireSession();
  return <ContexteMemoire value={memoire.current}>{children}</ContexteMemoire>;
}

/** Mémoire de la session ; lève une erreur hors d'un `FournisseurMemoireSession`. */
export function useMemoireSession(): MemoireSession {
  const memoire = useContext(ContexteMemoire);
  if (memoire === null) {
    throw new Error('useMemoireSession hors de FournisseurMemoireSession');
  }
  return memoire;
}
