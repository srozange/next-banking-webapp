import { StrictMode, type ReactElement } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const { render, createRoot } = vi.hoisted(() => {
  const render = vi.fn((_element: unknown) => {});
  const createRoot = vi.fn(() => ({ render }));
  return { render, createRoot };
});

vi.mock('react-dom/client', () => ({ createRoot }));

function simulerDocument(racine: object | null) {
  const getElementById = vi.fn(() => racine);
  vi.stubGlobal('document', { getElementById });
  return getElementById;
}

describe('main', () => {
  beforeEach(() => {
    vi.resetModules();
    render.mockClear();
    createRoot.mockClear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('rend l’application dans #root', async () => {
    const racine = {};
    const getElementById = simulerDocument(racine);

    await import('./main');
    // Après vi.resetModules() : la même instance de module que celle importée par main.
    const { Application } = await import('../adapters/Application');

    expect(getElementById).toHaveBeenCalledWith('root');
    expect(createRoot).toHaveBeenCalledWith(racine);
    expect(render).toHaveBeenCalledOnce();
    const element = render.mock.calls[0]?.[0] as ReactElement<{ children: ReactElement }>;
    expect(element.type).toBe(StrictMode);
    expect(element.props.children.type).toBe(Application);
  });

  it('lève une erreur si #root est absent', async () => {
    simulerDocument(null);

    await expect(import('./main')).rejects.toThrow('Élément #root introuvable dans index.html');
    expect(createRoot).not.toHaveBeenCalled();
  });
});
