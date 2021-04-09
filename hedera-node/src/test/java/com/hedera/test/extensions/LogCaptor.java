package com.hedera.test.extensions;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.CharArrayWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LogCaptor {
	private static final Pattern EVENT_PATTERN = Pattern.compile("(DEBUG|INFO|WARN|ERROR|$)");

	private static final String MINIMAL_PATTERN = "%-5level %msg";

	private final Logger logger;
	private final Appender appender;
	private final CharArrayWriter capture = new CharArrayWriter();

	public LogCaptor(org.apache.logging.log4j.Logger logger) {
		this.logger = (Logger) logger;

		appender = WriterAppender.newBuilder()
				.setTarget(capture)
				.setLayout(PatternLayout.newBuilder().withPattern(MINIMAL_PATTERN).build())
				.setName("LogCaptor")
				.build();

		appender.start();
		this.logger.addAppender(appender);
		this.logger.setLevel(Level.DEBUG);
	}

	public void stopCapture() {
		this.logger.removeAppender(appender);
	}

	public List<String> debugLogs() {
		return eventsAt("DEBUG");
	}

	public List<String> infoLogs() {
		return eventsAt("INFO");
	}

	public List<String> warnLogs() {
		return eventsAt("WARN");
	}

	public List<String> errorLogs() {
		return eventsAt("ERROR");
	}

	private List<String> eventsAt(String level) {
		var output = capture.toString();

		List<String> events = new ArrayList<>();
		var m = EVENT_PATTERN.matcher(output);
		String matchLevel = null;
		for (int i = -1; m.find(); ) {
			if (i != -1 && level.equals(matchLevel)) {
				events.add(output.substring(i, m.start()).trim());
			}
			i = m.end();
			matchLevel = m.group(0);
		}
		return events;
	}
}
