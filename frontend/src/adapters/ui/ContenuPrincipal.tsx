import type { ReactNode } from 'react';
import './ContenuPrincipal.css';

/** Identifiant du contenu principal, cible du lien d'évitement. */
export const ID_CONTENU_PRINCIPAL = 'contenu-principal';

interface Proprietes {
  /** Chargement en cours : aria-busy, sans squelette ni texte. */
  occupe?: boolean;
  children: ReactNode;
}

/** Contenu principal de la page (landmark main, data-testid figé), focalisable par le lien d'évitement. */
export function ContenuPrincipal({ occupe = false, children }: Proprietes) {
  return (
    <main
      id={ID_CONTENU_PRINCIPAL}
      className="contenu-principal"
      tabIndex={-1}
      aria-busy={occupe ? 'true' : undefined}
      data-testid="application-racine"
    >
      {children}
    </main>
  );
}
