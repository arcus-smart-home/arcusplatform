#!/bin/bash

while read c; do
   ./md.sh $c
done < $1

