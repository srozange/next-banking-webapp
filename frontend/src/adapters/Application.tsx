import { useRef } from 'react';
import { Outlet } from 'react-router';
import { TEXTES } from './textes';
import { LienEvitement } from './ui/LienEvitement';
import './Application.css';

const ID_CONTENU_PRINCIPAL = 'contenu-principal';

/** Mise en page commune à toutes les pages : lien d'évitement, puis contenu principal (route courante). */
export function Application() {
  const contenuPrincipal = useRef<HTMLElement>(null);
  return (
    <>
      <LienEvitement cible={contenuPrincipal} idCible={ID_CONTENU_PRINCIPAL}>
        {TEXTES.lienEvitement}
      </LienEvitement>
      <main
        ref={contenuPrincipal}
        id={ID_CONTENU_PRINCIPAL}
        className="application-contenu"
        tabIndex={-1}
        data-testid="application-racine"
      >
        <Outlet />
      </main>
    </>
  );
}
