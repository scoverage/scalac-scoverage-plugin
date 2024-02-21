#!/usr/bin/env bash
set -eux

version=$1

coursier fetch \
  org.scoverage:scalac-scoverage-plugin_2.12.10:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.11:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.12:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.13:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.14:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.15:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.16:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.17:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.18:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.19:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.5:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.5:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.6:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.7:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.8:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.9:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.10:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.11:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.12:$version \
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
