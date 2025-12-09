#!/usr/bin/env bash
set -eux

version=$1

coursier fetch \
  org.scoverage:scalac-scoverage-plugin_2.12.18:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.19:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.20:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.21:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.11:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.12:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.13:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.14:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.15:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.16:$version \
  org.scoverage:scalac-scoverage-runtime_2.12:$version \
  org.scoverage:scalac-scoverage-runtime_2.13:$version \
  org.scoverage:scalac-scoverage-runtime_sjs1_2.12:$version \
  org.scoverage:scalac-scoverage-runtime_sjs1_2.13:$version \
  org.scoverage:scalac-scoverage-runtime_native0.4_2.12:$version \
  org.scoverage:scalac-scoverage-runtime_native0.4_2.13:$version \
  org.scoverage:scalac-scoverage-domain_2.12:$version \
  org.scoverage:scalac-scoverage-domain_2.13:$version \
  org.scoverage:scalac-scoverage-domain_3:$version \
  org.scoverage:scalac-scoverage-reporter_2.12:$version \
  org.scoverage:scalac-scoverage-reporter_2.13:$version \
  org.scoverage:scalac-scoverage-reporter_3:$version \
  org.scoverage:scalac-scoverage-serializer_2.12:$version \
  org.scoverage:scalac-scoverage-serializer_2.13:$version \
  org.scoverage:scalac-scoverage-serializer_3:$version \
