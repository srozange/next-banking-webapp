import type { ReactNode } from 'react';
import './Carte.css';

interface Proprietes {
  children: ReactNode;
  /** `muted` : carte discrète sur le fond alternatif, pour un détail sous une liste. */
  variante?: 'muted';
}

/** Carte : fond surface, bordure fine, angles vifs, sans ombre ; variante `card-muted` sur le fond alternatif. */
export function Carte({ children, variante }: Proprietes) {
  return <div className={variante === 'muted' ? 'carte card-muted' : 'carte'}>{children}</div>;
}
