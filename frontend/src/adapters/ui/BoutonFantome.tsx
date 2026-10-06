import type { ReactNode } from 'react';
import './BoutonFantome.css';

interface Proprietes {
  'data-testid': string;
  children: ReactNode;
  onClick: () => void;
}

/** Bouton fantôme (composant `button-ghost`) : contour ink, fond transparent, 48 px de haut. */
export function BoutonFantome({ 'data-testid': testId, children, onClick }: Proprietes) {
  return (
    <button type="button" className="bouton-fantome" data-testid={testId} onClick={onClick}>
      {children}
    </button>
  );
}
