#!/usr/bin/env python3
"""
aggregate-foodchain.py - offline aggregation of the food-chain battery raw CSV.

Reads rezultate-foodchain-brut.csv (one row per measurement:
  mod,rep,nivel,cereri_trimise,cereri_reusite_post,verdicte_ajunse,rata_livrare_e2e,
  throughput_post_rps,lat_cumulata_medie_ms,p50_ms,p95_ms,p99_ms,orient_hop1_ms,orient_hop2_ms)
and writes rezultate-foodchain-agregat.csv with mean +/- sample sd per (mod, nivel) over reps.

Prints:
  * the aggregate table (cumulative end-to-end latency, e2e delivery rate, throughput, per-hop),
  * security overhead (secure - insecure) per level on cumulative latency and throughput,
  * a drift probe: per-rep insecure cumulative-latency spread (rep1 vs last, CoV).

Pure stdlib. Per-hop columns are ORIENTATIVE (see load-test-foodchain.sh).
"""
import csv, statistics, sys
from collections import defaultdict

RAW = sys.argv[1] if len(sys.argv) > 1 else "rezultate-foodchain-brut.csv"
OUT = sys.argv[2] if len(sys.argv) > 2 else "rezultate-foodchain-agregat.csv"

METRICS = ["rata_livrare_e2e", "throughput_post_rps", "lat_cumulata_medie_ms",
           "p50_ms", "p95_ms", "p99_ms", "orient_hop1_ms", "orient_hop2_ms"]

rows = []
with open(RAW) as f:
    for r in csv.DictReader(f):
        r["nivel"] = int(r["nivel"]); r["rep"] = int(r["rep"])
        for m in METRICS:
            r[m] = float(r[m])
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
    return f"{e[key+'_mean']:.2f}±{e[key+'_sd']:.2f}" if e else "  -  "

levels = sorted({l for (_, l) in agg})
print("=== AGGREGATE food-chain (mean ± sd over reps) ===")
for mod in ("insecure", "secure"):
    if not any(m == mod for (m, _) in agg): continue
    print(f"\n[{mod}]")
    print(f"  {'lvl':>4} | {'deliv_e2e':>11} | {'tput/s':>13} | {'lat_cumul ms':>15} | {'p95 ms':>13} | {'p99 ms':>13} | {'oH1*':>9} | {'oH2*':>9}")
    for l in levels:
        if (mod, l) not in agg: continue
        print(f"  {l:>4} | {fmt(mod,l,'rata_livrare_e2e'):>11} | {fmt(mod,l,'throughput_post_rps'):>13} | "
              f"{fmt(mod,l,'lat_cumulata_medie_ms'):>15} | {fmt(mod,l,'p95_ms'):>13} | {fmt(mod,l,'p99_ms'):>13} | "
              f"{agg[(mod,l)]['orient_hop1_ms_mean']:>9.1f} | {agg[(mod,l)]['orient_hop2_ms_mean']:>9.1f}")
print("\n  (* oH1/oH2 = orientative per-hop: client->RiskScoring receive / RiskScoring->Trust receive)")

print("\n=== SECURITY OVERHEAD on the chain (secure - insecure), cumulative latency ===")
print(f"  {'lvl':>4} | {'insec ms':>10} | {'sec ms':>10} | {'abs ms':>9} | {'overhead %':>10} | {'tput insec':>11} | {'tput sec':>10} | {'tput drop %':>11}")
for l in levels:
    a = agg.get(("insecure", l)); b = agg.get(("secure", l))
    if not a or not b: continue
    ai, bi = a["lat_cumulata_medie_ms_mean"], b["lat_cumulata_medie_ms_mean"]
    at, bt = a["throughput_post_rps_mean"], b["throughput_post_rps_mean"]
    absd = bi - ai; pct = (absd/ai*100) if ai else 0.0; tdrop = ((at-bt)/at*100) if at else 0.0
    print(f"  {l:>4} | {ai:>10.2f} | {bi:>10.2f} | {absd:>9.2f} | {pct:>9.1f}% | {at:>11.1f} | {bt:>10.1f} | {tdrop:>10.1f}%")

print("\n=== DRIFT probe: insecure cumulative latency across reps (per level) ===")
print(f"  {'lvl':>4} | {'rep1':>8} | {'repN':>8} | {'min':>8} | {'max':>8} | {'CoV %':>7}")
for l in levels:
    rs = sorted(groups.get(("insecure", l), []), key=lambda r: r["rep"])
    if not rs: continue
    vals = [r["lat_cumulata_medie_ms"] for r in rs]
    cov = (statistics.stdev(vals)/statistics.mean(vals)*100) if len(vals) > 1 and statistics.mean(vals) else 0.0
    print(f"  {l:>4} | {vals[0]:>8.2f} | {vals[-1]:>8.2f} | {min(vals):>8.2f} | {max(vals):>8.2f} | {cov:>6.1f}%")
