# Fonctionnalités

Paliers proposés: **MVP** (utilisable pour courir), **v1** (appli complète), **Plus tard**.
Le périmètre précis du MVP, qui fait foi, est dans [MVP.md](MVP.md). « MVP montre » = juste après le MVP téléphone.

## Course (enregistrement)

| Fonction | Palier |
|---|---|
| Chrono, distance, allure, durée, dénivelé via GPS | MVP |
| Auto-pause, enregistrement écran verrouillé (foreground service) | MVP |
| Mode tapis (sans GPS) | MVP |
| Garder l'écran allumé (option) | MVP |
| Passer l'intervalle en cours, passer l'échauffement / le retour au calme | MVP |
| Verrouillage d'écran pendant la course | MVP |
| Intervalle précédent / suivant, revenir à l'échauffement, recommencer l'intervalle | MVP |
| Terminer en avance: enregistrer ou supprimer | MVP |
| 3 styles d'affichage de séance: Focus, Chronologie, Complet | MVP |
| Tours manuels et automatiques | v1 |
| Cibles d'allure ou de FC avec alerte | v1 |
| Cadence, longueur de foulée (capteurs montre) | Plus tard |

## Audio et retours

| Fonction | Palier |
|---|---|
| Voix on/off, volume de voix | MVP |
| Bips on/off, compte à rebours | MVP |
| Vibrations téléphone / montre on/off, intensité | MVP |
| Mode musique: Superposer / Baisser / Pause | MVP |
| Encouragements en milieu de course, rappel du temps | v1 |
| Choix de la voix / langue TTS | v1 |

## Programmes et plans

| Fonction | Palier |
|---|---|
| Pré-C25K, C25K, Pont vers 10K, C210K | MVP |
| Coureur d'une heure et programmes avancés | v1 |
| Course libre (sans programme) | MVP |
| Point de départ libre (n'importe quel programme, semaine, jour) | MVP |
| Questionnaire "Où en es-tu ?" qui propose le bon point de départ | MVP |
| Constructeur de séances (échauffement, blocs répétés, récup, retour au calme) | v1 |
| Calendrier, glisser-déposer, rappels | v1 |
| Plans par objectif et date de course (10K en 55 min…) | v1 |
| Plan adaptatif: séance ratée, le plan se recale; ressenti post-séance | v1 |
| Partage/import de plans (fichier JSON) | v1 |

## Données et santé

| Fonction | Palier |
|---|---|
| Historique (par date / par programme), détail de séance, tours | MVP |
| Tracé GPS sans fond de carte (fin de séance et détail) | MVP |
| Calendrier mensuel (faites / prévues), replanifier une séance | v1 |
| Choisir un jour de programme: valide/annule automatiquement les séances intermédiaires | MVP |
| Écriture Health Connect (Google Health) | MVP |
| Records personnels | v1 |
| Export GPX/FIT, import GPX | v1 |
| Zones FC et allure, VDOT, prédicteur de temps de course | v1 |
| Charge d'entraînement, alerte hausse de volume > 10 %/semaine | Plus tard |
| Suivi des chaussures (km) | Plus tard |
| Carte (MapLibre + OSM) | Plus tard |
| Météo | Plus tard |

## Guide

| Fonction | Palier |
|---|---|
| Page Guide / FAQ (comme C2K): échauffement, allure, respiration, blessures, quand passer à la semaine suivante | v1 |
| Conseil de la semaine affiché dans le programme et dans l'aperçu de séance | MVP |

## Gamification (légère)

| Fonction | Palier |
|---|---|
| Objectif **hebdomadaire** (pas de série quotidienne), jours de repos qui ne cassent rien | MVP |
| **Rangs** Fer → Diamant (divisions III/II/I) avec logo de coureur, progression par calories cumulées | MVP |
| Poids de l'utilisateur pour estimer les calories sans montre | MVP |
| Animation de passage de rang | Plus tard |
| Équivalences: tours de piste, total couru | MVP |
| Écran de fin de séance qui célèbre (stats, XP, badge) | MVP |
| Badges de jalons (1re minute de course continue, 1er 5K, 1er 10K…) | v1 |
| Chemin visuel de progression entre programmes | MVP |
| Défis personnels (battre son temps, ghost de sa séance passée) | Plus tard |

## Montre (Wear OS)

| Fonction | Palier |
|---|---|
| Enregistrement autonome sans téléphone | MVP montre |
| Séance guidée par vibrations/voix depuis la montre | MVP montre |
| Complications / tuile "séance du jour" | v1 montre |
| Synchronisation vers le téléphone, écriture Health Connect | MVP montre |

## Autorisations et batterie (décisions D32 à D35)

| Fonction | Palier |
|---|---|
| Suivi GPS **optionnel, désactivé par défaut**: la position n'est demandée qu'en l'activant (remplace le « mode tapis ») | v1 (juste après les premiers retours terrain) |
| Écran d'introduction après « Où en es-tu ? »: explique et propose, avec un interrupteur par fonction (GPS, Health Connect, rappels), la boîte système n'apparaissant qu'à l'activation | v1 |
| État de la batterie dans Réglages (« Sans restriction » ou « À corriger ») + bouton vers le réglage Android + guide au premier usage du GPS | v1 |
| Rappels de séance (notifications), proposés à l'introduction et demandés à l'activation, avec le calendrier | v1 |

## Retours de la première sortie (décisions D36 à D40)

| Fonction | Palier |
|---|---|
| Couleurs des phases: course vert plein, marche jaune, échauffement / retour au calme neutre; écran de course teinté par la phase; pastille d'icône par phase; hauteurs différentes dans la barre | Fait (écran de course), reste des écrans à la refonte v2 |
| Fin de séance: un seul bouton jaune qui pulse, puce « Séance terminée », anneau plein avec coche | Fait |
| Bips au volume maximum par défaut + réglage de volume des bips | Fait |
| Signal de départ et d'arrivée (désactivable) | Fait |
| Plus de bip de changement de phase quand le compte à rebours est actif (gardé s'il est coupé) | Fait |
| Compteur des marches (« Marche · 3 / 6 »), comme pour les courses | Fait |

## Paramètres

Langue, unités, thème (clair, sombre, système), couleurs dynamiques, audio (voir BIBLE.md), garder l'écran allumé, mode tapis, auto-pause, Health Connect, exclusion batterie guidée, export/sauvegarde des données.

## Questions ouvertes

- Questionnaire de départ: combien de questions, et quelles réponses mènent à quel programme ?
- Plan adaptatif: quelles règles exactes, et quand refuser de monter la charge ?
- Seuils de calories par rang et par division; liste de badges.
- Planning: plusieurs séances le même jour (décidé en principe), et comment le calendrier se cale sur un programme.
- Annulation des séances validées automatiquement quand on saute des jours d'un programme: où et comment ?
