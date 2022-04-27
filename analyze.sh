#!/usr/bin/env bash

total_reqs=$(grep -i 'DebounceRequestHandler' application.log | wc -l)
proxied_reqs=$(grep -i 'Proxying' application.log | wc -l)
throttled_reqs=$(grep -i 'Throttling' application.log | wc -l)

echo "$total_reqs total requests"
echo "$proxied_reqs proxied, $throttled_reqs throttled"
