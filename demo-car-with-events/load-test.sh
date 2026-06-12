#!/usr/bin/env bash
URL="http://localhost:9895/quality-inspections"
BODY='{"carId":1,"brand":"Load","color":"X","defectDetected":true,"defectType":"WEAR","inspectionResult":"DEFECT_DETECTED","inspectionTimestamp":0}'
REQUESTS=200
LEVELS=(1 5 10 20 50)
MR_LOG="/tmp/maintenance.log"

one_req() {
  curl -s -o /dev/null -w "%{time_total} %{http_code}\n" -X POST "$URL" \
    -H 'Content-Type: application/json' -d "$BODY"
}
export -f one_req
export URL BODY

echo "nivel,total,reusite,esuate,durata_s,throughput_rps,medie_ms,p50_ms,p95_ms,p99_ms" > rezultate.csv

for C in "${LEVELS[@]}"; do
  echo "=== Concurenta: $C fire, $REQUESTS cereri ==="
  : > /tmp/lt_times.txt
  start=$(date +%s.%N)
  seq "$REQUESTS" | xargs -P "$C" -I{} bash -c 'one_req' >> /tmp/lt_times.txt
  end=$(date +%s.%N)

  python3 - "$C" "$REQUESTS" "$start" "$end" <<'PY'
import sys, math
C=int(sys.argv[1]); N=int(sys.argv[2]); start=float(sys.argv[3]); end=float(sys.argv[4])
times=[]; ok=0; fail=0
for line in open('/tmp/lt_times.txt'):
    line=line.strip()
    if not line: continue
    t,code=line.split()
    t=float(t)*1000.0
    times.append(t)
    if code.startswith('2'): ok+=1
    else: fail+=1
times.sort()
def pct(p):
    if not times: return 0
    k=(len(times)-1)*p; f=math.floor(k); c=math.ceil(k)
    return times[int(k)] if f==c else times[f]+(times[c]-times[f])*(k-f)
dur=end-start
tput=N/dur if dur>0 else 0
mean=sum(times)/len(times) if times else 0
row=f"{C},{N},{ok},{fail},{dur:.2f},{tput:.1f},{mean:.2f},{pct(.5):.2f},{pct(.95):.2f},{pct(.99):.2f}"
print(f"  reusite={ok} esuate={fail} durata={dur:.2f}s throughput={tput:.1f} req/s")
print(f"  medie={mean:.2f}ms p50={pct(.5):.2f}ms p95={pct(.95):.2f}ms p99={pct(.99):.2f}ms")
open('rezultate.csv','a').write(row+"\n")
PY
  sleep 2
done

echo ""
echo "=== Evenimente livrate la Maintenance (total) ==="
grep -c "Received event" "$MR_LOG"
echo ""
echo "=== Rezultate (rezultate.csv) ==="
cat rezultate.csv
