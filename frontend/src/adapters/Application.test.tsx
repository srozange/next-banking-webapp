import { describe, expect, it } from 'vitest';
import { renderToString } from 'react-dom/server';
import { Application } from './Application';

describe('Application', () => {
  it('rend la racine avec son data-testid figé', () => {
    expect(renderToString(<Application />)).toContain('data-testid="application-racine"');
  });
});
