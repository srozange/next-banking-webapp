import type { DecodeurJeton } from '../../application/seConnecter';

/** Décode un segment base64url (sans remplissage) en texte UTF-8. */
function decoderBase64Url(segment: string): string {
  const base64 = segment.replace(/-/g, '+').replace(/_/g, '/');
  const complete = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
  const binaire = atob(complete);
  return new TextDecoder().decode(Uint8Array.from(binaire, (caractere) => caractere.charCodeAt(0)));
}

/** Revendications d'un JWT (segment 2, base64url, JSON), sans vérifier la signature ; null si le jeton est illisible. */
function revendicationsDuJeton(jeton: string): Record<string, unknown> | null {
  const segments = jeton.split('.');
  if (segments.length !== 3) {
    return null;
  }
  try {
    const revendications: unknown = JSON.parse(decoderBase64Url(segments[1] as string));
    return revendications as Record<string, unknown> | null;
  } catch {
    return null;
  }
}

/**
 * Lit la revendication `roles` d'un JWT, sans vérifier la signature : chaque service la vérifie à chaque appel.
 * Rend null pour un jeton illisible ou sans liste de rôles.
 */
export function rolesDuJeton(jeton: string): string[] | null {
  const roles = revendicationsDuJeton(jeton)?.roles;
  return Array.isArray(roles) && roles.every((role): role is string => typeof role === 'string') ? roles : null;
}

/** Lit la revendication `exp` (secondes depuis l'epoch) ; null si le jeton est illisible ou `exp` n'est pas un nombre. */
export function expirationDuJeton(jeton: string): number | null {
  const exp = revendicationsDuJeton(jeton)?.exp;
  return typeof exp === 'number' && Number.isFinite(exp) ? exp : null;
}

export const decodeurJetonJwt: DecodeurJeton = { roles: rolesDuJeton };
