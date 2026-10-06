import type { ReactNode } from 'react';
import './LienEvitement.css';

interface Proprietes {
  /** Identifiant du contenu principal, focalisable (tabIndex -1), qui reçoit le focus à l'activation. */
  idCible: string;
  children: ReactNode;
}

/**
 * Lien d'évitement, premier élément au clavier, visible au focus. Le focus est posé par script :
 * le focus par ancre n'est fiable ni avec le routeur ni partout.
 */
export function LienEvitement({ idCible, children }: Proprietes) {
  return (
    <a
      className="lien-evitement"
      href={`#${idCible}`}
      data-testid="application-lien-evitement"
      onClick={(evenement) => {
        evenement.preventDefault();
        document.getElementById(idCible)?.focus();
      }}
    >
      {children}
    </a>
  );
}
