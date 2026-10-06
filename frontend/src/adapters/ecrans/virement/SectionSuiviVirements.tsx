import { useEffect, useState } from 'react';
import { chargerSuivi, type ClientVirement } from '../../../application/virement';
import { formaterDate, formaterHeure, type VirementSuivi } from '../../../domain/virement';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { Etiquette } from '../../ui/Etiquette';
import { Tableau } from '../../ui/Tableau';
import './SectionSuiviVirements.css';

const T = TEXTES.virement.suivi;
const COLONNES = [T.colonnes.date, T.colonnes.compte, T.colonnes.montant, T.colonnes.etat, T.colonnes.butoir];

interface Proprietes {
  client: ClientVirement;
  /** `virement-virements-revue` sur l'écran de virement, `dashboard-virements-revue` sur le tableau de bord. */
  testId: string;
  /** Change à chaque nouvelle soumission : la section se recharge. */
  version?: number;
}

/**
 * Section « Suivi de mes virements » (A-027-10) : les quatre premiers virements passés en revue du client, leur état
 * en badge, et pour un virement suspendu l'heure butoir de la réponse. Placée après le contenu existant de la page.
 */
export function SectionSuiviVirements({ client, testId, version = 0 }: Proprietes) {
  const [virements, setVirements] = useState<readonly VirementSuivi[] | null | undefined>(undefined);

  useEffect(() => {
    let actif = true;
    void chargerSuivi(client).then((lus) => {
      if (actif) {
        setVirements(lus);
      }
    });
    return () => {
      actif = false;
    };
  }, [client, version]);

  return (
    <section className="section-suivi" aria-labelledby={`${testId}-titre`} data-testid={testId}>
      <h2 id={`${testId}-titre`} className="section-suivi__titre">
        {T.titre}
      </h2>
      <p className="section-suivi__aide">{T.aide}</p>
      {virements === null && <AlerteErreur testId={`${testId}-erreur`}><p>{TEXTES.erreurTechnique}</p></AlerteErreur>}
      {virements !== null && virements !== undefined && (
        <Tableau testId={`${testId}-tableau`} colonnes={COLONNES}>
          {virements.length === 0 ? (
            <tr>
              <td colSpan={COLONNES.length}>{T.vide}</td>
            </tr>
          ) : (
            virements.map((virement) => (
              <tr key={virement.virementId}>
                <td>{formaterDate(virement.soumisLe)}</td>
                <td>{virement.compteDestinataire}</td>
                <td>{virement.montant}</td>
                <td>
                  <Etiquette data-testid={`virement-etat-${virement.virementId}`}>{T.etats[virement.etat]}</Etiquette>
                </td>
                <td>
                  {virement.etat === 'SUSPENDU'
                    ? T.butoir(formaterDate(virement.heureButoir), formaterHeure(virement.heureButoir))
                    : ''}
                </td>
              </tr>
            ))
          )}
        </Tableau>
      )}
    </section>
  );
}
