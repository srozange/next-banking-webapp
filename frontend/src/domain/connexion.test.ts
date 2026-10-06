import { describe, expect, it } from 'vitest';
import { accueilDuRole, ROUTE_CONNEXION, validerIdentifiants } from './connexion';

describe('validerIdentifiants', () => {
  it('[FR-001-03] signale les deux champs vides, dans l’ordre du formulaire', () => {
    expect(validerIdentifiants('', '')).toEqual(['identifiant', 'motDePasse']);
  });

  it('[FR-001-03] traite un identifiant blanc comme vide', () => {
    expect(validerIdentifiants('  \t ', 'secret')).toEqual(['identifiant']);
  });

  it('[FR-001-03] signale le seul mot de passe vide', () => {
    expect(validerIdentifiants('user1', '')).toEqual(['motDePasse']);
  });

  it('[FR-001-03] accepte un mot de passe fait d’espaces, refusé seulement s’il est vide', () => {
    expect(validerIdentifiants('user1', '   ')).toEqual([]);
  });

  it('accepte deux champs remplis', () => {
    expect(validerIdentifiants('user1', 'secret')).toEqual([]);
  });
});

describe('accueilDuRole', () => {
  it('[FR-001-05][FR-030-03] envoie chaque rôle sur son écran d’accueil', () => {
    expect(accueilDuRole(['ADMIN'])).toBe('/administration');
    expect(accueilDuRole(['CONSEILLER'])).toBe('/espace-conseiller');
    expect(accueilDuRole(['CLIENT'])).toBe('/tableau-de-bord');
  });

  it('[FR-030-03] retient le rôle le plus élevé : ADMIN > CONSEILLER > CLIENT', () => {
    expect(accueilDuRole(['CLIENT', 'ADMIN', 'CONSEILLER'])).toBe('/administration');
    expect(accueilDuRole(['CLIENT', 'CONSEILLER'])).toBe('/espace-conseiller');
  });

  it('ne connaît aucun accueil pour un rôle inconnu ou une liste vide', () => {
    expect(accueilDuRole(['INVITE'])).toBeNull();
    expect(accueilDuRole([])).toBeNull();
  });

  it('nomme la route de connexion', () => {
    expect(ROUTE_CONNEXION).toBe('/login');
  });
});
