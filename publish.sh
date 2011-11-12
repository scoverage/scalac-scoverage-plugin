#!/bin/sh

set -e
pushd scct && sbt clean update publish-local publish
popd
pushd sbt-scct && sbt clean update publish-local publish
popd

