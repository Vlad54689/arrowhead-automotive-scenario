#!/usr/bin/env python3
"""
aggregate-foodchain-final.py - methodologically coherent aggregation of the
food-chain battery, aligned with scenario 1's hot/interleaved split.

Rule (justified objectively by warm-up stabilization, see thesis notes):
  * BLOCK levels 1/5/10  -> warm steady-state only: keep reps 3-5 (drop rep1+rep2
    warm-up; rep2 is demonstrably above the rep3-5 band in 5/6 series, massively
    so at level 10: 55ms vs steady ~28ms). Absolute hot latencies come from here.
  * INTERLEAVED levels 20/50 -> keep all 5 reps (drift-free pairing). Absolutes
    are cold-start, so only the secure-vs-insecure PERCENTAGES are reported here.

Reads  rezultate-foodchain-brut.csv  (untouched, all 50 raw rows).
Writes rezultate-foodchain-agregat-final.csv  (mean +/- sample sd, with n_runs
       reflecting the rule: n=3 for 1/5/10, n=5 for 20/50).
Original rezultate-foodchain-agregat.csv is left intact.

Pure stdlib. Per-hop columns are ORIENTATIVE (see load-test-foodchain.sh).
"""
import csv, statistics, sys
from collections import defaultdict

RAW = sys.argv[1] if len(sys.argv) > 1 else "rezultate-foodchain-brut.csv"
OUT = sys.argv[2] if len(sys.argv) > 2 else "rezultate-foodchain-agregat-final.csv"

METRICS = ["rata_livrare_e2e", "throughput_post_rps", "lat_cumulata_medie_ms",
           "p50_ms", "p95_ms", "p99_ms", "orient_hop1_ms", "orient_hop2_ms"]

BLOCK_LEVELS = {1, 5, 10}        # warm regime, keep reps >= 3
INTERLEAVED_LEVELS = {20, 50}    # keep all reps

def keep(row):
    lvl, rep = row["nivel"], row["rep"]
    if lvl in BLOCK_LEVELS:
        return rep >= 3          # rep3-5 steady-state only
    return True                  # 20/50 interleaved: all reps

rows = []
with open(RAW) as f:
    for r in csv.DictReader(f):
        r["nivel"] = int(r["nivel"]); r["rep"] = int(r["rep"])
        for m in METRICS:
            r[m] = float(r[m])
        if keep(r):
            rows.append(r)

groups = defaultdict(list)
for r in rows:
    groups[(r["mod"], r["nivel"])].append(r)

def mean_sd(vals):
    m = statistics.mean(vals)
    sd = statistics.stdev(vals) if len(vals) > 1 else 0.0
    return m, sd

agg = {}
for (mod, lvl), rs in groups.items():
    entry = {"mod": mod, "nivel": lvl, "n_runs": len(rs)}
    for m in METRICS:
        mu, sd = mean_sd([r[m] for r in rs])
        entry[m + "_mean"] = mu; entry[m + "_sd"] = sd
    agg[(mod, lvl)] = entry

fields = ["mod", "nivel", "n_runs"] + [m + s for m in METRICS for s in ("_mean", "_sd")]
with open(OUT, "w", newline="") as f:
    w = csv.DictWriter(f, fieldnames=fields)
    w.writeheader()
    for key in sorted(agg, key=lambda k: (k[0], k[1])):
        w.writerow({k: (f"{v:.3f}" if isinstance(v, float) else v) for k, v in agg[key].items()})
print(f"wrote {OUT}\n")

def fmt(mod, lvl, key):
    e = agg.get((mod, lvl))
    return f"{e[key+'_mean']:.2f}+/-{e[key+'_sd']:.2f}" if e else "  -  "

levels = sorted({l for (_, l) in agg})
print("=== FINAL AGGREGATE food-chain (mean +/- sd) ===")
print("    levels 1/5/10: reps 3-5 (warm)  |  levels 20/50: all reps (interleaved)")
for mod in ("insecure", "secure"):
    print(f"\n[{mod}]")
    print(f"  {'lvl':>4} | {'n':>2} | {'deliv_e2e':>10} | {'tput/s':>13} | {'lat_cumul ms':>15} | {'p95 ms':>13} | {'p99 ms':>13}")
    for l in levels:
        if (mod, l) not in agg: continue
        print(f"  {l:>4} | {agg[(mod,l)]['n_runs']:>2} | {fmt(mod,l,'rata_livrare_e2e'):>10} | {fmt(mod,l,'throughput_post_rps'):>13} | "
              f"{fmt(mod,l,'lat_cumulata_medie_ms'):>15} | {fmt(mod,l,'p95_ms'):>13} | {fmt(mod,l,'p99_ms'):>13}")

print("\n=== SECURITY OVERHEAD (secure vs insecure) ===")
print(f"  {'lvl':>4} | {'regime':>11} | {'insec ms':>9} | {'sec ms':>9} | {'abs ms':>8} | {'overhead %':>10} | {'tput drop %':>11}")
for l in levels:
    a = agg.get(("insecure", l)); b = agg.get(("secure", l))
    if not a or not b: continue
    ai = a["lat_cumulata_medie_ms_mean"]; bi = b["lat_cumulata_medie_ms_mean"]
    at = a["throughput_post_rps_mean"]; bt = b["throughput_post_rps_mean"]
    regime = "warm" if l in BLOCK_LEVELS else "interleaved"
    pct = (bi - ai) / ai * 100 if ai else 0.0
    tdrop = (at - bt) / at * 100 if at else 0.0
    note = "" if l in BLOCK_LEVELS else "  (% only; abs cold)"
    print(f"  {l:>4} | {regime:>11} | {ai:>9.2f} | {bi:>9.2f} | {bi-ai:>8.2f} | {pct:>9.1f}% | {tdrop:>10.1f}%{note}")
