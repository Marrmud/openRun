# Design

## Principes

1. **Démarrer en une action.** L'accueil montre la séance du jour et un seul gros bouton Démarrer. Le reste est secondaire.
2. **Sobre.** Peu de cartes, beaucoup d'air, une seule couleur d'action. Les détails sont à un tap, jamais imposés.
3. **Lisible en courant.** Pendant la course: chiffres énormes, contraste fort, une information principale à la fois, boutons larges.
4. **Tout est ouvert.** Aucun programme ni semaine verrouillé. On choisit son point de départ.
5. **Célébrer, ne jamais culpabiliser.** Pas de "tu as raté", pas de série quotidienne punitive, repos = normal.
6. **Le débutant ne voit pas le jargon.** Les termes avancés sont derrière un glossaire contextuel.

## Système

- **Material Design 3** (Expressive), Jetpack Compose. *Pas* la lib React "Material UI".
- Thème **clair** d'abord, puis sombre et système. Couleurs dynamiques (Material You) activables.
- Maquette: couleur d'accent provisoire (vert-sarcelle). Les couleurs de phase sont des repères sémantiques:
  - Course = chaud (orange-rouge), Marche = froid (bleu), Échauffement / Retour au calme = neutre (vert doux).
  - Ne jamais coder la phase par la couleur seule (toujours un libellé et une icône), pour l'accessibilité.
- Typographie: Roboto / Roboto Flex, chiffres **tabulaires** (les valeurs ne "dansent" pas).
- Zones tactiles ≥ 48 dp, boutons principaux en course ≥ 64 dp de haut.

## Styles d'affichage de la séance (au choix, réglage + bascule en course)

| Style | Pour qui | Contenu |
|---|---|---|
| **Focus** | Débutant, soleil, vitesse | Un énorme compte à rebours + la phase en cours + la suivante. Rien d'autre. |
| **Chronologie** | "Je veux savoir où j'en suis" | Barre de la séance entière avec position + liste des intervalles à venir. |
| **Complet** | Avancé | Grille de métriques (temps, distance, allure, allure moyenne, FC, intervalle restant…). |

## Gamification (v0 de principe)

- **Objectif hebdomadaire** (ex. 3 séances) avec anneau/points. Un jour de repos ne casse jamais la série.
- **Rangs** Fer → Bronze → Argent → Or → Platine → Diamant, chacun en 3 divisions, avec un petit logo de coureur dans un écusson. La barre se remplit avec les **calories brûlées cumulées** (« 932 / 1 000 kcal »); à pleine barre, promotion (animation plus tard). « Niveau 2 » ne parle à personne, un rang oui.
- Pour donner du sens aux chiffres: **équivalences** (3,6 km = 9 tours de piste), total couru, calories si dispo.
- Seuils de calories et liste de badges: à définir (voir Questions ouvertes dans FEATURES.md).

## Montre (Wear OS)

- Thème **sombre** (OLED), écran rond ~450 px, marges de sécurité sur les bords.
- Course en 3 pages: **Focus** (compte à rebours en grand + arc de progression sur le bord), **Données** (allure, distance, temps), **Contrôles** (précédent, pause, suivant, verrouiller, terminer).
- Couleur de phase + libellé + haptique distincts (jamais la couleur seule).
- Maquette: `mockups/watch.html`.
- **Badges de jalons** liés au parcours, jamais à des comparaisons sociales.
- Fin de séance: écran de célébration (stats, XP gagnés, badge éventuel), puis "Comment c'était ?" (ressenti en 5 niveaux, utile au futur plan adaptatif).
- Ton: encourageant, court, sans jargon.

## Références analysées

| Appli | Ce qu'on prend | Ce qu'on laisse |
|---|---|---|
| C2K | Liste claire des programmes, aperçu de séance en feuille basse, paramètres simples | Tout est dans des cartes grises uniformes, peu de hiérarchie |
| RunDouble | Aperçu coloré des intervalles, choix du style d'écran de course | Fond sombre saturé, labels anglais mélangés, trop de tuiles |
| Decathlon Coach | Un gros bouton "Entraînement libre" bien visible | Bandeaux promo, contenu éditorial |
| openGym | Sobriété, bouton Démarrer central, cartes sobres, feuilles basses | Thème vert/sombre spécifique |

## Contraintes à donner à Claude Design

- Format de sortie: maquettes HTML interactives (comme `wear/mockups` dans openGym), téléphone d'abord, montre ensuite.
- Écran téléphone: ~412 × 915 dp (Pixel 9 Pro). Écran montre: rond ~450 px, marges de sécurité sur les bords.
- Material 3 Expressive, thème clair en premier, tokens de couleur définis une seule fois (pour basculer en sombre sans retoucher).
- Français par défaut, textes courts, tutoiement.
- En course: lisibilité en 1 seconde, mains moites, plein soleil.
- Les écrans à produire sont listés dans `mockups/index.html` (v0). À affiner, pas à jeter.
