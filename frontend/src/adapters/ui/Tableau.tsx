import type { ReactNode } from 'react';
import './Tableau.css';

interface Proprietes {
  testId: string;
  /** En-têtes de colonnes, toujours affichés, même au-dessus d'un corps vide. */
  colonnes: readonly string[];
  children: ReactNode;
}

/** Tableau de données : en-têtes de colonnes (scope="col"), lignes passées en enfants (`<tr>` de `<td>`). */
export function Tableau({ testId, colonnes, children }: Proprietes) {
  return (
    <div className="tableau">
      <table className="tableau__table" data-testid={testId}>
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
      </table>
    </div>
  );
}
