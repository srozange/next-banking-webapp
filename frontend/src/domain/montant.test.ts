import { describe, expect, it } from 'vitest';
import { centimesDepuisChaine } from './montant';

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
