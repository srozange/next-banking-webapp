import type { ReactNode } from 'react';
import './BoutonTexte.css';

interface Proprietes {
  'data-testid': string;
  children: ReactNode;
}

/** Bouton secondaire présenté comme un lien de navigation (typographie label, texte ink) ; son action arrive avec la story 2.5. */
export function BoutonTexte({ 'data-testid': testId, children }: Proprietes) {
  return (
    <button type="button" className="bouton-texte" data-testid={testId}>
      {children}
    </button>
  );
}
