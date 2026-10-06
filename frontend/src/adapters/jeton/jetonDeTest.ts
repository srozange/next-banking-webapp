/** Outil des tests : fabrique un JWT non signé (segments base64url sans remplissage). */
export function jetonDeTest(revendications: unknown): string {
  const segment = (valeur: unknown) =>
    btoa(String.fromCharCode(...new TextEncoder().encode(JSON.stringify(valeur))))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  return `${segment({ alg: 'RS256' })}.${segment(revendications)}.signature`;
}
