import { describe, expect, it, vi } from 'vitest';
import { creerSignalExpiration } from './signalExpiration';

describe('signalExpiration', () => {
  it('[FR-030-05] prévient chaque abonné, jusqu’à son désabonnement', () => {
    const signal = creerSignalExpiration();
    const premier = vi.fn();
    const second = vi.fn();
    const arreter = signal.ecouter(premier);
    signal.ecouter(second);

    signal.declencher();
    arreter();
    signal.declencher();

    expect(premier).toHaveBeenCalledOnce();
    expect(second).toHaveBeenCalledTimes(2);
  });
});
