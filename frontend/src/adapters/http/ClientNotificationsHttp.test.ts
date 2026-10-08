import { describe, expect, it, vi } from 'vitest';
import { creerClientNotificationsHttp } from './ClientNotificationsHttp';

const json = (corps: unknown, status: number) => Promise.resolve(new Response(JSON.stringify(corps), { status }));
const vide = (status: number) => Promise.resolve(new Response(null, { status }));
const notification = { id: 3, type: 'FRAUDE', message: 'm', creeLe: '2026-11-17T11:00:00Z', lue: false };

describe('ClientNotificationsHttp', () => {
  it('[FR-002-07] lit les notifications sans aucun paramètre', async () => {
    const appeler = vi.fn((_e: unknown, _o?: unknown) => json([notification], 200));
    const client = creerClientNotificationsHttp(appeler as unknown as typeof fetch);

    expect(await client.lister()).toEqual({ statut: 'liste', notifications: [notification] });
    expect(appeler).toHaveBeenCalledWith('/api/notifications/notifications', expect.objectContaining({ method: 'GET' }));
  });

  it.each([
    ['un 500', () => json({}, 500)],
    ['un 401', () => json({ message: 'Authentification requise' }, 401)],
    ['un corps qui n’est pas une liste', () => json({}, 200)],
    ['une notification nulle', () => json([null], 200)],
    ['un identifiant illisible', () => json([{ ...notification, id: 'a' }], 200)],
    ['un type absent', () => json([{ ...notification, type: 1 }], 200)],
    ['un message absent', () => json([{ ...notification, message: null }], 200)],
    ['une date absente', () => json([{ ...notification, creeLe: 1 }], 200)],
    ['une date illisible', () => json([{ ...notification, creeLe: 'hier' }], 200)],
    ['un état de lecture absent', () => json([{ ...notification, lue: 'non' }], 200)],
    ['un corps illisible', () => Promise.resolve(new Response('pas du json', { status: 200 }))],
    ['une coupure', () => Promise.reject(new Error('réseau'))],
  ])('[FR-002-07] rend technique pour %s', async (_nom, reponse) => {
    const client = creerClientNotificationsHttp(reponse as unknown as typeof fetch);

    expect(await client.lister()).toEqual({ statut: 'technique' });
  });

  it('[FR-025-06] tout marquer lu : POST sur /lecture, sans corps', async () => {
    const appeler = vi.fn((_e: unknown, _o?: unknown) => vide(204));
    const client = creerClientNotificationsHttp(appeler as unknown as typeof fetch);

    expect(await client.toutMarquerLu()).toEqual({ statut: 'effectuee' });
    expect(appeler).toHaveBeenCalledWith('/api/notifications/notifications/lecture', expect.objectContaining({ method: 'POST' }));
    expect((appeler.mock.calls[0]?.[1] as RequestInit).body).toBeUndefined();
  });

  it('[FR-025-07] tout effacer : DELETE sur la liste, sans corps', async () => {
    const appeler = vi.fn((_e: unknown, _o?: unknown) => vide(204));
    const client = creerClientNotificationsHttp(appeler as unknown as typeof fetch);

    expect(await client.toutEffacer()).toEqual({ statut: 'effectuee' });
    expect(appeler).toHaveBeenCalledWith('/api/notifications/notifications', expect.objectContaining({ method: 'DELETE' }));
    expect((appeler.mock.calls[0]?.[1] as RequestInit).body).toBeUndefined();
  });

  it.each([
    ['un 500', () => vide(500)],
    ['un 200 inattendu', () => vide(200)],
    ['une coupure', () => Promise.reject(new Error('réseau'))],
  ])('[FR-025-06] [FR-025-07] les actions rendent technique pour %s', async (_nom, reponse) => {
    const client = creerClientNotificationsHttp(reponse as unknown as typeof fetch);

    expect(await client.toutMarquerLu()).toEqual({ statut: 'technique' });
    expect(await client.toutEffacer()).toEqual({ statut: 'technique' });
  });
});
