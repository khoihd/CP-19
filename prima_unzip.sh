#!/bin/bash
# for algorithm in "RDIFF"
for algorithm in "RC-DIFF" "RDIFF"
do
for agent in 10
do
  # topology could be "random-network" or "scale-free-tree"
  for topology in "random-network" "scale-free-tree"
  # for topology in "scale-free-tree"
  do
    for instances in {0..9}
    do
    # cd $algorithm"/scenario/"$topology"/d"$agent"/"$instances"/output"
    directory=$PWD
    cd $algorithm"/scenario/"$topology"/d"$agent"/"$instances
    # zip map.zip map.log
    pwd
    unzip_files="unzip -q "$algorithm"_output.zip"
    $unzip_files
    # rm -rf output
    cd $directory
    # cd ../../../../../
    done
  done
done
done
