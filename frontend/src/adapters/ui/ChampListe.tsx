import type { ChangeEvent } from 'react';
import './ChampListe.css';

interface Proprietes {
  id: string;
  name: string;
  libelle: string;
  valeur: string;
  onChange: (valeur: string) => void;
  options: ReadonlyArray<{ valeur: string; libelle: string }>;
  testId: string;
}

/** Liste déroulante native avec libellé relié (label for) ; le libellé est mis en capitales par la page. */
export function ChampListe({ id, name, libelle, valeur, onChange, options, testId }: Proprietes) {
  return (
    <div className="champ-liste">
      <label className="champ-liste__libelle" htmlFor={id}>
        {libelle}
      </label>
      <select
        id={id}
        name={name}
        className="champ-liste__liste"
        value={valeur}
        onChange={(evenement: ChangeEvent<HTMLSelectElement>) => onChange(evenement.target.value)}
        data-testid={testId}
      >
        {options.map((option) => (
          <option key={option.valeur} value={option.valeur}>
            {option.libelle}
          </option>
        ))}
      </select>
    </div>
  );
}
