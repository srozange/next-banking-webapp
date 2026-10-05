# FLOW — FEAT-004 Virements programmés

```mermaid
stateDiagram-v2
    [*] --> Vide
    Vide --> Chargé : GET /banking-legacy/virement-programme.xhtml (utilisateur avec virements existants)
    Vide --> Vide : GET /banking-legacy/virement-programme.xhtml (utilisateur sans virements ni comptes)
    Chargé --> Erreur : POST vpForm:j_idt29 (formulaire vide soumis — compte source non sélectionné)
    Chargé --> Erreur : POST vpForm:j_idt29 (format de date invalide : dd/MM/yyyy au lieu de yyyy-MM-dd attendu)
    Chargé --> Confirmation : POST vpForm:j_idt29 (formulaire valide soumis)
    Confirmation --> Chargé : navigation ou rechargement de la page
```

## Captures par état

| État | Screenshot | Rôle | Notes |
|---|---|---|---|
| Chargé | SCR-004-01_charge.png | CLIENT | La page affiche simultanément le formulaire de création (vide) et la liste des virements programmés existants. Deux sections distinctes : « Nouveau virement programmé » et « Mes virements programmés ». |
| Vide | SCR-004-04_vide.png | ADMIN | Utilisateur sans comptes bancaires : la liste affiche « Aucun virement programme configure ». Le dropdown Compte débiteur ne contient que l'option « -- Choisir -- ». |
| Erreur | SCR-004-02_erreur.png | CLIENT | Validation serveur JSF : message « Veuillez selectionner un compte source » affiché dans un tableau d'erreurs au-dessus du formulaire. Le formulaire reste affiché avec les valeurs saisies. |
| Confirmation | SCR-004-03_confirmation.png | CLIENT | Message de succès : « Virement programme cree avec succes ». La liste se rafraîchit avec le nouveau virement. Pas de modale de confirmation — la soumission est immédiate et irréversible. |