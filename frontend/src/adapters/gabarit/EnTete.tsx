import { badgeDuRole, liensDuRole } from '../../domain/navigation';
import { TEXTES } from '../textes';
import { BoutonTexte } from '../ui/BoutonTexte';
import { Etiquette } from '../ui/Etiquette';
import { LienNavigation } from '../ui/LienNavigation';
import { TESTID_LIENS } from './identifiants';
import './EnTete.css';

const T = TEXTES.navigation;

interface Proprietes {
  roles: readonly string[];
  /** Nom complet lu par /moi ; null tant qu'il est inconnu (aucune salutation). */
  nomComplet: string | null;
  /** Action du bouton de déconnexion. */
  surDeconnexion: () => void;
}

/** En-tête global (header, 72px) : marque, liens filtrés par rôle, badge, salutation, déconnexion. */
export function EnTete({ roles, nomComplet, surDeconnexion }: Proprietes) {
  const badge = badgeDuRole(roles);
  return (
    <div className="en-tete">
      <div className="en-tete__marque">
        <span className="en-tete__logo">{T.logo}</span>
        <span className="en-tete__titre">{T.titre}</span>
      </div>
      <nav className="en-tete__navigation">
        <ul className="en-tete__liens">
          {liensDuRole(roles).map(({ cle, route }) => (
            <li key={cle}>
              <LienNavigation to={route} variante="en-tete" id={TESTID_LIENS[cle]} data-testid={TESTID_LIENS[cle]}>
                {T.liens[cle]}
              </LienNavigation>
            </li>
          ))}
        </ul>
      </nav>
      <div className="en-tete__session">
        {badge !== null && <Etiquette data-testid="nav-badge-role">{T.badges[badge]}</Etiquette>}
        {nomComplet !== null && (
          <span className="en-tete__salutation" data-testid="nav-salutation">
            {T.salutation(nomComplet)}
          </span>
        )}
        <BoutonTexte data-testid="nav-deconnexion" onClick={surDeconnexion}>{T.deconnexion}</BoutonTexte>
      </div>
    </div>
  );
}
