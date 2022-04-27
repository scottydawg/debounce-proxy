import org.tinylog.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DebounceAdmin {

	private long throttlePeriodInMs = 500;
	private final Pattern throttlePattern = Pattern.compile("/_admin/throttle/(\\d+)", Pattern.CASE_INSENSITIVE);

	public long getThrottlePeriodInMs() {
		return this.throttlePeriodInMs;
	}

	public boolean maybeUpdateThrottlePeriod(String path) {
		Matcher throttlePath = throttlePattern.matcher(path);

		if (throttlePath.matches()) {
			long duration = Long.decode(throttlePath.group(1));
			throttlePeriodInMs = duration;
			Logger.info("New throttle period duration: " + duration);
			return true;
		} else {
			return false;
		}
	}

}

