#!/bin/sh
# $1: the root directory (e.g., /Users/miguel/Desktop/deployments)
# $2: the execution scenario (e.g., constant)
# $3: the architecture variant (e.g., proxy-cache-3.1)
#
# Usage: $0 /Users/miguel/Desktop/deployments constant proxy-cache-3.1
#
if [ $# -eq 0 ]; then
    error "Three arguments were expected" >&2
    exit 1
fi

# Directory where this script resides
# From: https://stackoverflow.com/a/44644933/738968
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Create the target directory for the charts (used in the R scripts)
mkdir -p "$1/$2-scenario-$3/charts"

echo "[1/4] Unifying the CSV files into all.csv"
Rscript "$DIR/unify-CSVs.R" "$1" "$2" "$3"

echo "[2/4] Comparing variants"
Rscript "$DIR/compare-all.R" "$1" "$2" "$3"

echo "[3/4] Generating plots"
Rscript "$DIR/constant-plots.R" "$1" "$2" "$3"

echo "[4/4] Generating summary plots"
Rscript "$DIR/constant-summary-plots.R" "$1" "$2" "$3"
