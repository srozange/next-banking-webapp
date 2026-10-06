import type { ReactNode, Ref } from 'react';
import './AlerteErreur.css';

interface Proprietes {
  testId: string;
  children: ReactNode;
  ref?: Ref<HTMLDivElement>;
}

/** Message d'erreur annoncé (role="alert"), focalisable par script. */
export function AlerteErreur({ testId, children, ref }: Proprietes) {
  return (
    <div ref={ref} className="alerte-erreur" role="alert" tabIndex={-1} data-testid={testId}>
      {children}
    </div>
  );
}
