#!/usr/bin/env python3
"""
aggregate-battery-rep3-5.py - LOCAL recomputation of Scenario 1 (automotive,
single-hop) block battery using the warm steady-state reps 3-5 only, to make
its data treatment IDENTICAL to Scenario 2 (food-chain) for comparability.

Same objective warm-up-stabilization criterion as Scenario 2: rep1 is a
universal warm-up outlier (+47%..+144% over the rep3-5 band across all 10
series) and rep2 is still above the band in 8/10 series, so the steady regime
begins at rep3. Reps 3-5 (n=3) are kept.

Reads  rezultate-baterie-brut.csv   (block battery, untouched)
Writes rezultate-baterie-agregat-rep3-5.csv  (mean +/- sample sd over reps 3-5)
The originally published rezultate-baterie-agregat.csv is left intact; this is a
side-by-side recomputation only. The mtls-experiment branch is NOT touched.

Only mod in {insecure, secure} is aggregated (insecure_control excluded).
Pure stdlib.
"""
import csv, statistics, sys
from collections import defaultdict

RAW = sys.argv[1] if len(sys.argv) > 1 else "rezultate-baterie-brut.csv"
OUT = sys.argv[2] if len(sys.argv) > 2 else "rezultate-baterie-agregat-rep3-5.csv"

METRICS = ["rata_livrare", "throughput_publicare", "latenta_medie_ok",
           "p50_ok", "p95_ok", "p99_ok"]

rows = []
with open(RAW) as f:
    for r in csv.DictReader(f):
        if r["mod"] not in ("insecure", "secure"):
            continue
        r["nivel"] = int(r["nivel"]); r["rep"] = int(r["rep"])
        if r["rep"] < 3:                 # drop rep1+rep2 warm-up
            continue
        for m in METRICS:
            r[m] = float(r[m])
        rows.append(r)

groups = defaultdict(list)
for r in rows:
    groups[(r["mod"], r["nivel"])].append(r)

def mean_sd(vals):
    m = statistics.mean(vals)
    return m, (statistics.stdev(vals) if len(vals) > 1 else 0.0)

agg = {}
for (mod, lvl), rs in groups.items():
    e = {"mod": mod, "nivel": lvl, "n_runs": len(rs)}
    for m in METRICS:
        mu, sd = mean_sd([r[m] for r in rs])
        e[m + "_mean"] = mu; e[m + "_sd"] = sd
    agg[(mod, lvl)] = e

fields = ["mod", "nivel", "n_runs"] + [m + s for m in METRICS for s in ("_mean", "_sd")]
with open(OUT, "w", newline="") as f:
    w = csv.DictWriter(f, fieldnames=fields)
    w.writeheader()
    for key in sorted(agg, key=lambda k: (k[0], k[1])):
        w.writerow({k: (f"{v:.3f}" if isinstance(v, float) else v) for k, v in agg[key].items()})
print(f"wrote {OUT}\n")

levels = sorted({l for (_, l) in agg})
print("=== SCEN.1 RECOMPUTED (reps 3-5, warm) absolute latency ===")
for mod in ("insecure", "secure"):
    print(f"\n[{mod}]")
    print(f"  {'lvl':>4} | {'n':>2} | {'tput/s':>13} | {'lat ms':>13} | {'p95 ms':>13} | {'p99 ms':>13}")
    for l in levels:
        e = agg[(mod, l)]
        print(f"  {l:>4} | {e['n_runs']:>2} | {e['throughput_publicare_mean']:>7.1f}+/-{e['throughput_publicare_sd']:<5.1f} | "
              f"{e['latenta_medie_ok_mean']:>6.2f}+/-{e['latenta_medie_ok_sd']:<5.2f} | "
              f"{e['p95_ok_mean']:>6.2f}+/-{e['p95_ok_sd']:<5.2f} | {e['p99_ok_mean']:>6.2f}+/-{e['p99_ok_sd']:<5.2f}")

print("\n=== SCEN.1 SECURITY OVERHEAD (reps 3-5) ===")
print(f"  {'lvl':>4} | {'insec ms':>9} | {'sec ms':>9} | {'abs ms':>8} | {'overhead %':>10} | {'tput drop %':>11}")
for l in levels:
    a = agg[("insecure", l)]; b = agg[("secure", l)]
    ai, bi = a["latenta_medie_ok_mean"], b["latenta_medie_ok_mean"]
    at, bt = a["throughput_publicare_mean"], b["throughput_publicare_mean"]
    print(f"  {l:>4} | {ai:>9.2f} | {bi:>9.2f} | {bi-ai:>8.2f} | {(bi-ai)/ai*100:>9.1f}% | {(at-bt)/at*100:>10.1f}%")
