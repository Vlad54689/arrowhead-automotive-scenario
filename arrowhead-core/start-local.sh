#!/bin/bash
# Start Arrowhead core services locally (without Docker)

BASEDIR=/home/vled/sos-arrowhead-automotive/arrowhead-core
TARGET=$BASEDIR/target
PROPS=$BASEDIR/properties
LOCAL=$BASEDIR/local-run
LOGDIR=$BASEDIR/logs

rm -rf $LOCAL
mkdir -p $LOGDIR

# Create per-service directories with application.properties pointing to localhost
for f in $PROPS/*.properties; do
  bn=$(basename "$f" .properties)
  mkdir -p "$LOCAL/$bn"
  sed 's|jdbc:mysql://arrowhead_core_mysql:3306|jdbc:mysql://127.0.0.1:3306|g' "$f" > "$LOCAL/$bn/application.properties"
done

# Copy JARs and run script to local-run
cp $TARGET/arrowhead-*.jar $LOCAL/
cp $BASEDIR/run.sh $LOCAL/

# Run from local-run directory
cd $LOCAL
bash run.sh > "$LOGDIR/core-combined.out" 2>&1 &
echo "Core services starting in background. PID: $!"
echo "Logs: $LOGDIR/core-combined.out"
echo "Per-service logs will be in $LOCAL/"
