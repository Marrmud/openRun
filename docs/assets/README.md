# Images du README

| Fichier | Source | Rendu |
|---|---|---|
| `banner.png` | `banner.html` | 3000 × 900 (×3), fond transparent |
| `screens-placeholder.png` | `screens-placeholder.html` | 2000 × 940 (×2), en attendant les vraies captures |

Le mot-symbole (le « o » est la mascotte coureuse, en Fredoka 600) vient de la planche de marque
[`design/brand-sheet`](../../design/brand-sheet). Pour régénérer une image après avoir modifié sa source HTML
(Edge ou Chrome, avec accès à Google Fonts):

```powershell
msedge --headless=new --disable-gpu --hide-scrollbars --default-background-color=00000000 `
  --force-device-scale-factor=3 --window-size=1000,300 --virtual-time-budget=8000 `
  --screenshot=docs\assets\banner.png file:///C:/dev/openRun/docs/assets/banner.html
```
