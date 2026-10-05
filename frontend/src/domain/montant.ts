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
