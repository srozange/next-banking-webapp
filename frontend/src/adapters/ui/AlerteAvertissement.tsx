import type { ReactNode, Ref } from 'react';
import './AlerteAvertissement.css';

interface Proprietes {
  testId: string;
  /** `status` : annonce polie (suspension) ; `alert` : annonce immédiate (refus pour erreur du contrôle). */
  role: 'status' | 'alert';
  children: ReactNode;
  ref?: Ref<HTMLDivElement>;
}

/** Avis d'avertissement (`alert-warning`, tokens warning), focalisable par script. */
export function AlerteAvertissement({ testId, role, children, ref }: Proprietes) {
  return (
    <div ref={ref} className="alerte-avertissement" role={role} tabIndex={-1} data-testid={testId}>
      {children}
    </div>
  );
}
