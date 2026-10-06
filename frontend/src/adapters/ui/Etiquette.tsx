import type { ReactNode } from 'react';
import './Etiquette.css';

interface Proprietes {
  'data-testid': string;
  children: ReactNode;
}

/** Étiquette (composant `tag`) : pilule, fond primary-container, typographie label. */
export function Etiquette({ 'data-testid': testId, children }: Proprietes) {
  return (
    <span className="etiquette" data-testid={testId}>
      {children}
    </span>
  );
}
