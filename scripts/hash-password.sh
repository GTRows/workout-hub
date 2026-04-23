#!/usr/bin/env bash
# Generate a BCrypt hash suitable for the APP_ADMIN_PASSWORD_HASH env var.
# Usage:
#   ./scripts/hash-password.sh 'your-strong-plaintext'
# The output is a single line BCrypt hash (starts with $2y$10$...).
# Spring Security's BCryptPasswordEncoder accepts $2a$, $2b$, and $2y$
# variants, so the output is paste-ready.
#
# Requires: Docker (pulls httpd:alpine on first run for htpasswd).
set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <plaintext>" >&2
    exit 2
fi

# htpasswd -nbBC 10 '' "$1" emits "<user>:<hash>" -- we strip the leading ':'
# and any trailing newline, then print a single clean line.
docker run --rm httpd:alpine \
    sh -c "htpasswd -nbBC 10 '' \"$1\" | cut -d: -f2 | tr -d '\n'"
echo
