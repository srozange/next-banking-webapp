import type { ReactNode } from 'react';
import './AlerteSucces.css';

interface Proprietes {
  testId: string;
  children: ReactNode;
}

/** Message de succès annoncé poliment (role="status"). */
export function AlerteSucces({ testId, children }: Proprietes) {
  return (
    <div className="alerte-succes" role="status" data-testid={testId}>
      {children}
    </div>
  );
}
