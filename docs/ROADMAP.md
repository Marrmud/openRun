# Roadmap

Principe: petites tranches testables, la logique d'abord (`core/` pur + tests), l'UI ensuite.

| Phase | Contenu | Livrable |
|---|---|---|
| 0. Cadrage | Vision, décisions, features, design | Ce dossier (en cours) |
| 1. Maquettes | Téléphone v0, puis affinage dans Claude Design, puis montre | `mockups/` validées |
| 2. Fondations | Repo Android, module `core/`, modèle de séance (blocs, intervalles), programmes C25K/C210K en données | Tests verts, pas encore d'UI |
| 3. Course (téléphone) | Foreground service GPS, minuteur d'intervalles, audio (voix, bips, vibrations, modes musique), écrans Focus/Chronologie/Complet | Une séance C25K faisable en vrai |
| 4. Parcours et historique | Point de départ libre, accueil, historique, fin de séance + XP | MVP téléphone |
| 5. Santé | Écriture Health Connect | Séance visible dans Google Health |
| 6. Montre | Enregistrement autonome `ExerciseClient`, sync téléphone | MVP montre (Pixel Watch 4) |
| 7. v1 | Constructeur de séances, calendrier, plans par objectif, adaptatif, zones/VDOT, export | Appli complète |
| 8. Diffusion | Guide d'installation, F-Droid, autres Wear OS | Release publique |
| 9. Plus tard | Carte (fin de séance et détail), charge, chaussures, page Guide/FAQ enrichie, iOS/Apple Watch | |
