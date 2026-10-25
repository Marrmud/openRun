# La bible d'openRun

Document vivant. Chaque décision est datée et garde sa raison, pour ne pas la rediscuter par erreur.
Dernière mise à jour: 2026-10-25.

## 1. Vision

Une appli de course **complète et gratuite**, qui n'enferme aucune fonction utile derrière un paywall.

- **Pour le débutant:** s'ouvrir, voir "la séance du jour", appuyer sur Démarrer. Rien d'autre à comprendre.
- **Pour l'avancé:** séances structurées, calendrier, plans maison, zones, charge, export.
- **Pour tous:** local-first, sans compte, sans télémétrie, open source.

### Ce qui manque aux applis existantes (analyse de l'auteur, octobre 2026)

| Appli | Problème ressenti |
|---|---|
| RunDouble | Voix désactivée mais la musique/vidéo se met quand même en pause à chaque intervalle |
| JustRun, Couch25K | Fonctions utiles derrière paywall, ou trop limitées |
| Nike Run Club | Moyen |
| Kiprun, Decathlon Coach | Ne correspondent pas au besoin |
| C2K (hackerapps, GPL-3, F-Droid) | Très bonne base C25K/C210K, mais pas de Wear OS ni d'écriture santé poussée |

**Angle unique:** gratuit + open source + montre Wear OS autonome + plans libres + progression agréable.

## 2. Décisions actées

