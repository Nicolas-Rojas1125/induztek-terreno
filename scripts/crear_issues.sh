#!/usr/bin/env bash
# Crea etiquetas, hito e issues del encargo en GitHub a partir de docs/tareas_trello.csv
# Requisitos: GitHub CLI (gh) autenticado -> gh auth login
# Uso: bash scripts/crear_issues.sh USUARIO/induztek-terreno
set -euo pipefail
REPO="${1:?Indique el repositorio: USUARIO/induztek-terreno}"
CSV="$(dirname "$0")/../docs/tareas_trello.csv"

for etiqueta in "Análisis:bfbfbf" "Dominio:7057ff" "Datos:0075ca" "UI:0e8a16" "Nativo:d93f0b" "Calidad:b60205" "Gestión:fbca04" "Documentación:fef2c0" "Seguridad:5319e7" "Backend:1d76db"; do
  gh label create "${etiqueta%%:*}" --color "${etiqueta##*:}" --repo "$REPO" --force >/dev/null
done
gh api "repos/$REPO/milestones" -f title="Encargo EP2" -f description="Entrega semana 11" >/dev/null 2>&1 || true

# Columnas: Lista,Tarjeta,Descripcion,Responsable,Semana,Etiquetas,Requerimientos
python3 - "$CSV" <<'PY' | while IFS=$'\t' read -r titulo cuerpo etiqueta; do
import csv, sys
for f in csv.DictReader(open(sys.argv[1], encoding="utf-8")):
    cuerpo = f"{f['Descripcion']}\n\n- Responsable: {f['Responsable']}\n- Semana: {f['Semana']}\n- Requerimientos: {f['Requerimientos']}"
    plano = cuerpo.replace(chr(10), chr(92) + "n")
    print(f["Tarjeta"] + chr(9) + plano + chr(9) + f["Etiquetas"])
PY
  gh issue create --repo "$REPO" --title "$titulo" --body "$(printf '%b' "$cuerpo")" --label "$etiqueta" >/dev/null
  echo "Issue creado: $titulo"
done
echo "Listo. Asigne cada issue a su responsable y ciérrelo con 'Closes #N' en el commit o PR."
