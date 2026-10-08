import { describe, expect, it, vi } from 'vitest';
import { enregistrerPlafond, supprimerPlafond, type ClientPlafonds, type ReponseEnregistrementPlafond } from './gererPlafonds';

const client = (enregistrer: ClientPlafonds['enregistrer'], supprimer: ClientPlafonds['supprimer'] = () => Promise.resolve({ statut: 'supprime' })): ClientPlafonds => ({ enregistrer, supprimer });
const reponse = (r: ReponseEnregistrementPlafond) => () => Promise.resolve(r);

describe('[FR-006-05] enregistrerPlafond : contrôles locaux', () => {
  it.each([
    [{ categorie: '', plafond: '500' }, 'categorie'],
    [{ categorie: 'Revenus', plafond: '500' }, 'categorie'],
    [{ categorie: 'Logement', plafond: '' }, 'plafond'],
    [{ categorie: 'Logement', plafond: '0' }, 'plafond'],
    [{ categorie: 'Logement', plafond: '-10' }, 'plafond'],
    [{ categorie: 'Logement', plafond: 'abc' }, 'plafond'],
    [{ categorie: 'Logement', plafond: '12345678901234' }, 'plafondTropGrand'],
  ] as const)('refuse %j sans appel (%s)', async (formulaire, refus) => {
    const enregistrer = vi.fn(reponse({ statut: 'enregistre' }));

    expect(await enregistrerPlafond(formulaire, '2026-10', client(enregistrer))).toEqual({ statut: 'invalide', refus });
    expect(enregistrer).not.toHaveBeenCalled();
  });
});

describe('[FR-006-07] enregistrerPlafond : envoi', () => {
  it('envoie catégorie, mois affiché et plafond à deux décimales calculé en centimes', async () => {
    const enregistrer = vi.fn(reponse({ statut: 'enregistre' }));

    const resultat = await enregistrerPlafond({ categorie: 'Alimentation', plafond: ' 500,5 ' }, '2026-10', client(enregistrer));

    expect(enregistrer).toHaveBeenCalledWith({ categorie: 'Alimentation', mois: '2026-10', plafond: '500.50' });
    expect(resultat).toEqual({ statut: 'enregistre', categorie: 'Alimentation' });
  });

  it('transmet le refus du service avec son texte', async () => {
    const resultat = await enregistrerPlafond({ categorie: 'Logement', plafond: '5' }, '2026-10', client(reponse({ statut: 'refuse', message: 'Veuillez saisir un plafond valide' })));

    expect(resultat).toEqual({ statut: 'refuse', message: 'Veuillez saisir un plafond valide' });
  });

  it.each([
    ['une réponse technique', reponse({ statut: 'technique' })],
    ['une exception', () => Promise.reject(new Error('détail secret'))],
  ])('rend technique pour %s', async (_nom, enregistrer) => {
    expect(await enregistrerPlafond({ categorie: 'Logement', plafond: '5' }, '2026-10', client(enregistrer))).toEqual({ statut: 'technique' });
  });
});

describe('[FR-006-08] supprimerPlafond', () => {
  it('rend vrai quand le service a supprimé le plafond', async () => {
    const supprimer = vi.fn(() => Promise.resolve({ statut: 'supprime' as const }));

    expect(await supprimerPlafond(7, client(reponse({ statut: 'enregistre' }), supprimer))).toBe(true);
    expect(supprimer).toHaveBeenCalledWith(7);
  });

  it.each([
    ['une réponse technique', () => Promise.resolve({ statut: 'technique' as const })],
    ['une exception', () => Promise.reject(new Error('réseau'))],
  ])('rend faux pour %s', async (_nom, supprimer) => {
    expect(await supprimerPlafond(7, client(reponse({ statut: 'enregistre' }), supprimer))).toBe(false);
  });
});
