/// <reference types="vitest/config" />
import { defineConfig, type ProxyOptions } from 'vite';
import react from '@vitejs/plugin-react';

const complet = { lines: 100, branches: 100, functions: 100, statements: 100 };
const peripherie = { lines: 80, branches: 80, functions: 80, statements: 80 };

// Ports de développement des services (AD-17). Audit absent : /api/audit/** n'est pas renvoyé.
const portsDesServices: Record<string, number> = {
  'identite-clients': 8081,
  'coeur-bancaire': 8082,
  credits: 8083,
  documents: 8084,
  notifications: 8085,
  bourse: 8087,
  'open-banking-conseiller': 8088,
};

// /api/{identifiant}/** vers le service, chemin transmis tel quel ; X-Forwarded-For écrasé par l'adresse du client.
const renvoi = (port: number): ProxyOptions => ({
  target: `http://localhost:${port}`,
  // Appelé avant le renvoi ; ne rend rien, donc la requête est bien renvoyée, en-têtes compris.
  bypass: (requete) => {
    requete.headers['x-forwarded-for'] = requete.socket.remoteAddress ?? '';
  },
});

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: Object.fromEntries(
      Object.entries(portsDesServices).map(([identifiant, port]) => [`/api/${identifiant}/`, renvoi(port)]),
    ),
  },
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
