import { describe, expect, it } from 'vitest';
import { centimesDepuisChaine, centimesDepuisSaisie, chaineDepuisCentimes } from './montant';

describe('centimesDepuisChaine', () => {
  it('convertit un montant à deux décimales en centimes entiers', () => {
    expect(centimesDepuisChaine('100.00')).toBe(10000);
    expect(centimesDepuisChaine('0.05')).toBe(5);
    expect(centimesDepuisChaine('-12.34')).toBe(-1234);
    expect(centimesDepuisChaine('-0.00')).toBe(0);
  });

  it('refuse un montant sans exactement deux décimales', () => {
    expect(() => centimesDepuisChaine('100')).toThrow();
    expect(() => centimesDepuisChaine('100.5')).toThrow();
    expect(() => centimesDepuisChaine('1e3')).toThrow();
    expect(() => centimesDepuisChaine('100.555')).toThrow();
    expect(() => centimesDepuisChaine(' 1.00')).toThrow();
    expect(() => centimesDepuisChaine('+1.00')).toThrow();
  });
});

describe('centimesDepuisSaisie', () => {
  it('lit une saisie en centimes entiers', () => {
    expect(centimesDepuisSaisie('100')).toBe(10000);
    expect(centimesDepuisSaisie(' 100,5 ')).toBe(10050);
    expect(centimesDepuisSaisie('100,00')).toBe(10000);
    expect(centimesDepuisSaisie('0.05')).toBe(5);
    expect(centimesDepuisSaisie('-5')).toBe(-500);
    expect(centimesDepuisSaisie('-0')).toBe(0);
  });

  it('rend null pour une saisie illisible', () => {
    for (const saisie of ['', 'abc', '1,234', '1e3', '--1', '1,', ',5']) {
      expect(centimesDepuisSaisie(saisie)).toBeNull();
    }
  });
});

describe('chaineDepuisCentimes', () => {
  it('écrit deux décimales à point', () => {
    expect(chaineDepuisCentimes(10000)).toBe('100.00');
    expect(chaineDepuisCentimes(5)).toBe('0.05');
    expect(chaineDepuisCentimes(-500)).toBe('-5.00');
    expect(chaineDepuisCentimes(0)).toBe('0.00');
  });
});
