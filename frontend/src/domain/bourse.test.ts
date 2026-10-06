import { describe, expect, it } from 'vitest';
import {
  PERIODES,
  PERIODE_PAR_DEFAUT,
  calculerIndicateurs,
  centimesDepuisPrix,
  dernieresSeances,
  pourcentageEnCentiemes,
  selectionParmi,
  texteDepuisCentimes,
  type PointCotation,
  type ValeurWatchlist,
} from './bourse';

function point(date: string, ouverture: string, cloture: string, volume = 1000): PointCotation {
  return { date, ouverture, haut: cloture, bas: ouverture, cloture, volume };
}

function valeur(id: number, symbole: string): ValeurWatchlist {
  return { id, symbole, nom: `Nom ${symbole}`, marche: 'Euronext Paris', devise: 'EUR' };
}

describe('périodes', () => {
  it('[FR-009-03][FR-029-06] propose 1W, 1M, 1Y et 5Y, 1M par défaut', () => {
    expect(PERIODES).toEqual(['1W', '1M', '1Y', '5Y']);
    expect(PERIODE_PAR_DEFAUT).toBe('1M');
  });
});

describe('centimesDepuisPrix', () => {
  it.each([
    ['170.16', 17016],
    ['170.1600', 17016],
    ['170.1650', 17017],
    ['170.1649', 17016],
    ['170.1', 17010],
    ['170', 17000],
    ['0.00', 0],
    ['0.995', 100],
  ])('[FR-009-04] %s vaut %i centimes', (prix, centimes) => {
    expect(centimesDepuisPrix(prix)).toBe(centimes);
  });

  it.each(['', 'abc', '-1.00', '1,50', '1.'])('[FR-009-04] refuse « %s »', (prix) => {
    expect(() => centimesDepuisPrix(prix)).toThrow(`Prix invalide : ${prix}`);
  });
});

describe('texteDepuisCentimes', () => {
  it.each([
    [17016, '170.16'],
    [5, '0.05'],
    [0, '0.00'],
    [-260, '-2.60'],
    [-5, '-0.05'],
    [100, '1.00'],
  ])('[FR-009-04] %i centimes s’écrivent %s', (centimes, texte) => {
    expect(texteDepuisCentimes(centimes)).toBe(texte);
  });
});

describe('pourcentageEnCentiemes', () => {
  it.each([
    [-260, 17276, -150],
    [260, 17276, 150],
    [1, 3, 3333],
    [-1, 3, -3333],
    [1, 8, 1250],
    [1, 16000, 1],
    [1, 40000, 0],
    [0, 500, 0],
    [3, 0, 0],
  ])('[FR-029-07] %i sur %i vaut %i centièmes de pour cent', (numerateur, denominateur, centiemes) => {
    expect(pourcentageEnCentiemes(numerateur, denominateur)).toBe(centiemes);
  });

  it('[FR-029-07] arrondit la demi-unité en s’éloignant de zéro', () => {
    // 1 / 20000 × 10000 = 0,5 centième : arrondi à 1 ; -0,5 : arrondi à -1.
    expect(pourcentageEnCentiemes(1, 20000)).toBe(1);
    expect(pourcentageEnCentiemes(-1, 20000)).toBe(-1);
  });
});

