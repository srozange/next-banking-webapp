/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const complet = { lines: 100, branches: 100, functions: 100, statements: 100 };
const peripherie = { lines: 80, branches: 80, functions: 80, statements: 80 };

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'node',
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      thresholds: {
        'src/domain/**': complet,
        'src/application/**': complet,
        'src/adapters/**': peripherie,
        'src/infrastructure/**': peripherie,
      },
    },
  },
});
