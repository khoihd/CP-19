#!/bin/bash

# for timeout in 10 20 30 40 50 60 70 80 90 100
for timeout in 20 40 60 80 100
do
  for rate in 0.0 0.2 0.4 0.6 0.8 1.0
  do
    pushd random-network/timeout=$timeout/rate=$rate/d10/
    unzip '*.zip'
    # for id in {0..9}
    # do
      # rm -rf $id
    # done
    popd
  done
done
