import type { ReactNode } from 'react';
import './BoutonDepliant.css';

interface Proprietes {
  testId: string;
  /** Identifiant du bloc déplié ou replié. */
  controle: string;
  ouvert: boolean;
  onBasculer: () => void;
  children: ReactNode;
}

/** Bouton présenté comme un lien, qui déplie ou replie un bloc (aria-expanded, aria-controls). */
export function BoutonDepliant({ testId, controle, ouvert, onBasculer, children }: Proprietes) {
  return (
    <button
      type="button"
      className="bouton-depliant"
      aria-expanded={ouvert}
      aria-controls={controle}
      onClick={onBasculer}
      data-testid={testId}
    >
      {children}
    </button>
  );
}
