import { useId, useState, type FormEvent } from 'react';
import {
  formaterMontantLigne,
  formaterMontantSimulation,
  SAISIE_SIMULATION_DEFAUT,
  type SaisieSimulation,
  type Simulation,
} from '../../../domain/pret';
import { simulerEmprunt, type ClientPrets } from '../../../application/prets';
import type { StockageJeton } from '../../../application/seConnecter';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { BoutonDepliant } from '../../ui/BoutonDepliant';
import { BoutonPrimaire } from '../../ui/BoutonPrimaire';
import { Carte } from '../../ui/Carte';
import { ChampSaisie } from '../../ui/ChampSaisie';
import { Tableau } from '../../ui/Tableau';
import './SectionSimulateur.css';

const T = TEXTES.prets.simulateur;

interface Proprietes {
  client: ClientPrets;
  stockage: StockageJeton;
  /** Session perdue pendant la simulation. */
  surDeconnexion: () => void;
}

/**
 * Section « Simulateur de pret » (FR-007-02, FR-007-03) : saisie préremplie, calcul par le service, résultat avec
 * échéancier repliable. Un refus du service affiche son message, une panne le texte fixe, sans résultat.
 */
export function SectionSimulateur({ client, stockage, surDeconnexion }: Proprietes) {
  const [saisie, setSaisie] = useState<SaisieSimulation>({ ...SAISIE_SIMULATION_DEFAUT });
  const [simulation, setSimulation] = useState<Simulation | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);
  const [ouvert, setOuvert] = useState(false);
  const [occupe, setOccupe] = useState(false);
  const idTitre = useId();
  const idEcheancier = useId();

  async function calculer(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    setOccupe(true);
    const resultat = await simulerEmprunt(client, stockage, saisie);
    if (resultat.type === 'deconnecte') {
      surDeconnexion();
      return;
    }
    setOccupe(false);
    setOuvert(false);
    if (resultat.type === 'simulation') {
      setErreur(null);
      setSimulation(resultat.simulation);
      return;
    }
    setSimulation(null);
    setErreur(resultat.type === 'refus' ? resultat.message : TEXTES.prets.erreurSimulation);
  }

  return (
    <section className="section-simulateur" aria-labelledby={idTitre}>
      <h2 id={idTitre} className="section-simulateur__separateur">
        {TEXTES.prets.separateurs.simulateur}
      </h2>
      <Carte>
        <h3 className="section-simulateur__titre">{T.titre}</h3>
        <form className="section-simulateur__formulaire" noValidate onSubmit={calculer}>
          <ChampSaisie
            id="pret-sim-montant"
            name="simMontant"
            libelle={T.montant}
            valeur={saisie.montant}
            onChange={(montant) => setSaisie({ ...saisie, montant })}
            modeSaisie="decimal"
            testId="pret-sim-montant"
          />
          <ChampSaisie
            id="pret-sim-taux"
            name="simTaux"
            libelle={T.taux}
            valeur={saisie.tauxAnnuel}
            onChange={(tauxAnnuel) => setSaisie({ ...saisie, tauxAnnuel })}
            modeSaisie="decimal"
            testId="pret-sim-taux"
          />
          <ChampSaisie
            id="pret-sim-duree"
            name="simDuree"
            libelle={T.duree}
            valeur={saisie.dureeMois}
            onChange={(dureeMois) => setSaisie({ ...saisie, dureeMois })}
            modeSaisie="numeric"
            testId="pret-sim-duree"
          />
          <BoutonPrimaire testId="pret-sim-calculer" occupe={occupe}>
            {T.calculer}
          </BoutonPrimaire>
        </form>
        {erreur !== null && (
          <AlerteErreur testId="pret-message-erreur">
            <p>{erreur}</p>
          </AlerteErreur>
        )}
        {simulation !== null && (
          <div className="section-simulateur__resultat">
            <dl className="section-simulateur__cartes">
              <div className="section-simulateur__carte">
                <dt className="section-simulateur__etiquette">{T.mensualite}</dt>
                <dd className="section-simulateur__valeur">{formaterMontantSimulation(simulation.mensualite)}</dd>
              </div>
              <div className="section-simulateur__carte">
                <dt className="section-simulateur__etiquette">{T.coutTotal}</dt>
                <dd className="section-simulateur__valeur">{formaterMontantSimulation(simulation.coutTotal)}</dd>
              </div>
              <div className="section-simulateur__carte">
                <dt className="section-simulateur__etiquette">{T.totalInterets}</dt>
                <dd className="section-simulateur__valeur">{formaterMontantSimulation(simulation.totalInterets)}</dd>
              </div>
            </dl>
            <BoutonDepliant
              testId="pret-sim-echeancier-lien"
              controle={idEcheancier}
              ouvert={ouvert}
              onBasculer={() => setOuvert(!ouvert)}
            >
              {T.lienEcheancier(simulation.echeancier.length)}
            </BoutonDepliant>
            <div id={idEcheancier} hidden={!ouvert}>
              <Tableau libelle={T.libelleEcheancier}>
                <thead>
                  <tr>
                    <th scope="col">{T.colonnes.numero}</th>
                    <th scope="col">{T.colonnes.capital}</th>
                    <th scope="col">{T.colonnes.interets}</th>
                    <th scope="col">{T.colonnes.total}</th>
                    <th scope="col">{T.colonnes.capitalRestant}</th>
                  </tr>
                </thead>
                <tbody>
                  {simulation.echeancier.map((ligne) => (
                    <tr key={ligne.numero}>
                      <td>{ligne.numero}</td>
                      <td>{formaterMontantLigne(ligne.capital)}</td>
                      <td>{formaterMontantLigne(ligne.interets)}</td>
                      <td>{formaterMontantLigne(ligne.total)}</td>
                      <td>{formaterMontantLigne(ligne.capitalRestant)}</td>
                    </tr>
                  ))}
                </tbody>
              </Tableau>
            </div>
          </div>
        )}
      </Carte>
    </section>
  );
}