| # | Date | Décision | Raison |
|---|---|---|---|
| D1 | 2026-10-25 | Téléphone natif **Kotlin + Jetpack Compose**, Material 3 | Le GPS en arrière-plan doit survivre écran verrouillé. Dans openGym, le JS de la WebView est gelé ~1 min après verrouillage (mesuré), donc Capacitor est exclu. |
| D2 | 2026-10-25 | Montre **Compose for Wear OS + Health Services `ExerciseClient`** | Enregistrement autonome (GPS double fréquence, FC, auto-pause) sans téléphone. |
| D3 | 2026-10-25 | Données locales **Room (SQLite)**, aucun compte | Philosophie openGym: la donnée reste chez l'utilisateur. |
| D4 | 2026-10-25 | Module Kotlin pur `core/` testé (plans, allures, zones, XP) | Même règle qu'openGym: ce qui décide est une fonction pure avec test. Partagé téléphone/montre. |
| D5 | 2026-10-25 | **Pas de carte en v1** (la trace GPS est enregistrée quand même) | Réduit le time-to-market. Plus tard: MapLibre + OSM (sans clé API). |
| D6 | 2026-10-25 | Écriture de chaque séance dans **Health Connect** (visible dans Google Health) | Demande utilisateur. La montre enregistre, le téléphone écrit. |
| D7 | 2026-10-25 | Cible v1: Pixel + Pixel Watch 4. Ensuite autres Wear OS. iOS/Apple Watch beaucoup plus tard | Usage réel de l'auteur. Rester sur les API standard pour ne rien verrouiller sur Pixel. |
| D8 | 2026-10-25 | Projet **séparé d'openGym** | Roadmap propre. Intégration éventuelle bien plus tard. |
| D9 | 2026-10-25 | Open source, gratuit, pas de Play Store au départ | Objectif "open et free". Distribution: GitHub Releases, F-Droid. |
| D10 | 2026-10-25 | **Thème clair en premier**, puis sombre/système | Préférence de l'auteur. Material 3 (pas "Material UI" qui est une lib React). |
| D11 | 2026-10-25 | Gamification **légère et bienveillante** | Célébrer, jamais culpabiliser. Voir DESIGN.md. |
| D12 | 2026-10-25 | **Point de départ libre** dans les parcours: aucun programme ni semaine verrouillé | Demande explicite: chacun choisit où il en est. |
| D13 | 2026-10-25 | Le choix de nom provisoire est **openRun** | Recherche web (2026-10-25): aucun projet de ce nom exact trouvé. À revérifier GitHub/F-Droid avant publication. |
| D14 | 2026-10-25 | **Rangs façon jeu vidéo** (Fer, Bronze, Argent, Or, Platine, Diamant, avec divisions III/II/I) et un petit logo de coureur, à la place de « niveau 1/2 » | « Niveau 2 » ne dit rien; un rang se comprend sans explication. |
| D15 | 2026-10-25 | **La progression de rang = calories brûlées cumulées** (« 932 / 1 000 kcal »); à la barre pleine, on passe au rang suivant, avec une petite animation de promotion (plus tard). Les séances montrent aussi des équivalences: distance + tours de piste (3,6 km = 9 tours), total couru | Plus parlant que des points abstraits. Calories venant de la montre si dispo, sinon estimation depuis le poids (réglage « Mon poids »). Seuils par rang à définir. Point d'attention: les calories favorisent les coureurs lourds ou rapides, à garder en tête. |
| D16 | 2026-10-25 | Le **tracé GPS** (sans fond de carte) s'affiche en fin de séance et dans le détail; la carte viendra plus tard au même endroit | Valeur immédiate sans le coût d'une carte (D5). |
| D17 | 2026-10-25 | Données de la montre (FC, cadence, kcal, tours) affichées **si présentes**, avec la source indiquée | L'appli reste complète en « téléphone seul ». |
| D18 | 2026-10-25 | Contrôles de course: intervalle **précédent / suivant**, revenir à l'échauffement, recommencer l'intervalle, **terminer en avance** (enregistrer ou supprimer) | Demande explicite; une séance ne doit jamais être un tunnel. |
| D19 | 2026-10-25 | Choisir un jour d'un programme **valide automatiquement** les séances sautées (et l'inverse en reculant), avec confirmation et annulation possible | Demande explicite; garder une chronologie logique. Détail fonctionnel à spécifier plus tard. |
| D20 | 2026-10-25 | **Une phrase de conseil par semaine** de programme, utile et concrète | Ton pédagogique du débutant (comme C2K). |
| D21 | 2026-10-25 | Réglages: sections très visibles; désactiver « Voix » **masque ses options**; « Avec ta musique » disparaît si voix et bips sont coupés | Moins de bruit, plus intuitif. |
| D22 | 2026-10-25 | Historique: liste par date par défaut (simple, comme Google) + vue « par programme »; détail de séance avec tracé, FC, tours; calendrier mensuel avec séances faites/prévues | Demande explicite. |
| D23 | 2026-10-25 | Le réglage **Superposer / Baisser / Pause** vit dans la section « Voix et sons » (pas dans une section à part) | Il fait partie du volume et du son; masqué si voix et bips sont coupés. |
| D24 | 2026-10-25 | Montre: **thème sombre** (la maquette téléphone reste claire), 3 pages en course (Focus, Données, Contrôles), la montre peut tout faire seule | OLED, lisibilité, batterie, norme Wear OS. |
| D25 | 2026-10-25 | Le planning doit permettre **plusieurs séances le même jour**; le reste (calquage du calendrier sur le programme) est repoussé | Décision de principe, détails plus tard. |
| D26 | 2026-10-25 | **MVP = téléphone seul**; la montre vient juste après. Maquette montre validée plus tard, après la fin de l'appli mobile. Premier chantier: un **spike technique** (GPS écran verrouillé + audio sans casser la musique) avant toute UI | Le risque principal est la course elle-même. Voir [MVP.md](MVP.md). |

### Audio: règles non négociables

Cause du bug RunDouble: l'appli demande un *focus audio* qui force l'autre appli à se mettre en pause, même voix coupée.

