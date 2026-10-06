import type { ReactNode } from 'react';
import './BoutonPrimaire.css';

interface Proprietes {
  testId: string;
  /** Envoi en cours : bouton désactivé et aria-busy. */
  occupe?: boolean;
  children: ReactNode;
}

/** Bouton d'envoi principal, pleine largeur, 48px. */
export function BoutonPrimaire({ testId, occupe = false, children }: Proprietes) {
  return (
    <button
      type="submit"
      className="bouton-primaire"
      disabled={occupe}
      aria-busy={occupe ? 'true' : undefined}
      data-testid={testId}
    >
      {children}
    </button>
  );
}
