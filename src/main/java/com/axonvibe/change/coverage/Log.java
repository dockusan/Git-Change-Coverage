package com.axonvibe.change.coverage;

import java.io.PrintStream;

public class Log {
	
	static PrintStream logger = null;
	
	static void log(String message) {
		if (logger != null) {
			logger.println(message);
		}
	}
}
