#!/bin/bash
algorithm="RDIFF"
for agent in 15
do
  # topology could be "random-network" or "scale-free-tree"
  # for topology in "random-network"
  for topology in "scale-free-tree"
  do
    for instances in {0..19}
    do
    cd $algorithm"/scenario/"$topology"/d"$agent"/"$instances"/output"
    # zip map.zip map.log
    unzip map.zip
    cd ../../../../../../
    done
  done
done
