import { describe, expect, it, vi } from 'vitest';
import {
  chargerNotifications,
  toutEffacer,
  toutMarquerLu,
  type ClientNotifications,
  type ReponseActionNotifications,
  type ReponseNotifications,
} from './gererNotifications';

const LISTE: ReponseNotifications = {
  statut: 'liste',
  notifications: [
    { id: 3, type: 'FRAUDE', titre: null, message: 'trois', creeLe: '2026-11-17T11:00:00Z', lue: false },
    { id: 2, type: 'ALERTE', titre: null, message: 'deux', creeLe: '2026-11-17T10:00:00Z', lue: true },
  ],
};
const EFFECTUEE: ReponseActionNotifications = { statut: 'effectuee' };
const TECHNIQUE = { statut: 'technique' } as const;

const client = (surcharge: Partial<ClientNotifications> = {}) => ({
  lister: vi.fn(surcharge.lister ?? (() => Promise.resolve(LISTE))),
  toutMarquerLu: vi.fn(surcharge.toutMarquerLu ?? (() => Promise.resolve(EFFECTUEE))),
  toutEffacer: vi.fn(surcharge.toutEffacer ?? (() => Promise.resolve(EFFECTUEE))),
});

describe('chargerNotifications', () => {
  it('[FR-002-07] rend les notifications prêtes à afficher et le nombre de non lues', async () => {
    const centre = await chargerNotifications(client());

    expect(centre?.nonLues).toBe(1);
    expect(centre?.notifications.map((n) => [n.id, n.icone, n.horodatage, n.lue])).toEqual([
      [3, '🚨', '17/11/2026 12:00:00', false],
      [2, '⚠️', '17/11/2026 11:00:00', true],
    ]);
  });

  it('[FR-002-07] une liste vide donne un centre vide', async () => {
    const centre = await chargerNotifications(client({ lister: () => Promise.resolve({ statut: 'liste', notifications: [] }) }));

    expect(centre).toEqual({ notifications: [], nonLues: 0 });
  });

  it('[FR-002-07] rend null si le service est indisponible ou lève', async () => {
    expect(await chargerNotifications(client({ lister: () => Promise.resolve(TECHNIQUE) }))).toBeNull();
    expect(await chargerNotifications(client({ lister: () => Promise.reject(new Error('réseau')) }))).toBeNull();
  });
});

describe('toutMarquerLu', () => {
  it('[FR-025-06] marque tout lu puis relit la liste du service', async () => {
    const c = client();

    const centre = await toutMarquerLu(c);

    expect(c.toutMarquerLu).toHaveBeenCalledTimes(1);
    expect(c.lister).toHaveBeenCalledTimes(1);
    expect(centre?.notifications).toHaveLength(2);
  });

  it('[FR-025-06] rend null, sans relire, si l’action échoue ou lève', async () => {
    const technique = client({ toutMarquerLu: () => Promise.resolve(TECHNIQUE) });
    const leve = client({ toutMarquerLu: () => Promise.reject(new Error('réseau')) });

    expect(await toutMarquerLu(technique)).toBeNull();
    expect(await toutMarquerLu(leve)).toBeNull();
    expect(technique.lister).not.toHaveBeenCalled();
    expect(leve.lister).not.toHaveBeenCalled();
  });

  it('[FR-025-06] rend null si la relecture échoue', async () => {
    expect(await toutMarquerLu(client({ lister: () => Promise.resolve(TECHNIQUE) }))).toBeNull();
  });
});

describe('toutEffacer', () => {
  it('[FR-025-07] efface tout puis relit la liste du service', async () => {
    const c = client({ lister: () => Promise.resolve({ statut: 'liste', notifications: [] }) });

    expect(await toutEffacer(c)).toEqual({ notifications: [], nonLues: 0 });
    expect(c.toutEffacer).toHaveBeenCalledTimes(1);
    expect(c.toutMarquerLu).not.toHaveBeenCalled();
  });

  it('[FR-025-07] rend null, sans relire, si l’action échoue', async () => {
    const c = client({ toutEffacer: () => Promise.resolve(TECHNIQUE) });

    expect(await toutEffacer(c)).toBeNull();
    expect(c.lister).not.toHaveBeenCalled();
  });
});
