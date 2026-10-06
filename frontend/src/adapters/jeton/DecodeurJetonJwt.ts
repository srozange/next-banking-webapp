import type { DecodeurJeton } from '../../application/seConnecter';

/** Décode un segment base64url (sans remplissage) en texte UTF-8. */
function decoderBase64Url(segment: string): string {
  const base64 = segment.replace(/-/g, '+').replace(/_/g, '/');
  const complete = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
  const binaire = atob(complete);
  return new TextDecoder().decode(Uint8Array.from(binaire, (caractere) => caractere.charCodeAt(0)));
}

/**
 * Lit la revendication `roles` d'un JWT (segment 2, base64url, JSON), sans vérifier la signature :
 * chaque service la vérifie à chaque appel. Rend null pour un jeton illisible ou sans liste de rôles.
 */
export function rolesDuJeton(jeton: string): string[] | null {
  const segments = jeton.split('.');
  if (segments.length !== 3) {
    return null;
  }
  try {
    const revendications: unknown = JSON.parse(decoderBase64Url(segments[1] as string));
    const roles: unknown = (revendications as { roles?: unknown } | null)?.roles;
    if (Array.isArray(roles) && roles.every((role): role is string => typeof role === 'string')) {
      return roles;
    }
    return null;
  } catch {
    return null;
  }
}

export const decodeurJetonJwt: DecodeurJeton = { roles: rolesDuJeton };
