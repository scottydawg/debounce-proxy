
# Implementation #

This throttling proxy relies on a centralized cache to record requests to specific upstream paths and prohibits subsequent calls to that path until the record expires.

## Design Choices ##

I assumed an eventual real-world scenario with multiple instances of the proxy server, where the throttling period for a given path spans all instances. This requires a centralized cache for recording requests. In-memory throttling would allow more upstream traffic as instances were added, which I assumed would be undesirable.

## Technology Choices ##

Having used Scala almost exclusively for 18 months, I had to come back up to speed on current options in the Java ecosystem. Minutes-long research yielded the following:

* Server – Spring Boot was the last framework I worked with heavily, but have no special affinity to it. I selected for performance, simplicity, and currency. Undertow rated highly in all of those areas.
* Client – I used the native Java HTTP client for proxying requests. I selected for performance after seeing benchmarks that compared it to the old HttpUrlConnection. (Later I noticed benchmarks against the Apache HTTPClient that made the native client look sloth-like. Remorse ensued.)
* Database – A simple caching database with TTL support was all I needed. Memcached and Redis perform similarly here; I chose Redis for sake of familiarity, given time constraints.
* Logging - Was delighted to find Tinylog, and selected it for simplicity and low overhead.
* Load Testing - I'm assuming Apache Bench (`ab`) is available natively, and using it to flood the service.
* Upstream Server - For testing during development, I configured MockServer to field upstream requests with a 100ms delay.

# Operations #

## Running the Server ##

Bring up the environment (a Redis instance and upstream MockServer):
```
docker-compose -f dcomp-dev.yaml up
```

Run the server:
```
mvn clean compile exec:java -Dexec.mainClass="DebounceServer"
```

> Note: `clean` removes the application.log, which is necessary for proper analysis before each run.

## Testing the Server ##

The proxy server runs on [localhost:8080](http://localhost:8080/), while the upstream runs on [localhost:1080](http://localhost:1080/). Append any URI to the root path to proxy that request upstream.

Simulate a flood of requests:
```
./attack.sh
```

Analyze results:
```
./analyze.sh
```

The total number of requests proxied should equal the "Time taken for tests" from the Bench output divided by the configured throttle duration, rounded down.

## Change the Throttle Duration ##

Hit [http://localhost:8080/_admin/throttle/1000](http://localhost:8080/_admin/throttle/1000) to set the throttle duration to 1000 ms. Pass any numeric value in place of 1000 to alter the duration.

## Stopping the Server ##

Ctrl+C in the running Terminal, and tear down the environment with:
```
docker-compose -f dcomp-dev.yaml down --remove-orphans
```

# Improvements #

* The throttle duration should be loaded from cache. In a multi-instance cluster with the current implementation, the duration will be inconsistent across instances.
* `DebounceAdmin` should probably be a separate HttpHandler that handles the `/_admin` path and modifies the cache.
* Move upstream URL, default throttle duration, proxy timeout duration into config
* Use asynchronous HTTPClient features
* The endpoint to change the throttle duration should really be a POST. I used this GET hack for sake of time.

