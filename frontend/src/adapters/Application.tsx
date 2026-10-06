import { Outlet } from 'react-router';
import { TEXTES } from './textes';
import { ID_CONTENU_PRINCIPAL } from './ui/ContenuPrincipal';
import { LienEvitement } from './ui/LienEvitement';

/**
 * Mise en page commune à toutes les pages : lien d'évitement, puis la branche courante (connexion ou
 * gabarit authentifié), qui rend elle-même le contenu principal, après l'en-tête le cas échéant.
 */
export function Application() {
  return (
    <>
      <LienEvitement idCible={ID_CONTENU_PRINCIPAL}>{TEXTES.lienEvitement}</LienEvitement>
      <Outlet />
    </>
  );
}
