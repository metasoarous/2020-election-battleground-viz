#!/bin/bash

for (( ; ; ))
do
  curl https://alex.github.io/nyt-2020-election-scraper/battleground-state-changes.csv > public/battleground-state-changes.csv
  surge public 2020-election-results.surge.sh
  sleep 300
done

