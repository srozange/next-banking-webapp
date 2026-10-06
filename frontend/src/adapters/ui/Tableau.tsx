import type { ReactNode } from 'react';
import './Tableau.css';

interface Proprietes {
  /** Nom accessible de la région défilante. */
  libelle: string;
  children: ReactNode;
}

/** Tableau de données dans une région défilante focalisable au clavier ; en-têtes et cellules stylés ici. */
export function Tableau({ libelle, children }: Proprietes) {
  return (
    <div className="tableau" role="region" aria-label={libelle} tabIndex={0}>
      <table className="tableau__table">{children}</table>
    </div>
  );
}
