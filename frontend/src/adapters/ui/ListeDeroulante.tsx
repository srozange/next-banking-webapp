import type { ChangeEvent, Ref } from 'react';
import './ListeDeroulante.css';

export interface OptionListe {
  valeur: string;
  libelle: string;
}

interface Proprietes {
  id: string;
  name: string;
  libelle: string;
  /** Première option, valeur vide : aucun choix. */
  libelleVide: string;
  options: readonly OptionListe[];
  valeur: string;
  onChange: (valeur: string) => void;
  testId: string;
  erreur?: string;
  ref?: Ref<HTMLSelectElement>;
}

/** Liste déroulante native, libellé relié (label for), mêmes erreurs que ChampTexte. */
export function ListeDeroulante({ id, name, libelle, libelleVide, options, valeur, onChange, testId, erreur, ref }: Proprietes) {
  const idErreur = `${id}-erreur`;
  return (
    <div className="liste-deroulante">
      <label className="liste-deroulante__libelle" htmlFor={id}>
        {libelle}
      </label>
      <select
        ref={ref}
        id={id}
        name={name}
        className={erreur === undefined ? 'liste-deroulante__saisie' : 'liste-deroulante__saisie liste-deroulante__saisie--erreur'}
        value={valeur}
        onChange={(evenement: ChangeEvent<HTMLSelectElement>) => onChange(evenement.target.value)}
        aria-required="true"
        aria-invalid={erreur === undefined ? undefined : 'true'}
        aria-describedby={erreur === undefined ? undefined : idErreur}
        data-testid={testId}
      >
        <option value="">{libelleVide}</option>
        {options.map((option) => (
          <option key={option.valeur} value={option.valeur}>
            {option.libelle}
          </option>
        ))}
      </select>
      {erreur !== undefined && (
        <p id={idErreur} className="liste-deroulante__erreur">
          {erreur}
        </p>
      )}
    </div>
  );
}
