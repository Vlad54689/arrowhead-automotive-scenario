#!/bin/bash

# Arrowhead Core Database Initialization Script
#
# This is the ONE AND ONLY init script the MySQL entrypoint runs at the top level of
# /docker-entrypoint-initdb.d. The schema and privilege SQL files are mounted in subdirectories
# (schema/, privileges/) so the entrypoint does NOT execute them as standalone *.sql scripts
# (out of order, with unresolved bare `source` paths).
#
# create_empty_arrowhead_db.sql is the real orchestrator: it DROP+CREATEs the `arrowhead`
# database, USEs it, then `source`s create_arrowhead_tables.sql and the per-service
# *_privileges.sql files by BARE filename. Those bare paths only resolve when the mysql client
# runs from a directory that contains the files, so we stage everything into a single working
# directory and run the orchestrator from there.
#
# We stage into /tmp because the bind-mounted /docker-entrypoint-initdb.d is NOT writable by the
# `mysql` runtime user (copying into it fails with "Permission denied").
#
# No --force: initialization must FAIL LOUDLY (set -e + a non-zero exit aborts container startup)
# if any script errors, rather than silently leaving a half-initialized database. All 19 sourced
# privilege files are present (the 3 optional ones - timemanager/gams/mscv - ship as stubs), so a
# clean run produces no errors.

set -e

WORK_DIR=/tmp/arrowhead-sql-init
mkdir -p "$WORK_DIR"

# Stage the schema + privilege scripts (mounted under initdb.d subdirs) into the writable
# working dir so the bare `source ...` lines inside create_empty_arrowhead_db.sql resolve.
cp /docker-entrypoint-initdb.d/schema/*.sql "$WORK_DIR/"
cp /docker-entrypoint-initdb.d/privileges/*.sql "$WORK_DIR/"

cd "$WORK_DIR"

# Run the orchestrator from the working directory.
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" < create_empty_arrowhead_db.sql
