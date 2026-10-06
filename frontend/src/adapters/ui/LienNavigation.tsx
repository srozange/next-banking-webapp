import type { ReactNode } from 'react';
import { NavLink } from 'react-router';
import './LienNavigation.css';

interface Proprietes {
  to: string;
  /** en-tete : texte ink, lien courant souligné primary ; sous-en-tete : texte et soulignement on-primary. */
  variante: 'en-tete' | 'sous-en-tete';
  id?: string;
  'data-testid': string;
  children: ReactNode;
}

/** Lien de navigation applicative : aria-current="page" sur la route exacte, soulignement 2px. */
export function LienNavigation({ to, variante, id, 'data-testid': testId, children }: Proprietes) {
  return (
    <NavLink to={to} end id={id} className={`lien-navigation lien-navigation--${variante}`} data-testid={testId}>
      {children}
    </NavLink>
  );
}
