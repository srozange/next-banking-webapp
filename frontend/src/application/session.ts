/**
 * Port sortant : la session de l'utilisateur, vue de l'application. Tout ce qui la porte (stockage local, lecture
 * des rôles, calcul du temps restant) reste dans les adaptateurs.
 */
export interface SessionLocale {
  /** Vrai quand une session est en cours. */
  ouverte(): boolean;
  /** Rôles de la session ; null sans session. Une session illisible ou sans rôle connu est terminée par l'adaptateur. */
  roles(): readonly string[] | null;
  /** Termine la session locale. */
  terminer(): void;
  /** Secondes restantes avant la fin de la session ; null sans session ou si elle est illisible. */
  secondesRestantes(): number | null;
}

/** Sans session ouverte, la termine et rend faux ; sinon rend vrai. */
export function sessionOuverteOuFin(session: SessionLocale): boolean {
  if (session.ouverte()) {
    return true;
  }
  session.terminer();
  return false;
}
