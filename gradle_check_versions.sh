#!/usr/bin/env bash

versions=("4.4" "4.5" "4.6" "4.7" "4.8" "4.9" "4.10" "5.0" "5.1" "5.2" "5.3" "5.4" "5.5" "5.6.4" "6.0" "6.1" "6.2" "6.3" "6.4")

rm -fr build/reports
for i in "${versions[@]}"
do
    echo "test gradle version $i"
    GRADLE_VERSION=$i ./gradlew test &> /dev/null
    status=$?
    mkdir -p "build/reports/$i"
    mv build/reports/test "build/reports/$i"
    if [ $status -ne 0 ]; then
        echo "test error $i"
    fi

    GRADLE_VERSION=$i ./gradlew integrationTest &> /dev/null
    status=$?
    mkdir -p "build/reports/$i"
    mv build/reports/integrationTest "build/reports/$i"
    if [ $status -ne 0 ]; then
        echo "integrationTest error $i"
    fi
done
