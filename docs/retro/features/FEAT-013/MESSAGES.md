---
feature: FEAT-013
name: Gestion des bénéficiaires externes
status: current
generated_by: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# Messages et libellés — FEAT-013 « Gestion des bénéficiaires externes »

Sources : `src/main/webapp/beneficiaires.xhtml` (libellés statiques),
`src/main/java/com/accenture/bank/web/BeneficiaireBean.java` (messages dynamiques — ajout, vérification IBAN, suppression).

> Note d'encodage : `beneficiaires.xhtml` utilise des caractères non accentués à plusieurs endroits
> (ex. « Beneficiaires », « Ajouter un beneficiaire ») — les textes ci-dessous reproduisent l'orthographe
> réelle du source. Les messages produits en Java (`BeneficiaireBean.java`) utilisent des accents.

---

## En-tête et navigation

| Identifiant | Texte exact (source) | Condition d'affichage | Preuve |
|---|---|---|---|
| MSG-013-001 | `BankApp Demo - Beneficiaires` | Titre du navigateur — toujours | |
| MSG-013-002 | `DB` | Logo — initiales dans le disque rouge | |
| MSG-013-003 | `Demo Bank` | Marque — toujours | |
| MSG-013-004 | `Bonjour, {beneficiaireBean.currentCustomer.fullName}` | Utilisateur connecté — toujours | |
| MSG-013-005 | `Deconnexion` | Lien déconnexion — toujours | |
| MSG-013-006 | `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 / WildFly 14 — Validation IBAN MOD-97 + BIC SEPA (zoneSEPA auto-detectee)` | Bandeau technique informatif — toujours | |

---

## Liens de navigation

### Toujours visibles

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-013-010 | `Tableau de bord` | |
| MSG-013-011 | `Virement` | |
| MSG-013-012 | `Virements programmes` | |
| MSG-013-013 | `Releve` | |
| MSG-013-014 | `Budget` | |
| MSG-013-015 | `Prets` | |
| MSG-013-016 | `Documents` | |
| MSG-013-017 | `Bourse` | |
| MSG-013-018 | `Mon Conseiller` | |
| MSG-013-019 | `Produits` | |
| MSG-013-020 | `Comptes Tiers` | |
| MSG-013-021 | `Beneficiaires` | actif sur cette page (`class="active"`) |

### Conditionnels (rôle)

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-013-022 | `Espace Conseiller` | `securityBean.adminOrConseiller` = true (CONSEILLER ou ADMIN) | |
| MSG-013-023 | `Administration` | `securityBean.admin` = true (ADMIN uniquement) | |

---

## Formulaire « Ajouter un bénéficiaire »

### Libellés statiques

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-013-030 | `Ajouter un beneficiaire` | Séparateur de section — toujours | |
| MSG-013-031 | `Nouveau beneficiaire SEPA` | Titre de section h2 — toujours | |
| MSG-013-032 | `Libelle *` | Label du champ libelle — toujours | |
| MSG-013-033 | `Nom du titulaire *` | Label du champ nomTitulaire — toujours | |
| MSG-013-034 | `IBAN *` | Label du champ iban — toujours | |
| MSG-013-035 | `ex: FR76 3000 1007 9412 3456 7890 185` | Placeholder du champ IBAN — toujours | |
| MSG-013-036 | `Verifier l'IBAN` | Bouton de vérification Ajax — toujours | |
| MSG-013-037 | `BIC` | Label du champ BIC — toujours | |
| MSG-013-038 | `(optionnel)` | Suffixe du label BIC — toujours | |
| MSG-013-039 | `ex: BNPAFRPPXXX` | Placeholder du champ BIC — toujours | |
| MSG-013-040 | `Banque` | Label du champ banque — toujours | |
| MSG-013-041 | `(optionnel)` | Suffixe du label banque — toujours | |
| MSG-013-042 | `ex: BNP Paribas` | Placeholder du champ banque — toujours | |
| MSG-013-043 | `Ajouter le beneficiaire` | Bouton de soumission — toujours | |

---

## Messages de vérification IBAN (Ajax)

Les messages ci-dessous sont produits par `BeneficiaireBean.verifierIban()` et affichés dans le panneau `ibanCheckPanel` sans soumission du formulaire. `{pays}` est le code ISO à 2 lettres extrait des deux premiers caractères de l'IBAN normalisé.

