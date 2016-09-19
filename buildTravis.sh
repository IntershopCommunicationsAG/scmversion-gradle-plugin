#!/bin/bash
# This script will build the project.

export JAVA_OPTS="-Xms96m -Xmx128m -XX:MaxPermSize=64m -XX:+CMSClassUnloadingEnabled -XX:ReservedCodeCacheSize=64M"
export GRADLE_OPTS="-Dorg.gradle.daemon=true"

if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
    if [ "$TRAVIS_TAG" == "" ]; then
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] and without Tag '
        ./gradlew test build -s
    else
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] and Tag ['$TRAVIS_TAG']'
        ./gradlew test build :bintrayUpload :publishPlugins -s
    fi
else
    if [ "$TRAVIS_TAG" == "" ]; then
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] and without Tag'
        ./gradlew test build -s
    else
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] and Tag ['$TRAVIS_TAG']'
        ./gradlew test build :bintrayUpload :publishPlugins -s
    fi
fi