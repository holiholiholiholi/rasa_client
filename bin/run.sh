#!/bin/zsh

. `dirname $0`/setpath.sh

java -Xmx4096M -cp $CLASSPATH  $@