#!/bin/sh
# Start this script to build/test the application.
# Usage:
# ./run.sh clean  # to clean the build if wanted.
# ./run.sh        # to build and run the application.

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk/
cd "$(dirname "$(realpath "$0")" )"

if [ "$1" == "clean" ] ; then
    (cd AICompetition && ant clean)
    (cd 2ID90-Group-11 && ant clean)
    exit
fi

# 2ID90-Group-11 depends on AICompetition to compile, so compile it first.
# If that succeeds, start the GUI via "ant runn" in AICompetition
(cd AICompetition && ant jar) && \
(cd 2ID90-Group-11 && ant jar) && \
(cd AICompetition && ant run -Dapplication.args="../2ID90-Group-11/dist dist")

