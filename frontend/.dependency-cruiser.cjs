/**
 * Hexagone du front (docs/quality-gates.md) : règles bloquantes, toutes en sévérité error.
 * Fichier protégé : créé par la story 1.2, modifié ensuite par l'humain seulement.
 * @type {import('dependency-cruiser').IConfiguration}
 */
module.exports = {
  forbidden: [
    {
      name: 'domain-ne-depend-de-rien',
      comment: 'src/domain ne dépend d’aucun autre code du front (application, adapters, infrastructure).',
      severity: 'error',
      from: { path: '^src/domain/' },
      to: { path: '^src/', pathNot: '^src/domain/' },
    },
    {
      name: 'application-ne-depend-pas-des-adaptateurs',
      comment: 'src/application ne dépend ni de src/adapters ni de src/infrastructure.',
      severity: 'error',
      from: { path: '^src/application/' },
      to: { path: '^src/(adapters|infrastructure)/' },
    },
    {
      name: 'hexagone-sans-paquet-npm',
      comment: 'src/domain et src/application sont du TypeScript pur : aucun paquet npm ni module Node, hors fichiers de test.',
      severity: 'error',
      from: {
        path: '^src/(domain|application)/',
        pathNot: '\\.test\\.tsx?$',
      },
      to: {
        dependencyTypes: [
          'npm',
          'npm-dev',
          'npm-optional',
          'npm-peer',
          'npm-bundled',
          'npm-no-pkg',
          'npm-unknown',
          'core',
        ],
      },
    },
  ],
  options: {
    doNotFollow: { path: 'node_modules' },
    tsPreCompilationDeps: true,
    tsConfig: { fileName: 'tsconfig.json' },
  },
};
