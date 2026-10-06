import type { ReactNode } from 'react';
import './Carte.css';

interface Proprietes {
  children: ReactNode;
  /** Carte qui tient lieu d'image (graphique) : `role="img"`, avec son nom accessible. */
  image?: { nom: string };
  /** Variante `card-muted` : fond surface-alt. */
  attenuee?: boolean;
}

/** Carte : fond surface, bordure fine, angles vifs, sans ombre. */
export function Carte({ children, image, attenuee = false }: Proprietes) {
  const classe = attenuee ? 'carte carte--attenuee' : 'carte';
  return image === undefined ? (
    <div className={classe}>{children}</div>
  ) : (
    <div className={classe} role="img" aria-label={image.nom}>
      {children}
    </div>
  );
}
