# MVP (validé le 2026-10-25)

## Objectif

> Faire sa séance du jour de bout en bout avec **l'appli téléphone seule**: ouvrir, voir la séance, démarrer,
> courir téléphone en poche écran verrouillé, être guidé, terminer, retrouver la séance dans l'historique et
> dans Google Health.

**Critère de réussite (le vrai test):** l'auteur fait ses séances Du canapé au 5 km / 10 km avec openRun à la place de C2K
pendant 2 semaines, avec sa musique ou une vidéo, sans une seule pause intempestive ni séance perdue.

**Hors périmètre du MVP: la montre.** Elle vient juste après (phase 6), une fois le téléphone validé.

## Dedans

### Course
- Programmes guidés: **Pré-C25K, Du canapé au 5 km, Pont vers 10K, Du canapé au 10 km** (données écrites par nous, reformulées).
- **Course libre** (chrono + distance, sans programme).
- Enregistrement GPS écran verrouillé (foreground service), distance, allure, durée, auto-pause, mode tapis.
- 3 styles d'écran: Focus, Chronologie, Complet, bascule en course.
- Intervalle précédent / suivant, revenir à l'échauffement, recommencer l'intervalle, terminer en avance (enregistrer / supprimer).
- Aperçu de séance, ignorer échauffement / retour au calme, conseil de la semaine.
- Verrouillage d'écran, garder l'écran allumé.

### Audio et retours
- Voix (TTS hors ligne) avec volume propre, bips, compte à rebours, vibrations téléphone (+ intensité).
- Un interrupteur par canal; « tout couper » ne demande aucun focus audio.
- Mode musique: **Superposer / Baisser / Pause** (défaut: Superposer).

### Parcours et suivi
- Question « Où en es-tu ? » + choix libre du programme, de la semaine et du jour.
- Sauter à un jour futur: les séances intermédiaires sont validées après confirmation (annulation possible dans l'historique).
- Accueil: séance du jour, semaine (objectif hebdomadaire), prochain jalon.
- **Rangs** Fer → Diamant à la **calorie cumulée** (formule simple depuis le poids et la distance; sans animation).
- Fin de séance: stats, tracé GPS, équivalence en tours de piste, rang, ressenti en un tap.
- Historique **par date** + détail d'une séance (tracé, tours).

### Santé et réglages
- Écriture **Health Connect** de chaque séance.
- Réglages: voix et sons, vibrations, séance, poids, santé, thème **clair**, langue (français d'abord).

## Dehors (v1 ou plus tard)

| Sujet | Quand |
|---|---|
| Montre Wear OS (enregistrement autonome, sync) | Juste après le MVP |
| Thème sombre / système, couleurs dynamiques | v1 (le MVP est conçu en tokens pour que ce soit facile) |
| Calendrier, replanifier, plusieurs séances par jour | v1 |
| Constructeur de séances, plans par objectif, plan adaptatif | v1 |
| Historique « par programme », export GPX, import | v1 |
| Records, zones FC/allure, VDOT, prédicteur | v1 |
| « Coureur d'une heure » et programmes avancés | v1 |
| Badges de jalons, animation de promotion de rang | v1 |
| Page Guide / FAQ | v1 |
| Cibles d'allure ou de FC avec alerte | v1 |
| Carte (fond OSM) | Plus tard |
| Charge d'entraînement, chaussures, météo | Plus tard |
| iOS / Apple Watch | Beaucoup plus tard |

## Tranches de construction (dans cet ordre)

Le risque le plus élevé est la course elle-même (GPS écran verrouillé + audio sans casser la musique). On le teste **en premier**, avant toute UI.

| # | Tranche | Livrable vérifiable |
|---|---|---|
| T0 | **Spike technique** | Mini-appli sans UI soignée: foreground service GPS + minuteur + voix + bips + vibrations. Test: voix coupée pendant une vidéo YouTube = aucune pause; écran verrouillé 30 min = trace complète. Décide d'abord si l'architecture tient. |
| T1 | Repo Android + module `core/` | Projet Gradle, CI de tests. Modèle de séance, données des 4 programmes, allure, calories, rang, validation de jours sautés: **fonctions pures testées**. |
| T2 | Stockage + réglages | Room (séances, points GPS, programme en cours), DataStore (réglages). |
| T3 | Coque UI Material 3 | Navigation, Accueil, Où en es-tu ?, Programme, Aperçu de séance. |
| T4 | Écran de course | Les 3 styles, contrôles d'intervalle, terminer en avance, branché sur le moteur de T0. |
| T5 | Fin de séance + Historique + Détail | Tracé, équivalences, rang, ressenti, liste par date. |
| T6 | Health Connect | Séance visible dans Google Health, sans doublon. |
| T7 | Finitions + diffusion | Réglages complets, FR, README d'installation, release APK sur GitHub (F-Droid ensuite). |

## Définition de « terminé » pour le MVP

- Les tests de `core/` passent (programmes, calories, rang, sauts de jours).
- Le test T0 (YouTube, écran verrouillé) est rejoué sur l'appli finale.
- Une séance complète de chaque programme se déroule sans erreur sur le Pixel de l'auteur.
- Séance visible dans Google Health, une seule fois.
- Aucune permission demandée sans explication; l'appli fonctionne sans compte ni réseau.

## Points tranchés

1. **Seuils de rang:** **1 000 kcal par division** pour tout, provisoire, dans une seule constante; à affiner avec de vraies données.
2. **« Coureur d'une heure »:** repoussé en v1.
3. **Langues:** français seul au MVP, anglais en v1 (les textes vivent déjà dans des ressources traduisibles).
4. **Course libre:** incluse au MVP.
