#!/bin/bash

sed s/CAPNAME/$1/g template.driver | sed s/CAPNS/$2/g > MOCK_$1_1_1.driver

