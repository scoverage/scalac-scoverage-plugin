#!/usr/bin/env bash
#

set -euo pipefail

cd "${TMPDIR:-/tmp}"
git clone "https://github.com/typesafehub/scala-logging"
( cd scala-logging && sbt -sbt-version 0.13.13-M1 ++2.11.8 publishLocal ++2.12.0-RC1 publishLocal )
rm -rf scala-logging