| Identifiant | Type | Texte exact | Condition de déclenchement | Preuve |
|---|---|---|---|---|
| MSG-013-060 | Erreur inline | `Veuillez saisir un IBAN.` | IBAN vide ou null à la vérification | 126 |
| MSG-013-061 | Erreur inline | `IBAN invalide (echec MOD-97). Verifiez la saisie.` | IBAN non vide mais échec MOD-97 | 129 |
| MSG-013-062 | Succès inline | `IBAN valide — {pays} (Zone SEPA)` | IBAN valide et code pays dans la liste SEPA | 135 |
| MSG-013-063 | Succès inline | `IBAN valide — {pays} (Hors zone SEPA)` | IBAN valide et code pays hors zone SEPA | 135 |

> Règles associées : RG-013-011, RG-013-012, RG-013-013.

---

## Messages de validation à la soumission (ajout)

Les messages ci-dessous sont produits par `BeneficiaireBean.ajouterBeneficiaire()` et affichés via `h:messages globalOnly="true"` dans le formulaire `addForm`.

| Identifiant | Sévérité | Texte exact | Condition de déclenchement | Preuve |
|---|---|---|---|---|
| MSG-013-070 | ERREUR | `Session expirée, veuillez vous reconnecter.` | `currentCustomer` null en session à la soumission | 65 |
| MSG-013-071 | ERREUR | `Le libellé est obligatoire.` | Champ `libelle` vide ou null à la soumission | 70 |
| MSG-013-072 | ERREUR | `L'IBAN est obligatoire.` | Champ `iban` vide ou null à la soumission | 75 |
| MSG-013-073 | ERREUR | `IBAN invalide. Veuillez saisir un IBAN correct (algorithme MOD-97).` | IBAN non vide mais MOD-97 échoue à la soumission | 79 |
| MSG-013-074 | ERREUR | `Le nom du titulaire est obligatoire.` | Champ `nomTitulaire` vide ou null à la soumission | 83 |
| MSG-013-075 | ERREUR | `BIC invalide. Format attendu : 8 ou 11 caractères alphanumériques.` | BIC fourni et ne correspond pas au pattern `^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$` | 88 |
| MSG-013-076 | INFO | `Bénéficiaire ajouté avec succès.` | Ajout persisté sans erreur | |
| MSG-013-077 | ERREUR | `Erreur lors de l'ajout du bénéficiaire : {message}` | Exception inattendue lors de la persistance (`{message}` = `e.getMessage()`) | |

> Règles associées : RG-013-015 à RG-013-020 (validations), RG-013-027 (succès).

---

## Message d'erreur à la suppression

| Identifiant | Sévérité | Texte exact | Condition de déclenchement | Preuve |
|---|---|---|---|---|
| MSG-013-080 | ERREUR | `Erreur lors de la suppression.` | Exception inattendue lors du soft-delete | 118 |

> Règle associée : RG-013-033.

---

## Tableau « Mes bénéficiaires enregistrés »

### Libellés statiques

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-013-090 | `Mes beneficiaires enregistres` | Séparateur de section — toujours | |
| MSG-013-091 | `Beneficiaires ({beneficiaireBean.beneficiaires.size()})` | Titre de section avec compteur — toujours | |
| MSG-013-092 | `Libelle` | En-tête de colonne | |
| MSG-013-093 | `Titulaire` | En-tête de colonne | |
| MSG-013-094 | `IBAN` | En-tête de colonne | |
| MSG-013-095 | `BIC` | En-tête de colonne | |
| MSG-013-096 | `Banque` | En-tête de colonne | |
| MSG-013-097 | `Zone SEPA` | En-tête de colonne | |
| MSG-013-098 | `Action` | En-tête de colonne | |
| MSG-013-099 | `Supprimer` | Bouton de suppression par ligne — toujours | |

### Valeurs de remplacement et badges conditionnels

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-013-100 | `Aucun beneficiaire enregistre. Utilisez le formulaire ci-dessus pour en ajouter un.` | État vide — aucun bénéficiaire actif pour l'utilisateur (référencé par RG-013-006) | 213 |
| MSG-013-101 | `—` | Colonne BIC — si `b.bic` vide ou null | |
| MSG-013-102 | `—` | Colonne Banque — si `b.bank` vide ou null | |
| MSG-013-103 | `✓ SEPA` | Badge zone SEPA — si `b.sepaZone` = true (rendu du caractère HTML `&#10003;`) | |
| MSG-013-104 | `Hors SEPA` | Badge hors zone SEPA — si `b.sepaZone` = false | |
