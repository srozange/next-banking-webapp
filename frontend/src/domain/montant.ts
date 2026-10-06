/**
 * Règle pure : un montant échangé en JSON est une chaîne à deux décimales ("100.00").
 * Le front calcule en centimes entiers, jamais en flottant.
 */
const FORMAT_MONTANT = /^-?\d+\.\d{2}$/;

export function centimesDepuisChaine(montant: string): number {
  if (!FORMAT_MONTANT.test(montant)) {
    throw new Error(`Montant invalide : ${montant}`);
  }
  const negatif = montant.startsWith('-');
  const [entiers, decimales] = montant.replace('-', '').split('.');
  const centimes = Number(entiers) * 100 + Number(decimales);
  return negatif && centimes !== 0 ? -centimes : centimes;
}

/** Saisie d'un montant à l'écran : entiers, virgule ou point, deux décimales au plus, signe moins facultatif. */
const FORMAT_SAISIE = /^(-?)(\d+)(?:[.,](\d{1,2}))?$/;

/**
 * Centimes entiers d'une saisie (« 100 », « 100,5 », « 100,00 »), ou null si la saisie n'est pas un montant
 * lisible. Le calcul reste en entiers : jamais de flottant.
 */
export function centimesDepuisSaisie(saisie: string): number | null {
  const lecture = FORMAT_SAISIE.exec(saisie.trim());
  if (lecture === null) {
    return null;
  }
  const [, signe, entiers, decimales] = lecture;
  const centimes = Number(entiers) * 100 + Number((decimales ?? '').padEnd(2, '0'));
  return signe === '-' && centimes !== 0 ? -centimes : centimes;
}

/** Chaîne JSON à point et deux décimales d'un nombre de centimes entiers. */
export function chaineDepuisCentimes(centimes: number): string {
  const absolu = Math.abs(centimes);
  const reste = String(absolu % 100).padStart(2, '0');
  return `${centimes < 0 ? '-' : ''}${Math.floor(absolu / 100)}.${reste}`;
}
