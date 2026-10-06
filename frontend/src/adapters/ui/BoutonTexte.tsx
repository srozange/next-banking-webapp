import type { ReactNode } from 'react';
import './BoutonTexte.css';

interface Proprietes {
  'data-testid': string;
  children: ReactNode;
  /** Action du bouton ; sans elle, le bouton reste inerte. */
  onClick?: () => void;
}

/** Bouton secondaire présenté comme un lien de navigation (typographie label, texte ink) ; son action est portée par l'appelant. */
export function BoutonTexte({ 'data-testid': testId, children, onClick }: Proprietes) {
  return (
    <button type="button" className="bouton-texte" data-testid={testId} onClick={onClick}>
      {children}
    </button>
  );
}
