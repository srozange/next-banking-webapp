import { describe, expect, it } from 'vitest';
import {
  alerteOuverte,
  clientDepuis,
  compterAlertesOuvertes,
  dateAlerte,
  filtrerClients,
  montantATraiter,
  relecteurAlerte,
  scoreAlerte,
  type AlerteFraudeAdministree,
  type ClientAdministre,
} from './administration';

const client = (clientId: number, nomComplet: string, login: string, email: string): ClientAdministre => ({
  clientId,
  nomComplet,
  login,
  email,
  telephone: null,
  role: 'CLIENT',
  actif: true,
  nomConseiller: null,
  dateCreation: '2024-02-01',
});

const JEAN = client(1, 'Jean Dupont', 'user1', 'jean.dupont@example.com');
const LUCIE = client(4, 'Lucie Martin', 'user4', 'lucie.martin@example.com');
const MARIE = client(2, 'Marie Lebrun', 'user2', 'marie.lebrun@example.com');
const PIERRE = client(3, 'Pierre Bernard', 'user3', 'pierre.bernard@example.com');
const CLIENTS = [JEAN, LUCIE, MARIE, PIERRE];

describe('filtrerClients', () => {
  it('[FR-016-08] « DUPONT » ne garde que Jean Dupont (nom, sans casse)', () => {
    expect(filtrerClients(CLIENTS, 'DUPONT')).toEqual([JEAN]);
  });

  it('[FR-016-08] « user » garde les quatre clients (login partiel), dans leur ordre', () => {
    expect(filtrerClients(CLIENTS, 'user')).toEqual(CLIENTS);
  });

  it('[FR-016-08] cherche aussi dans l’email, terme rogné', () => {
    expect(filtrerClients(CLIENTS, '  LEBRUN@EXAMPLE ')).toEqual([MARIE]);
    expect(filtrerClients(CLIENTS, 'user3')).toEqual([PIERRE]);
  });

  it('[FR-016-08] un terme vide ou blanc rend tous les clients', () => {
    expect(filtrerClients(CLIENTS, '')).toBe(CLIENTS);
    expect(filtrerClients(CLIENTS, '   ')).toBe(CLIENTS);
  });

  it('[FR-016-08] un terme sans correspondance rend une liste vide', () => {
    expect(filtrerClients(CLIENTS, 'zzz')).toEqual([]);
  });
});

describe('montantATraiter', () => {
  it('[FR-016-02] rend le cumul au format legacy', () => {
    expect(montantATraiter({ pretsEnAttente: 2, montantEnAttente: '15000.00' })).toBe('EUR15,000');
    expect(montantATraiter({ pretsEnAttente: 1, montantEnAttente: '1234.50' })).toBe('EUR1,234.5');
  });

  it('[FR-016-02] rend EUR0 sans prêt en attente', () => {
    expect(montantATraiter({ pretsEnAttente: 0, montantEnAttente: '0.00' })).toBe('EUR0');
  });
});

describe('clientDepuis', () => {
  it('[FR-016-10][BUG-055] rend la date de création de la fiche en jj/MM/aaaa', () => {
    expect(clientDepuis(JEAN)).toBe('01/02/2024');
  });
});

const ALERTE: AlerteFraudeAdministree = {
  alerteId: 3, clientId: 1, nomClient: 'Jean Dupont', regle: 'LARGE_AMOUNT', score: 45, statut: 'OPEN', description: 'Montant eleve',
  creeLe: '2026-11-17T08:05:00Z', revuPar: null, nomRelecteur: null,
};

describe('alertes de fraude', () => {
  it('[FR-016-14][BUG-056] compte les seules alertes OPEN parmi tous les statuts', () => {
    const alertes = [ALERTE, { ...ALERTE, alerteId: 2, statut: 'CONFIRMED' }, { ...ALERTE, alerteId: 1, statut: 'FALSE_POSITIVE' }, { ...ALERTE, statut: 'REVIEWED' }];
    expect(compterAlertesOuvertes(alertes)).toBe(1);
    expect(compterAlertesOuvertes([])).toBe(0);
    expect(alerteOuverte(ALERTE)).toBe(true);
    expect(alerteOuverte(alertes[1]!)).toBe(false);
  });

  it('[FR-016-14] rend le score sur 100 et la date en Europe/Paris', () => {
    expect(scoreAlerte(ALERTE)).toBe('45/100');
    expect(dateAlerte(ALERTE)).toBe('17/11/2026 09:05');
  });

  it('[FR-016-16] affiche le relecteur par son nom, sinon par son identifiant, sinon rien', () => {
    expect(relecteurAlerte({ ...ALERTE, revuPar: '10', nomRelecteur: 'Alice Martin' })).toBe('Alice Martin');
    expect(relecteurAlerte({ ...ALERTE, revuPar: '77' })).toBe('77');
    expect(relecteurAlerte(ALERTE)).toBe('');
  });
});
