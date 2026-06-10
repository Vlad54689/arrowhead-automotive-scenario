#!/bin/bash

# Arrowhead Core Database Initialization Script

# Create privileges directory
mkdir -p /docker-entrypoint-initdb.d/privileges

# Move privilege scripts
for script in /docker-entrypoint-initdb.d/privileges/*.sql; do
    cp "$script" /docker-entrypoint-initdb.d/
done
