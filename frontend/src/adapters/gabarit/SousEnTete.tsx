import type { SousNavigation } from '../../domain/navigation';
import { TEXTES } from '../textes';
import { LienNavigation } from '../ui/LienNavigation';
import { TESTID_LIENS, TESTID_SOUS_LIENS } from './identifiants';
import './SousEnTete.css';

interface Proprietes {
  navigation: SousNavigation;
}

/**
 * Sous-en-tête (sub-header, 48px, fond primary) d'un espace réservé, second et dernier niveau de navigation.
 * Nommé par le lien d'en-tête de l'espace (aria-labelledby), sans libellé de regroupement visible.
 */
export function SousEnTete({ navigation }: Proprietes) {
  return (
    <nav className="sous-en-tete" aria-labelledby={TESTID_LIENS[navigation.espace.cle]}>
      <ul className="sous-en-tete__liens">
        {navigation.liens.map(({ cle, route }) => (
          <li key={cle}>
            <LienNavigation to={route} variante="sous-en-tete" data-testid={TESTID_SOUS_LIENS[cle]}>
              {TEXTES.navigation.sousLiens[cle]}
            </LienNavigation>
          </li>
        ))}
      </ul>
    </nav>
  );
}
