import io.undertow.Undertow;
import redis.clients.jedis.JedisPool;

public class DebounceServer {

	private static JedisPool jedisPool;

	public static void main(final String[] args) {
		jedisPool = new JedisPool("localhost", 6379);
		buildServer().start();
	}

	private static Undertow buildServer() {
		return Undertow
				.builder()
				.addHttpListener(8080, "localhost")
				.setHandler(new DebounceRequestHandler(jedisPool))
				.build();
	}

}
