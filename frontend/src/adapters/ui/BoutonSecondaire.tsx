import type { ReactNode, Ref } from 'react';
import './BoutonSecondaire.css';

interface Proprietes {
  'data-testid': string;
  children: ReactNode;
  onClick: () => void;
  ref?: Ref<HTMLButtonElement>;
}

/** Bouton secondaire (composant `button-secondary`) : contour ink, fond transparent ; focalisable par script. */
export function BoutonSecondaire({ 'data-testid': testId, children, onClick, ref }: Proprietes) {
  return (
    <button ref={ref} type="button" className="bouton-secondaire" data-testid={testId} onClick={onClick}>
      {children}
    </button>
  );
}
