import type { ReactNode } from 'react';
import './Carte.css';

interface Proprietes {
  children: ReactNode;
  /** `muted` : carte discrète sur le fond alternatif, pour un détail sous une liste. */
  variante?: 'muted';
  /** Carte qui tient lieu d'image (graphique) : `role="img"`, avec son nom accessible. */
  image?: { nom: string };
  /** Variante `card-muted` : fond surface-alt. */
  attenuee?: boolean;
}

/** Carte : fond surface, bordure fine, angles vifs, sans ombre ; variante `card-muted` sur le fond alternatif. */
export function Carte({ children, variante, image, attenuee = false }: Proprietes) {
  const classe = attenuee ? 'carte carte--attenuee' : variante === 'muted' ? 'carte card-muted' : 'carte';
  return image === undefined ? (
    <div className={classe}>{children}</div>
  ) : (
    <div className={classe} role="img" aria-label={image.nom}>
      {children}
    </div>
  );
}
