import type { MouseEvent, ReactNode } from 'react';
import './BoutonTexte.css';

interface Proprietes {
  'data-testid': string;
  children: ReactNode;
  /** Action du bouton ; sans elle, le bouton reste inerte. */
  onClick?: (evenement: MouseEvent<HTMLButtonElement>) => void;
  /** Infobulle, reprise comme nom accessible quand le texte du bouton est un simple symbole. */
  titre?: string;
  /** Désactive le bouton (action en cours). */
  desactive?: boolean;
}

/** Bouton secondaire présenté comme un lien de navigation (typographie label, texte ink) ; son action est portée par l'appelant. */
export function BoutonTexte({ 'data-testid': testId, children, onClick, titre, desactive }: Proprietes) {
  return (
    <button type="button" className="bouton-texte" data-testid={testId} onClick={onClick} title={titre} aria-label={titre} disabled={desactive}>
      {children}
    </button>
  );
}
