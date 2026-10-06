/**
 * Règles pures de la conversation avec Sophie (FEAT-010) : l'historique est tenu par le front, sans état serveur.
 */

export type Role = 'utilisateur' | 'assistant';

/** Un message de la conversation, sans le message de bienvenue (affiché mais jamais envoyé). */
export interface Echange {
  role: Role;
  contenu: string;
}

/** Prénom pour la bienvenue (FR-010-02) : premier mot du nom complet ; sans nom exploitable, null. */
export function prenomDe(nomComplet: string | null): string | null {
  if (nomComplet === null) {
    return null;
  }
  const premier = nomComplet.trim().split(/\s+/)[0];
  return premier === '' ? null : (premier as string);
}

/** Saisie à ignorer (FR-010-05) : vide ou faite uniquement de blancs. */
export function saisieVide(saisie: string): boolean {
  return saisie.trim() === '';
}