describe('calculerIndicateurs', () => {
  it('[FR-009-04][FR-029-07] rend cours, variation et performance, en baisse', () => {
    const points = [point('2026-09-03', '172.00', '172.76'), point('2026-10-01', '168.90', '166.44'), point('2026-10-02', '167.30', '170.16')];

    expect(calculerIndicateurs(points)).toEqual({ cours: '170.16', variation: '-2.60', performance: '-1.50', hausse: false });
  });

  it('[FR-009-05][FR-029-07] une performance positive ou nulle est une hausse', () => {
    expect(calculerIndicateurs([point('2026-09-03', '1', '100.0000'), point('2026-10-02', '1', '101.5000')])).toEqual({
      cours: '101.50',
      variation: '1.50',
      performance: '1.50',
      hausse: true,
    });
    expect(calculerIndicateurs([point('2026-09-03', '1', '50.00'), point('2026-10-02', '1', '50.00')])).toEqual({
      cours: '50.00',
      variation: '0.00',
      performance: '0.00',
      hausse: true,
    });
  });

  it('[FR-009-04] un seul point donne une variation nulle', () => {
    expect(calculerIndicateurs([point('2026-10-02', '1', '10.00')])?.variation).toBe('0.00');
  });

  it('[FR-009-09] sans point, aucun indicateur', () => {
    expect(calculerIndicateurs([])).toBeNull();
  });

  it('[FR-029-07] une première clôture nulle donne une performance nulle', () => {
    expect(calculerIndicateurs([point('2026-09-03', '0', '0.00'), point('2026-10-02', '0', '2.00')])?.performance).toBe('0.00');
  });
});

describe('dernieresSeances', () => {
  const quinze = Array.from({ length: 15 }, (_, indice) =>
    point(`2026-09-${String(indice + 1).padStart(2, '0')}`, '100.00', '101.00', indice),
  );

  it('[FR-009-07][FR-029-08] garde les 10 plus récentes, de la plus récente à la plus ancienne', () => {
    const seances = dernieresSeances(quinze);

    expect(seances).toHaveLength(10);
    expect(seances.map((seance) => seance.date)).toEqual([
      '2026-09-15', '2026-09-14', '2026-09-13', '2026-09-12', '2026-09-11',
      '2026-09-10', '2026-09-09', '2026-09-08', '2026-09-07', '2026-09-06',
    ]);
    expect(quinze[0]?.date).toBe('2026-09-01');
  });

  it('[FR-009-07][FR-029-08] moins de 10 points : tous', () => {
    expect(dernieresSeances(quinze.slice(0, 3))).toHaveLength(3);
    expect(dernieresSeances([])).toEqual([]);
  });

  it('[FR-009-07] rend les prix à 2 décimales, Var. J et sens de la séance', () => {
    const [haussiere, baissiere] = dernieresSeances([
      { date: '2026-10-01', ouverture: '168.9000', haut: '170.0000', bas: '166.4400', cloture: '166.4400', volume: 791316 },
      { date: '2026-10-02', ouverture: '167.3000', haut: '171.2600', bas: '166.9200', cloture: '170.1600', volume: 1133144 },
    ]);

    expect(haussiere).toEqual({
      date: '2026-10-02',
      ouverture: '167.30',
      haut: '171.26',
      bas: '166.92',
      cloture: '170.16',
      volume: 1133144,
      variationJour: '1.71',
      hausse: true,
    });
    expect(baissiere).toMatchObject({ ouverture: '168.90', cloture: '166.44', variationJour: '-1.46', hausse: false });
  });

  it('[FR-009-07] clôture égale à l’ouverture : séance haussière', () => {
    expect(dernieresSeances([point('2026-10-02', '10.00', '10.00')])[0]).toMatchObject({ variationJour: '0.00', hausse: true });
  });
});

describe('selectionParmi', () => {
  const valeurs = [valeur(1, 'AI.PA'), valeur(2, 'BNP.PA'), valeur(3, 'MSFT')];

  it('[FR-009-02][FR-029-06] sans souhait, la première valeur', () => {
    expect(selectionParmi(valeurs, null)).toBe('AI.PA');
  });

  it('[FR-009-12][FR-029-10] la valeur souhaitée, si elle est dans la watchlist', () => {
    expect(selectionParmi(valeurs, 'MSFT')).toBe('MSFT');
  });

  it('[FR-009-13][FR-029-12] valeur souhaitée retirée : la première restante', () => {
    expect(selectionParmi(valeurs.slice(1), 'AI.PA')).toBe('BNP.PA');
  });

  it('[FR-009-13][FR-029-12] watchlist vide : aucune sélection', () => {
    expect(selectionParmi([], 'AI.PA')).toBeNull();
    expect(selectionParmi([], null)).toBeNull();
  });
});
