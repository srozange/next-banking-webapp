import { describe, expect, it } from 'vitest';
import { decodeurJetonJwt, rolesDuJeton } from './DecodeurJetonJwt';
import { jetonDeTest } from './jetonDeTest';

describe('rolesDuJeton', () => {
  it('lit la revendication roles du jeton', () => {
    expect(rolesDuJeton(jetonDeTest({ sub: '3', roles: ['CLIENT'] }))).toEqual(['CLIENT']);
    expect(decodeurJetonJwt.roles(jetonDeTest({ roles: ['ADMIN', 'CLIENT'] }))).toEqual(['ADMIN', 'CLIENT']);
  });

  it('décode le base64url (caractères - et _) et l’UTF-8', () => {
    // « ÿÿ? » produit des caractères base64 + et / remplacés par - et _ en base64url.
    const jeton = jetonDeTest({ roles: ['CLIENT'], nom: 'Jérôme ÿÿ?>>>' });
    expect(jeton.split('.')[1]).toMatch(/[-_]/);
    expect(rolesDuJeton(jeton)).toEqual(['CLIENT']);
  });

  it('rend null pour un jeton qui n’a pas trois segments', () => {
    expect(rolesDuJeton('abc')).toBeNull();
    expect(rolesDuJeton('a.b.c.d')).toBeNull();
  });

  it('rend null pour un segment illisible', () => {
    expect(rolesDuJeton('a.%%%.c')).toBeNull();
    expect(rolesDuJeton(`a.${btoa('pas du json')}.c`)).toBeNull();
    expect(rolesDuJeton(`a.${btoa('null')}.c`)).toBeNull();
  });

  it('rend null sans liste de rôles textuels', () => {
    expect(rolesDuJeton(jetonDeTest({ sub: '3' }))).toBeNull();
    expect(rolesDuJeton(jetonDeTest({ roles: 'CLIENT' }))).toBeNull();
    expect(rolesDuJeton(jetonDeTest({ roles: [1] }))).toBeNull();
  });
});
