import type { ReactNode } from 'react';
import './AlerteInfo.css';

interface Proprietes {
  testId: string;
  children: ReactNode;
}

/** Message d'information annoncé poliment (role="status"). */
export function AlerteInfo({ testId, children }: Proprietes) {
  return (
    <div className="alerte-info" role="status" data-testid={testId}>
      {children}
    </div>
  );
}
