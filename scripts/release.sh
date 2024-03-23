#!/bin/bash
##
#

VERSION="$1"
SNAPSHOT="$2" 

usage()
{
   echo "usage: <RELEASE_VERSION> <NEW_SNAPSHOT>"
}

fatal()
{
   echo "FATAL:""$@"
   exit -1
}

doMvn() {
 echo "MVN:$@"   
 mvn "$@"
 ret="$?"
 if [ "${ret}" == "0" ] ; then 
   return;
 else
    fatal "mvn $@ failed with error:${ret}" 
 fi
}

doGit() { 
 echo "GIT:$@"   
 git "$@"
 ret="$?"
 if [ "${ret}" == "0" ] ; then 
   return;
 else
    fatal "git $@ failed with error:${ret}" 
 fi
}

if [ -z "${VERSION}" ] ; then 
    usage
    exit 1 
fi

if [ -z "${SNAPSHOT}" ] ; then 
    usage
    exit 1 
fi

if [[ "${SNAPSHOT}" != *-SNAPSHOT ]] ; then 
    echo "SNAPSHOT version must end with '-SNAPSHOT'! but is:'"${SNAPSHOT}"'"
    exit 1
fi

# Update/build/verify to VERSION
doGit checkout develop
doMvn versions:set -DnewVersion=${VERSION}
doMvn clean verify
doGit add pom.xml **/pom.xml
doGit commit -m "New release version=${VERSION}"

# Create/merge/deploy master to VERSION
doGit checkout master
doGit merge develop
doGit tag "v${VERSION}"
doGit push origin master
doGit push origin "v${VERSION}"
doMvn clean package verify deploy

# Merge back/update develop to SNAPSHOT
doGit checkout develop
doMvn versions:set -DnewVersion=${SNAPSHOT}
doGit add pom.xml **/pom.xml
doGit commit -m "New develop version=${SNAPSHOT}"
doGit push origin

