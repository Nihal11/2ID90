#!/bin/sh
# Start this script to build/test the application.
# Usage:
# ./run.sh clean  # to clean the build if wanted.
# ./run.sh        # to build and run the application.

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk/
if [ ! -d "$JAVA_HOME" ] ; then
    # Specific hack for Dennis' setup.
    export JAVA_HOME=/home/dennis/jdk1.8.0_40/
fi

cd "$(dirname "$(realpath "$0")" )/spellchecker"

if [ "$1" = "clean" ] ; then
    ant clean
    exit
fi

# Separate compile and run to avoid useless output
if OUTPUT=$(ant compile -s) ; then
    export NO_PEACH=1

    # Success! Now run it.
    "$JAVA_HOME/bin/java" -cp build/classes/ SpellChecker
else
    # Something terrible happened.
    echo "$OUTPUT"
    # If you're only interested in the compile result:
    # awk '/^-post-compile/{show=0};show;/^-do-compile/{show=1}'
    # echo -e "\033[0m" # ant outputs colors. Reset the color.
fi
