#!/bin/sh
set -e
pushd scct && sbt clean update publish-local publish
popd
pushd sbt-scct && sbt clean update publish-local publish
popd

SCALA_VERSION='scala_2.9.1'
SCCT_ARTIFACT='scct_2.9.1-0.1-SNAPSHOT'
SBT_SCALA_VERSION='scala_2.7.7'
SBT_SCCT_ARTIFACT='scct-sbt-for-2.9.1-0.1-SNAPSHOT'
mvn install:install-file -Dfile=scct/target/${SCALA_VERSION}/${SCCT_ARTIFACT}.jar -DpomFile=scct/target/${SCALA_VERSION}/${SCCT_ARTIFACT}.pom -DcreateChecksum=true -DlocalRepositoryPath=../gh-pages/maven-repo
mvn install:install-file -Dfile=sbt-scct/target/${SBT_SCALA_VERSION}/${SBT_SCCT_ARTIFACT}.jar -DpomFile=sbt-scct/target/${SBT_SCALA_VERSION}/${SBT_SCCT_ARTIFACT}.pom -DcreateChecksum=true -DlocalRepositoryPath=../gh-pages/maven-repo
