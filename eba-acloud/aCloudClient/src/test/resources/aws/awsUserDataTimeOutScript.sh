#!/bin/bash
COUNTER=1
while :
do
    echo "Timeout waiting $COUNTER"
    COUNTER=$[$COUNTER +1]
    sleep 1
done