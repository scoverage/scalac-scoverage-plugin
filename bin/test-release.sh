#!/usr/bin/env bash
set -eux

version=$1

coursier fetch \
  org.scoverage:scalac-scoverage-plugin_2.11.12:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.8:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.9:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.10:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.11:$version \
  org.scoverage:scalac-scoverage-plugin_2.12.12:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.0:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.1:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.2:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.3:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.4:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.5:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.5:$version \
  org.scoverage:scalac-scoverage-plugin_2.13.6:$version \
  org.scoverage:scalac-scoverage-runtime_2.11:$version \
  org.scoverage:scalac-scoverage-runtime_2.12:$version \
  org.scoverage:scalac-scoverage-runtime_2.13:$version \
  org.scoverage:scalac-scoverage-runtime_sjs1_2.11:$version \
  org.scoverage:scalac-scoverage-runtime_sjs1_2.12:$version \
  org.scoverage:scalac-scoverage-runtime_sjs1_2.13:$version \
