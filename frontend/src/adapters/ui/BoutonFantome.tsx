import type { ReactNode } from 'react';
import './BoutonFantome.css';

interface Proprietes {
  'data-testid': string;
  children: ReactNode;
  onClick: () => void;
  /** Action en cours : `aria-disabled="true"`, le bouton reste focalisable et le clic est ignoré. */
  occupe?: boolean;
}

/** Bouton fantôme (composant `button-ghost`) : contour ink, fond transparent, 48 px de haut. */
export function BoutonFantome({ 'data-testid': testId, children, onClick, occupe = false }: Proprietes) {
  return (
    <button
      type="button"
      className="bouton-fantome"
      data-testid={testId}
      aria-disabled={occupe ? 'true' : undefined}
      onClick={() => {
        if (!occupe) {
          onClick();
        }
      }}
    >
      {children}
    </button>
  );
}