- Trois modes pour la musique: **Superposer** (aucun focus demandé, la musique n'est jamais touchée), **Baisser** (focus "peut baisser"), **Pause**.
- Limite d'Android: on ne peut pas imposer "un peu / beaucoup" de baisse à l'autre appli. Compensation: régler **le volume de notre voix** et proposer le mode Superposer.
- Interrupteurs indépendants: voix, bips, vibrations téléphone, vibrations montre. "Tout couper" doit vraiment tout couper, et ne demander **aucun** focus audio.
- Test de non-régression obligatoire: voix off + Superposer pendant une vidéo YouTube = la vidéo ne bouge jamais.

### Santé

- Montre: `ExerciseClient` (GPS, FC, distance, tours, auto-pause). Le GPS double fréquence est utilisé automatiquement quand le matériel le supporte.
- Téléphone: reçoit la séance, écrit `ExerciseSessionRecord` (+ distance, vitesse, FC, trace) dans Health Connect.
- À régler: éviter le doublon avec l'enregistrement natif de la montre.

## 3. Risques et défis

| Risque | Gravité | Piste |
|---|---|---|
| Installer l'appli sur une montre sans Play Store (adb Wi-Fi) est réservé aux développeurs | Haute pour la diffusion | Guide pas à pas, mode "téléphone seul" complet, Play Store montre plus tard (25 $ une fois) |
| Ducking non réglable finement côté Android | Moyenne | Volume de voix + mode Superposer (voir ci-dessus) |
| Économie de batterie des constructeurs (Samsung, Xiaomi) tue le service GPS | Haute en multi-marques | Foreground service, demande d'exclusion batterie guidée, doc par marque |
| Précision GPS en ville / auto-pause | Moyenne | Filtrer par précision, lissage, tests en réel |
| Doublon de séance (montre + appli) dans Google Health | Moyenne | Identifiant de séance, ne pas lancer deux enregistrements |
| Ne pas compiler à l'aveugle: l'assistant ne peut pas toujours lancer Gradle | Moyenne | Petites tranches, l'auteur compile/colle les erreurs (comme sur openGym) |
| Conseils d'entraînement erronés ou dangereux | Haute | Progression prudente (règle des 10 %), avertissements, aucune promesse médicale |

## 4. Licence (à trancher)

GPL-3.0-or-later proposée: copyleft qui garde l'appli libre, compatible F-Droid et avec C2K (GPL-3). À confirmer avant le premier commit de code.
Si on s'inspire des *données* des programmes C25K, vérifier leur statut (les plans classiques sont d'usage courant, mais on reformule nous-mêmes).

## 5. Journal de bord

- 2026-10-25: cadrage, décisions D1 à D13, maquette téléphone v0.
- 2026-10-25: T3 fait: coque Compose Material 3 (thème clair aux couleurs de la maquette) avec Accueil, « Où en es-tu ? », Parcours, Programme (conseil par semaine, saut de jour avec confirmation), Aperçu de séance, Réglages complets (voix masque ses options, musique dans « Voix et sons »). Séries hebdomadaires dans `core/Weeks` (9 tests). Écran de course et Historique: provisoires. Vérifié sur le Pixel par captures.
- 2026-10-25: T2 fait: module `:app` (applicationId `org.openrun.app`), Room 2.8.5 + KSP sur AGP 9, DataStore. Tables: séance, points GPS, résultats par phase, séances faites. Réglages = `AppSettings` dans `core/` (règle testée: aucun focus audio si voix et bips sont coupés, ou en mode Superposer). 6 tests purs + 16 tests sur téléphone.
- 2026-10-25: T0 spike validé à l'oreille (Superposer / Baisser / Pause OK, voir T0-SPIKE.md); T1 `core/` fait: 4 programmes (Pré-C25K 9 séances, C25K 27, Pont 18, C210K 42), progression avec sauts de jours, calories (ACSM), rangs, distance GPS, moteur de séance (suivant, précédent, recommencer, longs écarts). 52 tests.
- 2026-10-25: rangs aux calories (D15), réglage musique déplacé (D23), maquette montre v0 (`mockups/watch.html`), D23 à D25.
- 2026-10-25: retours sur la maquette v0, décisions D14 à D22, maquette v1 (13 écrans: + programme, terminer en avance, historique, détail, calendrier).
