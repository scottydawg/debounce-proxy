#!/usr/bin/env bash

# Assumes Apache Bench is available.
ab -n 10000 -c 5 http://127.0.0.1:8080/
