import { useEffect, useId, useRef } from 'react';
import { formaterDateEcheance, formaterMontantLigne, type Echeance } from '../../../domain/pret';
import { TEXTES } from '../../textes';
import { BoutonTexte } from '../../ui/BoutonTexte';
import { Carte } from '../../ui/Carte';
import { Tableau } from '../../ui/Tableau';
import './SectionEcheancier.css';

const T = TEXTES.prets.echeancier;

interface Proprietes {
  /** Prêt dont l'échéancier est ouvert : le focus va au titre à chaque ouverture. */
  pretId: number;
  echeances: readonly Echeance[];
  surPayer: (echeanceId: number) => void;
}

/**
 * Section « Echeancier du pret selectionne » (FR-007-11, FR-007-12) : détail des échéances dans une carte discrète
 * sous la liste, dates « jj/MM/aaaa », « Marquer paye » seulement sur les échéances non payées.
 */
export function SectionEcheancier({ pretId, echeances, surPayer }: Proprietes) {
  const idSeparateur = useId();
  const titre = useRef<HTMLHeadingElement>(null);
  const C = T.colonnes;

  useEffect(() => {
    titre.current?.focus();
  }, [pretId]);

  return (
    <section className="section-echeancier" aria-labelledby={idSeparateur}>
      <h2 id={idSeparateur} className="section-echeancier__separateur">
        {T.separateur}
      </h2>
      <Carte variante="muted">
        <h3 ref={titre} tabIndex={-1} className="section-echeancier__titre">
          {T.titre}
        </h3>
        <Tableau libelle={T.titre}>
          <thead>
            <tr>
              <th scope="col">{C.numero}</th>
              <th scope="col">{C.dateEcheance}</th>
              <th scope="col">{C.capital}</th>
              <th scope="col">{C.interets}</th>
              <th scope="col">{C.total}</th>
              <th scope="col">{C.capitalRestant}</th>
              <th scope="col">{C.statut}</th>
              <th scope="col">{C.action}</th>
            </tr>
          </thead>
          <tbody>
            {echeances.map((echeance) => (
              <tr key={echeance.id}>
                <td>{echeance.numero}</td>
                <td>{formaterDateEcheance(echeance.dateEcheance)}</td>
                <td>{formaterMontantLigne(echeance.capital)}</td>
                <td>{formaterMontantLigne(echeance.interets)}</td>
                <td>{formaterMontantLigne(echeance.total)}</td>
                <td>{formaterMontantLigne(echeance.capitalRestant)}</td>
                <td>{echeance.paye ? T.paye : T.enAttente}</td>
                <td>
                  {!echeance.paye && (
                    <BoutonTexte data-testid={`pret-marquer-paye-${echeance.id}`} onClick={() => surPayer(echeance.id)}>
                      {T.marquerPaye}
                    </BoutonTexte>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </Tableau>
      </Carte>
    </section>
  );
}
