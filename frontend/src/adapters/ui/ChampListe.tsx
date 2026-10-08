import type { ChangeEvent, Ref } from 'react';
import './ChampListe.css';

interface Proprietes {
  id: string;
  name: string;
  libelle: string;
  valeur: string;
  onChange: (valeur: string) => void;
  options: ReadonlyArray<{ valeur: string; libelle: string }>;
  testId: string;
  /** Première option, valeur vide : aucun choix. Sa présence rend le choix obligatoire (aria-required). */
  libelleVide?: string;
  /** Champ en erreur dont le message est affiché ailleurs : aria-invalid et bordure d'erreur. */
  invalide?: boolean;
  ref?: Ref<HTMLSelectElement>;
}

/** Liste déroulante native avec libellé relié (label for) ; le libellé est mis en capitales par la page. */
export function ChampListe({ id, name, libelle, valeur, onChange, options, testId, libelleVide, invalide = false, ref }: Proprietes) {
  return (
    <div className="champ-liste">
      <label className="champ-liste__libelle" htmlFor={id}>
        {libelle}
      </label>
      <select
        ref={ref}
        id={id}
        name={name}
        className={invalide ? 'champ-liste__liste champ-liste__liste--erreur' : 'champ-liste__liste'}
        aria-required={libelleVide === undefined ? undefined : 'true'}
        aria-invalid={invalide ? 'true' : undefined}
        value={valeur}
        onChange={(evenement: ChangeEvent<HTMLSelectElement>) => onChange(evenement.target.value)}
        data-testid={testId}
      >
        {libelleVide !== undefined && <option value="">{libelleVide}</option>}
        {options.map((option) => (
          <option key={option.valeur} value={option.valeur}>
            {option.libelle}
          </option>
        ))}
      </select>
    </div>
  );
}
