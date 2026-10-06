import { useId } from 'react';
import {
  formaterDuree,
  formaterMontantListe,
  formaterTaux,
  LIBELLES_STATUT,
  libelleType,
  type Pret,
} from '../../../domain/pret';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { Carte } from '../../ui/Carte';
import { Etiquette } from '../../ui/Etiquette';
import { Tableau } from '../../ui/Tableau';
import './SectionEmprunts.css';

const T = TEXTES.prets.emprunts;

interface Proprietes {
  /** Prêts de l'utilisateur, dans l'ordre du service ; null tant qu'ils ne sont pas lus. */
  prets: readonly Pret[] | null;
  /** La lecture a échoué : texte d'erreur technique, sans détail. */
  indisponible: boolean;
}

/**
 * Section « Mes emprunts » (FR-007-08) : prêts de l'utilisateur, statuts en étiquettes, « En cours de traitement... »
 * pour une demande en attente, ligne « Aucun emprunt enregistre » sous les en-têtes conservés si la liste est vide.
 */
export function SectionEmprunts({ prets, indisponible }: Proprietes) {
  const idTitre = useId();
  const C = T.colonnes;

  return (
    <section className="section-emprunts" aria-labelledby={idTitre}>
      <h2 id={idTitre} className="section-emprunts__separateur">
        {TEXTES.prets.separateurs.emprunts}
      </h2>
      <Carte>
        <h3 className="section-emprunts__titre">{T.titre}</h3>
        {indisponible && (
          <AlerteErreur testId="pret-message-erreur">
            <p>{TEXTES.erreurTechnique}</p>
          </AlerteErreur>
        )}
        <Tableau libelle={T.titre}>
          <thead>
            <tr>
              <th scope="col">{C.type}</th>
              <th scope="col">{C.montant}</th>
              <th scope="col">{C.taux}</th>
              <th scope="col">{C.duree}</th>
              <th scope="col">{C.mensualite}</th>
              <th scope="col">{C.totalInterets}</th>
              <th scope="col">{C.statut}</th>
              <th scope="col">{C.echeancier}</th>
              <th scope="col">{C.action}</th>
            </tr>
          </thead>
          <tbody>
            {prets?.length === 0 && (
              <tr>
                <td colSpan={9}>{T.aucun}</td>
              </tr>
            )}
            {prets?.map((pret) => (
              <tr key={pret.id}>
                <td>{libelleType(pret.type)}</td>
                <td>{formaterMontantListe(pret.montant)}</td>
                <td>{formaterTaux(pret.tauxAnnuel)}</td>
                <td>{formaterDuree(pret.dureeMois)}</td>
                <td>{formaterMontantListe(pret.mensualite)}</td>
                <td>{formaterMontantListe(pret.totalInterets)}</td>
                <td>
                  <Etiquette data-testid={`pret-statut-${pret.id}`}>{LIBELLES_STATUT[pret.statut]}</Etiquette>
                </td>
                <td>{pret.statut === 'EN_ATTENTE' ? T.enCoursDeTraitement : null}</td>
                <td />
              </tr>
            ))}
          </tbody>
        </Tableau>
      </Carte>
    </section>
  );
}
