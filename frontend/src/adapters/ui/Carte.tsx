import type { ReactNode } from 'react';
import './Carte.css';

interface Proprietes {
  children: ReactNode;
  /** Carte qui tient lieu d'image (graphique) : `role="img"`, avec son nom accessible. */
  image?: { nom: string };
}

/** Carte : fond surface, bordure fine, angles vifs, sans ombre. */
export function Carte({ children, image }: Proprietes) {
  return image === undefined ? (
    <div className="carte">{children}</div>
  ) : (
    <div className="carte" role="img" aria-label={image.nom}>
      {children}
    </div>
  );
}
