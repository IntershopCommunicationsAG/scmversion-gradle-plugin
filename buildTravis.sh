#!/bin/bash
# This script will build the project.

if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
    if [ "$TRAVIS_TAG" == "" ]; then
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']'
        ./gradlew test build -i -s
    else
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] and without Tag'
        ./gradlew test build publishPlugins -i -s
    fi
else
    if [ "$TRAVIS_TAG" == "" ]; then
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']'
        ./gradlew test build -i -s
    else
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] and without Tag'
        ./gradlew test build publishPlugins -i -s
    fi
fi