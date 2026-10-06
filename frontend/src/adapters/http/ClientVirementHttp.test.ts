import { describe, expect, it, vi } from 'vitest';
import { creerClientVirementHttp } from './ClientVirementHttp';

const commande = { compteSourceId: 1, compteDestinataireId: 2, montant: '100.00', motif: 'loyer' };
const json = (corps: unknown, status: number) => Promise.resolve(new Response(JSON.stringify(corps), { status }));

describe('ClientVirementHttp', () => {
  it('lit les comptes du client', async () => {
    const comptes = [{ id: 1, type: 'COURANT', numero: 'FR76-0001-0001' }];
    const appeler = vi.fn((_e: unknown, _o?: unknown) => json(comptes, 200));
    const client = creerClientVirementHttp(appeler as unknown as typeof fetch);

    expect(await client.lireComptes()).toEqual({ statut: 'comptes', comptes });
    expect(appeler).toHaveBeenCalledWith('/api/coeur-bancaire/comptes', expect.objectContaining({ method: 'GET' }));
  });

  it.each([
    ['un statut d’erreur', () => json({}, 500)],
    ['un corps qui n’est pas une liste', () => json({ a: 1 }, 200)],
    ['un compte mal formé', () => json([{ id: 'x' }], 200)],
    ['un corps illisible', () => Promise.resolve(new Response('pas du json', { status: 200 }))],
    ['une coupure', () => Promise.reject(new Error('réseau'))],
  ])('rend technique pour %s à la lecture des comptes', async (_nom, reponse) => {
    const client = creerClientVirementHttp(reponse as unknown as typeof fetch);
    expect(await client.lireComptes()).toEqual({ statut: 'technique' });
  });

  it('[FR-003-08] poste le virement avec sa clé d’idempotence et omet un motif vide', async () => {
    const appeler = vi.fn((_e: unknown, _o?: RequestInit) => json({}, 201));
    const client = creerClientVirementHttp(appeler as unknown as typeof fetch, () => 'cle-1');

    expect(await client.executer(commande)).toEqual({ statut: 'execute' });
    expect(await client.executer({ ...commande, motif: '' })).toEqual({ statut: 'execute' });

    const [chemin, options] = appeler.mock.calls[0] as [string, RequestInit];
    expect(chemin).toBe('/api/coeur-bancaire/virements');
    expect(options.method).toBe('POST');
    expect((options.headers as Record<string, string>)['Idempotency-Key']).toBe('cle-1');
    expect(JSON.parse(options.body as string)).toEqual({ compteSourceId: 1, compteDestinataireId: 2, montant: '100.00', motif: 'loyer' });
    const second = JSON.parse((appeler.mock.calls[1] as [string, RequestInit])[1].body as string) as object;
    expect(second).not.toHaveProperty('motif');
  });

  it('[FR-023-02] garde la clé après un 5xx ou une coupure, puis en change après une réponse définitive', async () => {
    const reponses = [
      () => json({ message: 'Le virement n\'a pas pu etre pris en compte, veuillez reessayer plus tard' }, 500),
      () => Promise.reject(new Error('réseau')),
      () => json({ message: 'Solde insuffisant sur le compte source' }, 400),
      () => json({}, 201),
    ];
    const appeler = vi.fn((_e: unknown, _o?: unknown) => (reponses.shift() as () => Promise<Response>)());
    let n = 0;
    const client = creerClientVirementHttp(appeler as unknown as typeof fetch, () => `cle-${++n}`);

    expect(await client.executer(commande)).toEqual({
      statut: 'erreur',
      message: "Le virement n'a pas pu etre pris en compte, veuillez reessayer plus tard",
    });
    expect(await client.executer(commande)).toEqual({ statut: 'erreur', message: null });
    expect(await client.executer(commande)).toEqual({ statut: 'erreur', message: 'Solde insuffisant sur le compte source' });
    expect(await client.executer(commande)).toEqual({ statut: 'execute' });

    const cles = appeler.mock.calls.map((appel) => ((appel[1] as RequestInit).headers as Record<string, string>)['Idempotency-Key']);
    expect(cles).toEqual(['cle-1', 'cle-1', 'cle-1', 'cle-2']);
  });

  it('rend un message nul quand la réponse d’erreur n’en porte pas de textuel', async () => {
    for (const corps of [() => json({ message: 3 }, 400), () => Promise.resolve(new Response('x', { status: 403 })), () => json(null, 404)]) {
      const client = creerClientVirementHttp(corps as unknown as typeof fetch);
      expect(await client.executer(commande)).toEqual({ statut: 'erreur', message: null });
    }
  });

  it('génère par défaut une clé UUID', async () => {
    const appeler = vi.fn((_e: unknown, _o?: unknown) => json({}, 201));
    await creerClientVirementHttp(appeler as unknown as typeof fetch).executer(commande);
    const cle = ((appeler.mock.calls[0]?.[1] as RequestInit).headers as Record<string, string>)['Idempotency-Key'];
    expect(cle).toMatch(/^[0-9a-f-]{36}$/);
  });
});
