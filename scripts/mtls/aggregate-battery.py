#!/usr/bin/env python3
"""
aggregate-battery.py - offline aggregation of the raw battery CSV.

Reads rezultate-baterie-brut.csv (one row per measurement: mod,rep,nivel,...) and writes
rezultate-baterie-agregat.csv with mean +/- sample standard deviation per (mod, nivel) over
the repetitions, for the metrics that matter: delivery rate, publish throughput, and latency
(mean / p50 / p95 / p99 over successful requests).

It also prints, to stdout:
  * the aggregate table,
  * Experiment 3 overhead (secure - insecure) per level (absolute ms and %),
  * the control-drift check (insecure_control vs the initial insecure block, same levels).

Pure stdlib; no modification of any measurement script.
"""
import csv, statistics, sys
from collections import defaultdict

RAW = sys.argv[1] if len(sys.argv) > 1 else "rezultate-baterie-brut.csv"
OUT = sys.argv[2] if len(sys.argv) > 2 else "rezultate-baterie-agregat.csv"

# metric columns in the raw csv we aggregate (name -> csv field)
METRICS = ["rata_livrare", "throughput_publicare", "latenta_medie_ok", "p50_ok", "p95_ok", "p99_ok"]

rows = []
with open(RAW) as f:
    for r in csv.DictReader(f):
        r["nivel"] = int(r["nivel"])
        for m in METRICS:
            r[m] = float(r[m])
        rows.append(r)

# group by (mod, nivel)
groups = defaultdict(list)
for r in rows:
    groups[(r["mod"], r["nivel"])].append(r)

def mean_sd(vals):
    m = statistics.mean(vals)
    sd = statistics.stdev(vals) if len(vals) > 1 else 0.0
    return m, sd

# build aggregate
agg = {}
for (mod, lvl), rs in groups.items():
    entry = {"mod": mod, "nivel": lvl, "n_runs": len(rs)}
    for m in METRICS:
        mu, sd = mean_sd([r[m] for r in rs])
        entry[m + "_mean"] = mu
        entry[m + "_sd"] = sd
    agg[(mod, lvl)] = entry

# write aggregate csv
fields = ["mod", "nivel", "n_runs"] + [m + s for m in METRICS for s in ("_mean", "_sd")]
with open(OUT, "w", newline="") as f:
    w = csv.DictWriter(f, fieldnames=fields)
    w.writeheader()
    for key in sorted(agg, key=lambda k: (k[0], k[1])):
        row = {k: (f"{v:.3f}" if isinstance(v, float) else v) for k, v in agg[key].items()}
        w.writerow(row)

print(f"wrote {OUT}\n")

# ---- pretty aggregate table ----
def fmt(mod, lvl, key):
    e = agg.get((mod, lvl))
    if not e: return "  -  "
    return f"{e[key+'_mean']:.2f}±{e[key+'_sd']:.2f}"

levels = sorted({l for (_, l) in agg})
print("=== AGGREGATE (mean ± sd over reps) ===")
for mod in ("insecure", "secure", "insecure_control"):
    if not any(m == mod for (m, _) in agg): continue
    print(f"\n[{mod}]  (n_runs={agg[(mod, levels[0] if (mod,levels[0]) in agg else [l for (mm,l) in agg if mm==mod][0])]['n_runs']})")
    print(f"  {'lvl':>4} | {'deliv':>11} | {'tput/s':>14} | {'lat_mean ms':>14} | {'p95 ms':>14} | {'p99 ms':>14}")
    for l in levels:
        if (mod, l) not in agg: continue
        print(f"  {l:>4} | {fmt(mod,l,'rata_livrare'):>11} | {fmt(mod,l,'throughput_publicare'):>14} | "
              f"{fmt(mod,l,'latenta_medie_ok'):>14} | {fmt(mod,l,'p95_ok'):>14} | {fmt(mod,l,'p99_ok'):>14}")

# ---- Experiment 3: security overhead (secure - insecure) per level ----
print("\n=== EXP.3 security overhead (secure - insecure), latency mean ===")
print(f"  {'lvl':>4} | {'insec ms':>10} | {'sec ms':>10} | {'abs ms':>9} | {'overhead %':>10} | {'tput insec':>11} | {'tput sec':>10} | {'tput drop %':>11}")
for l in levels:
    a = agg.get(("insecure", l)); b = agg.get(("secure", l))
    if not a or not b: continue
    ai, bi = a["latenta_medie_ok_mean"], b["latenta_medie_ok_mean"]
    at, bt = a["throughput_publicare_mean"], b["throughput_publicare_mean"]
    absd = bi - ai
    pct = (absd / ai * 100) if ai else 0.0
    tdrop = ((at - bt) / at * 100) if at else 0.0
    print(f"  {l:>4} | {ai:>10.2f} | {bi:>10.2f} | {absd:>9.2f} | {pct:>9.1f}% | {at:>11.1f} | {bt:>10.1f} | {tdrop:>10.1f}%")

# ---- control drift: insecure_control vs initial insecure block ----
print("\n=== CONTROL drift (insecure_control vs initial insecure), latency mean & throughput ===")
print(f"  {'lvl':>4} | {'init lat':>9} | {'ctrl lat':>9} | {'lat drift %':>11} | {'init tput':>10} | {'ctrl tput':>10} | {'tput drift %':>12}")
for l in levels:
    a = agg.get(("insecure", l)); c = agg.get(("insecure_control", l))
    if not a or not c: continue
    al, cl = a["latenta_medie_ok_mean"], c["latenta_medie_ok_mean"]
    at, ct = a["throughput_publicare_mean"], c["throughput_publicare_mean"]
    ld = ((cl - al) / al * 100) if al else 0.0
    td = ((ct - at) / at * 100) if at else 0.0
    print(f"  {l:>4} | {al:>9.2f} | {cl:>9.2f} | {ld:>10.1f}% | {at:>10.1f} | {ct:>10.1f} | {td:>11.1f}%")
