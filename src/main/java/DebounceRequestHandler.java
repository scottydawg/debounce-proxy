import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.tinylog.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;

public class DebounceRequestHandler implements HttpHandler {

	private static final String OK = "OK";
	private static final String UPSTREAM = "http://localhost:1080";
	private static final String ADMIN_RESPONSE = "{\"throttlePeriod\": %d}";

	private final JedisPool pool;
	private final DebounceAdmin admin;
	private final HttpClient client = HttpClient.newBuilder().build();

	public DebounceRequestHandler(JedisPool jedisPool) {
		this.pool = jedisPool;
		this.admin = new DebounceAdmin();
	}

	@Override
	public void handleRequest(final HttpServerExchange exchange) throws Exception {
		String relPath = exchange.getRelativePath().toLowerCase();

		if (admin.maybeUpdateThrottlePeriod(relPath)) {
			sendJson(String.format(ADMIN_RESPONSE, admin.getThrottlePeriodInMs()), exchange);
		} else if (isAllowed(relPath)) {
			proxyRequest(relPath, exchange);
		} else {
			throttle(relPath, exchange);
		}
	}

	private boolean isAllowed(String path) {
		try(Jedis jedis = pool.getResource()) {
			String res = jedis.set(path, "A", SetParams.setParams().px(admin.getThrottlePeriodInMs()).nx());
			return OK.equals(res);
		} catch (Error e) {
			return false;
		}
	}

	private void proxyRequest(String path, HttpServerExchange exchange)
			throws URISyntaxException, IOException, InterruptedException {
		Logger.info("Proxying request to " + path);
		HttpRequest req = HttpRequest.newBuilder()
				.uri(new URI(UPSTREAM + path))
				.timeout(Duration.of(10, SECONDS))
				.GET()
				.build();

		HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
		sendJson(res.body(), exchange);
	}

	private void sendJson(String body, HttpServerExchange exchange) {
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		exchange.getResponseSender().send(body);
	}

	private void throttle(String path, HttpServerExchange exchange) {
		Logger.info("Throttling request to " + path);
		exchange.setStatusCode(StatusCodes.TOO_MANY_REQUESTS);
		exchange.endExchange();
	}

}
