import { useEffect, useRef, type KeyboardEvent, type ReactNode } from 'react';
import './Modale.css';

interface Proprietes {
  /** Identifiant du titre, relié par aria-labelledby. */
  idTitre: string;
  titre: string;
  testId: string;
  /** Fermeture demandée : touche Échap. */
  surFermeture: () => void;
  children: ReactNode;
}

const FOCALISABLES = 'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [href]';

/**
 * Fenêtre modale (role="dialog", aria-modal) sans élément dialog natif : le focus entre dans la fenêtre, y reste (piège du
 * focus, Tab et Maj+Tab bouclent) et la touche Échap la ferme. L'appelant rend le focus à son déclencheur.
 */
export function Modale({ idTitre, titre, testId, surFermeture, children }: Proprietes) {
  const fenetre = useRef<HTMLDivElement>(null);

  useEffect(() => {
    fenetre.current?.querySelector<HTMLElement>(FOCALISABLES)?.focus();
  }, []);

  function surTouche(evenement: KeyboardEvent<HTMLDivElement>) {
    if (evenement.key === 'Escape') {
      evenement.stopPropagation();
      surFermeture();
      return;
    }
    if (evenement.key !== 'Tab') {
      return;
    }
    const elements = Array.from(fenetre.current?.querySelectorAll<HTMLElement>(FOCALISABLES) ?? []);
    const premier = elements[0];
    const dernier = elements[elements.length - 1];
    if (evenement.shiftKey && document.activeElement === premier) {
      evenement.preventDefault();
      dernier?.focus();
    } else if (!evenement.shiftKey && document.activeElement === dernier) {
      evenement.preventDefault();
      premier?.focus();
    }
  }

  return (
    <div className="modale__fond">
      <div
        ref={fenetre}
        className="modale"
        role="dialog"
        aria-modal="true"
        aria-labelledby={idTitre}
        data-testid={testId}
        onKeyDown={surTouche}
      >
        <h2 id={idTitre} className="modale__titre">
          {titre}
        </h2>
        {children}
      </div>
    </div>
  );
}
