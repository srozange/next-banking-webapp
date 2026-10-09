import type { PartieAdministration } from '../../../application/consulterAdministration';
import type { UtilisateurAdministre } from '../../../domain/administration';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { Etiquette } from '../../ui/Etiquette';
import { Tableau } from '../../ui/Tableau';

const U = TEXTES.administration.utilisateurs;
const G = TEXTES.administration.gestionClients;
const COLONNES = [U.colonnes.nom, U.colonnes.login, U.colonnes.email, U.colonnes.role, U.colonnes.statut];

interface Proprietes {
  /** Utilisateurs de tous rôles ; null pendant le chargement. */
  partie: PartieAdministration<readonly UtilisateurAdministre[]> | null;
}

/** Section « Tous les utilisateurs du systeme » de l'écran Administration (FR-016-13), en lecture seule. */
export function SectionUtilisateurs({ partie }: Proprietes) {
  const utilisateurs = partie?.donnees ?? null;
  return (
    <section className="ecran-administration__section" aria-labelledby="admin-utilisateurs-titre" data-testid="admin-utilisateurs-section">
      <h2 id="admin-utilisateurs-titre" className="ecran-administration__titre">
        {U.titre}
      </h2>
      {partie?.message != null && (
        <AlerteErreur testId="admin-message-erreur">
          <p>{partie.message}</p>
        </AlerteErreur>
      )}
      {utilisateurs !== null && (
        <Tableau testId="admin-utilisateurs" colonnes={COLONNES}>
          {utilisateurs.map((utilisateur) => (
            <tr key={utilisateur.utilisateurId} data-testid={`admin-utilisateur-${utilisateur.utilisateurId}`}>
              <td>{utilisateur.nomComplet}</td>
              <td>{utilisateur.login}</td>
              <td>{utilisateur.email}</td>
              <td>
                <Etiquette data-testid={`admin-utilisateur-role-${utilisateur.utilisateurId}`}>{utilisateur.role}</Etiquette>
              </td>
              <td>
                <Etiquette data-testid={`admin-utilisateur-statut-${utilisateur.utilisateurId}`}>
                  {utilisateur.actif ? G.actif : G.inactif}
                </Etiquette>
              </td>
            </tr>
          ))}
        </Tableau>
      )}
    </section>
  );
}
