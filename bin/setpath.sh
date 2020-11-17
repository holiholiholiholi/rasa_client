#!/bin/zsh

function addJar(){
  for j in $1/*.jar
do
  if [ -e $j ]
  then CLASSPATH=$CLASSPATH:$j
  fi
done

  for d in $1/*
  do
   if [ -d $d ]
   then
      addJar $d
   fi
  done
}

# do "mvn install -Dmaven.test.skip=true" before run this script

HOMEDIR=`dirname $0`

CLASSPATH=$HOMEDIR/../target/classes

#the classes
#for i in dare-common dare-re dare-transform dare-annotation nlp-tools dare-tools experiment
#do
#   CLASSPATH=$CLASSPATH:$HOMEDIR/../$i/target/classes
#   CLASSPATH=$CLASSPATH:$HOMEDIR/../$i/target/test-classes
#done


#the libraries
addJar $HOMEDIR/../target/lib