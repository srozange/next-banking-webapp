import type { ChangeEvent, Ref } from 'react';
import './ChampTexte.css';

interface Proprietes {
  id: string;
  name: string;
  type: 'text' | 'password';
  libelle: string;
  valeur: string;
  onChange: (valeur: string) => void;
  testId: string;
  autoComplete?: string;
  /** Nombre de caractères que le champ accepte, sans message à la saisie. */
  maxLength?: number;
  placeholder?: string;
  /** Texte d'aide sous le champ, relié par aria-describedby. */
  aide?: string;
  /** Message d'erreur affiché sous le champ, relié par aria-describedby. */
  erreur?: string;
  /** Aide affichée sous le champ, reliée par aria-describedby. */
  indice?: string;
  /** Champ facultatif : ni astérisque ni aria-required. */
  facultatif?: boolean;
  inputMode?: 'text' | 'decimal';
  ref?: Ref<HTMLInputElement>;
}

/** Champ obligatoire avec libellé relié (label for) ; l'astérisque est purement visuel (CSS). */
export function ChampTexte({
  id,
  name,
  type,
  libelle,
  valeur,
  onChange,
  testId,
  autoComplete,
  maxLength,
  placeholder,
  aide,
  erreur,
  indice,
  facultatif = false,
  inputMode,
  ref,
}: Proprietes) {
  const idErreur = `${id}-erreur`;
  const idAide = `${id}-aide`;
  const idIndice = `${id}-indice`;
  const decrit = [
    aide === undefined ? null : idAide,
    indice === undefined ? null : idIndice,
    erreur === undefined ? null : idErreur,
  ].filter((cible) => cible !== null);
  return (
    <div className="champ-texte">
      <label className={facultatif ? 'champ-texte__libelle champ-texte__libelle--facultatif' : 'champ-texte__libelle'} htmlFor={id}>
        {libelle}
      </label>
      <input
        ref={ref}
        id={id}
        name={name}
        type={type}
        className={erreur === undefined ? 'champ-texte__saisie' : 'champ-texte__saisie champ-texte__saisie--erreur'}
        value={valeur}
        onChange={(evenement: ChangeEvent<HTMLInputElement>) => onChange(evenement.target.value)}
        autoComplete={autoComplete}
        maxLength={maxLength}
        placeholder={placeholder}
        inputMode={inputMode}
        aria-required={facultatif ? undefined : 'true'}
        aria-invalid={erreur === undefined ? undefined : 'true'}
        aria-describedby={decrit.length === 0 ? undefined : decrit.join(' ')}
        data-testid={testId}
      />
      {aide !== undefined && (
        <p id={idAide} className="champ-texte__aide">
          {aide}
        </p>
      )}
      {indice !== undefined && (
        <p id={idIndice} className="champ-texte__indice">
          {indice}
        </p>
      )}
      {erreur !== undefined && (
        <p id={idErreur} className="champ-texte__erreur">
          {erreur}
        </p>
      )}
    </div>
  );
}
