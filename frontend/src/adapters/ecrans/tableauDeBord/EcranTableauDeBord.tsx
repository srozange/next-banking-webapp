import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { chargerTableauDeBord, type ClientTableauDeBord, type TableauDeBord } from '../../../application/consulterTableauDeBord';
import type { ClientNotifications } from '../../../application/gererNotifications';
import type { ClientVirement } from '../../../application/virement';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { BoutonSecondaire } from '../../ui/BoutonSecondaire';
import { Carte } from '../../ui/Carte';
import { Tableau } from '../../ui/Tableau';
import { SectionCentreNotifications } from '../notifications/SectionCentreNotifications';
import { formaterDate } from '../releve/formaterDate';
import { SectionSuiviVirements } from '../virement/SectionSuiviVirements';
import './EcranTableauDeBord.css';

const T = TEXTES.tableauDeBord;
const COLONNES = [T.colonnes.date, T.colonnes.description, T.colonnes.type, T.colonnes.montant, T.colonnes.soldeApres];

interface Proprietes {
  client: ClientTableauDeBord;
  /** Client de la section « Centre de notifications », placée après le contenu comparé et avant « Suivi de mes virements ». */
  clientNotifications: ClientNotifications;
  /** Client de la section « Suivi de mes virements », toujours la dernière de la page (UX-DR153). */
  clientVirement: ClientVirement;
}

/**
 * Écran /tableau-de-bord (FEAT-002) : solde total, une carte par compte, 5 dernières opérations du compte COURANT,
 * puis « Centre de notifications » et « Suivi de mes virements » (UX-DR153). Le chargement part au rendu, sans attendre l'identité (appels en parallèle) ;
 * aria-busy tant qu'il dure, ni squelette ni texte. Erreur du service : texte technique, rien d'autre.
 */
export function EcranTableauDeBord({ client, clientNotifications, clientVirement }: Proprietes) {
  const navigate = useNavigate();
  const [tableau, setTableau] = useState<TableauDeBord | null | undefined>(undefined);

  useEffect(() => {
    let actif = true;
    void chargerTableauDeBord(client).then((lu) => {
      if (actif) {
        setTableau(lu);
      }
    });
    return () => {
      actif = false;
    };
  }, [client]);

  return (
    <div className="ecran-tableau-de-bord" aria-busy={tableau === undefined ? 'true' : undefined} data-testid="dashboard-ecran">
      <title>{TEXTES.titresOnglet.tableauDeBord}</title>
      {tableau === null && (
        <AlerteErreur testId="dashboard-message-erreur">
          <p>{TEXTES.erreurTechnique}</p>
        </AlerteErreur>
      )}
      {tableau !== null && tableau !== undefined && (
        <>
          <section className="ecran-tableau-de-bord__section" aria-labelledby="dashboard-comptes-titre">
            <h2 id="dashboard-comptes-titre" className="ecran-tableau-de-bord__separateur" data-testid="dashboard-comptes-titre">
              {T.mesComptes}
            </h2>
            <Carte attenuee>
              <p className="ecran-tableau-de-bord__eyebrow" data-testid="dashboard-solde-total-titre">
                {T.soldeTotal}
              </p>
              <p className="ecran-tableau-de-bord__montant" data-testid="dashboard-solde-total">
                {tableau.soldeTotal}
              </p>
              <p className="ecran-tableau-de-bord__sous-titre">{T.soldeTotalSousTitre}</p>
            </Carte>
            <div className="ecran-tableau-de-bord__comptes">
              {tableau.comptes.map((compte) => (
                <Carte key={compte.id} attenuee>
                  <div data-testid={`dashboard-compte-${compte.id}`}>
                    <p className="ecran-tableau-de-bord__eyebrow" data-testid={`dashboard-compte-type-${compte.id}`}>
                      {compte.type}
                    </p>
                    <p className="ecran-tableau-de-bord__montant" data-testid={`dashboard-compte-solde-${compte.id}`}>
                      {compte.solde}
                    </p>
                    <p className="ecran-tableau-de-bord__sous-titre" data-testid={`dashboard-compte-numero-${compte.id}`}>
                      {T.numeroCompte(compte.numero)}
                    </p>
                  </div>
                </Carte>
              ))}
            </div>
          </section>
          <section className="ecran-tableau-de-bord__section ecran-tableau-de-bord__operations" aria-labelledby="dashboard-operations-titre">
            <p className="ecran-tableau-de-bord__separateur" data-testid="dashboard-activite-titre">
              {T.activiteRecente}
            </p>
            <h2 id="dashboard-operations-titre" className="ecran-tableau-de-bord__titre">
              {T.dernieresOperations}
            </h2>
            <Tableau testId="dashboard-operations" colonnes={COLONNES}>
              {tableau.lignes.length === 0 ? (
                <tr>
                  <td colSpan={COLONNES.length} data-testid="dashboard-operations-vide" />
                </tr>
              ) : (
                tableau.lignes.map((ligne, index) => {
                  const sens = ligne.type === 'CREDIT' ? 'credit' : 'debit';
                  return (
                    <tr key={index} data-testid={`dashboard-operation-${index}`}>
                      <td>{formaterDate(ligne.date)}</td>
                      <td>{ligne.description}</td>
                      <td className={`ecran-tableau-de-bord__sens--${sens}`} data-testid={`dashboard-operation-type-${index}`}>
                        {ligne.type}
                      </td>
                      <td className={`ecran-tableau-de-bord__sens--${sens}`} data-testid={`dashboard-operation-montant-${index}`}>
                        {ligne.montant}
                      </td>
                      <td data-testid={`dashboard-operation-solde-apres-${index}`}>{ligne.soldeApres}</td>
                    </tr>
                  );
                })
              )}
            </Tableau>
            <div>
              <BoutonSecondaire data-testid="dashboard-voir-releve" onClick={() => void navigate('/releve')}>
                {T.voirReleve}
              </BoutonSecondaire>
            </div>
          </section>
        </>
      )}
      <SectionCentreNotifications client={clientNotifications} />
      <SectionSuiviVirements client={clientVirement} testId="dashboard-virements-revue" />
    </div>
  );
}
