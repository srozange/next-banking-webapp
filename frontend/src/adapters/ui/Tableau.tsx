import type { ReactNode } from 'react';
import './Tableau.css';

interface Proprietes {
  testId?: string;
  /**
   * En-têtes de colonnes, toujours affichés, même au-dessus d'un corps vide ; les enfants sont alors les lignes
   * (`<tr>` de `<td>`). Sans colonnes, les enfants sont le contenu complet de la table (`<thead>`, `<tbody>`).
   */
  colonnes?: readonly string[];
  /** Nom accessible de la région défilante focalisable au clavier ; sans lui, pas de région. */
  libelle?: string;
  children: ReactNode;
}

/** Tableau de données : en-têtes de colonnes (scope="col") et cellules stylés ici, région défilante si libellée. */
export function Tableau({ testId, colonnes, libelle, children }: Proprietes) {
  const region = libelle === undefined ? {} : { role: 'region', 'aria-label': libelle, tabIndex: 0 };
  return (
    <div className="tableau" {...region}>
      <table className="tableau__table" data-testid={testId}>
        {colonnes === undefined ? (
          children
        ) : (
          <>
            <thead>
              <tr>
                {colonnes.map((colonne) => (
                  <th key={colonne} scope="col">
                    {colonne}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>{children}</tbody>
          </>
        )}
      </table>
    </div>
  );
}
