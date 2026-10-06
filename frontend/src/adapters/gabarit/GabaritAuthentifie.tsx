import { useEffect, useState } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router';
import { ROUTE_CONNEXION } from '../../domain/connexion';
import { sousNavigation } from '../../domain/navigation';
import { chargerProfil, type ClientProfil, type ResultatProfil } from '../../application/chargerProfil';
import type { StockageJeton } from '../../application/seConnecter';
import { ContenuPrincipal } from '../ui/ContenuPrincipal';
import { EnTete } from './EnTete';
import { SousEnTete } from './SousEnTete';

interface Proprietes {
  /** Rôles de la session, lus dans le jeton par la garde. */
  roles: readonly string[];
  clientProfil: ClientProfil;
  stockage: StockageJeton;
}

/**
 * Gabarit des pages authentifiées : en-tête (landmark banner), sous-en-tête des espaces réservés, puis contenu
 * principal en aria-busy jusqu'à la réponse de /moi. Profil refusé (401) : retour à /login, sans message.
 */
export function GabaritAuthentifie({ roles, clientProfil, stockage }: Proprietes) {
  const { pathname } = useLocation();
  const [profil, setProfil] = useState<ResultatProfil | null>(null);

  useEffect(() => {
    let actif = true;
    void chargerProfil(clientProfil, stockage).then((resultat) => {
      if (actif) {
        setProfil(resultat);
      }
    });
    return () => {
      actif = false;
    };
  }, [clientProfil, stockage]);

  if (profil?.type === 'deconnecte') {
    return <Navigate to={ROUTE_CONNEXION} replace />;
  }
  const sousNav = sousNavigation(pathname);
  return (
    <>
      <header>
        <EnTete roles={roles} nomComplet={profil?.type === 'profil' ? profil.nomComplet : null} />
        {sousNav !== null && <SousEnTete navigation={sousNav} />}
      </header>
      <ContenuPrincipal occupe={profil === null}>
        <Outlet />
      </ContenuPrincipal>
    </>
  );
}
