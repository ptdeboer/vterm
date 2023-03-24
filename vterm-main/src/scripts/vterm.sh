#!/bin/bash
##
# (C) Piter.NL
# Start VTerm main
#

startJava() {
   echo "java" "$@"
   java "$@"
}

CLASS=nl.piter.vterm.VTermMain

DIRNAME=`dirname "$0"`
BASE_DIR=`cd "$DIRNAME/.." ; pwd`
echo "Using BASE_DIR: ${BASE_DIR}"

CLASSPATH="${BASE_DIR}:${BASE_DIR}/etc:${BASE_DIR}/lib:${BASE_DIR}/lib/*:${BASE_DIR}/bin"

startJava -cp "${CLASSPATH}" ${CLASS} "$@"
