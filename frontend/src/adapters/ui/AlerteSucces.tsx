import type { ReactNode, Ref } from 'react';
import './AlerteSucces.css';

interface Proprietes {
  testId: string;
  children: ReactNode;
  ref?: Ref<HTMLDivElement>;
}

/** Confirmation d'une action (tokens success), annoncée poliment (role="status"), focalisable par script. */
export function AlerteSucces({ testId, children, ref }: Proprietes) {
  return (
    <div ref={ref} className="alerte-succes" role="status" tabIndex={-1} data-testid={testId}>
      {children}
    </div>
  );
}
