import type { ReactNode } from 'react';
import './BoutonPrimaire.css';

interface Proprietes {
  testId: string;
  /** `button` pour une action qui n'envoie aucun formulaire. */
  type?: 'submit' | 'button';
  /** Envoi en cours : bouton désactivé et aria-busy. */
  occupe?: boolean;
  children: ReactNode;
}

/** Bouton d'envoi principal, pleine largeur, 48px. */
export function BoutonPrimaire({ testId, type = 'submit', occupe = false, children }: Proprietes) {
  return (
    <button
      type={type}
      className="bouton-primaire"
      disabled={occupe}
      aria-busy={occupe ? 'true' : undefined}
      data-testid={testId}
    >
      {children}
    </button>
  );
}
