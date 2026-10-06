import type { ReactNode } from 'react';
import './Carte.css';

/** Carte : fond surface, bordure fine, angles vifs, sans ombre. */
export function Carte({ children }: { children: ReactNode }) {
  return <div className="carte">{children}</div>;
}
