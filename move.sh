#!/bin/bash
timeout=$1

for rate in '0.0' '0.1' '0.2' '0.3' '0.4' '0.5' '0.6' '0.7' '0.8' '0.9' '1.0'
do
  for id in {10..19}
  do
    # rm -rf timeout=$timeout/rate=$rate/random-network/d10/*$id
    # mv rate=$rate/random-network/$id timeout=$timeout/rate=$rate/random-network/d10/
    echo 1
  done
  rm -rf rate=$rate/*
done
