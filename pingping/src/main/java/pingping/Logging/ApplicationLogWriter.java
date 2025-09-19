package pingping.Logging;

import java.util.ArrayList;
import java.util.Map;

import org.tinylog.Logger;
import org.tinylog.core.LogEntry;
import org.tinylog.writers.AbstractFormatPatternWriter;


public class ApplicationLogWriter extends AbstractFormatPatternWriter {
    private static ArrayList<ApplicationLogEventListener> eventListeners = new ArrayList<ApplicationLogEventListener>();

    public static ApplicationLogEventListener registerLogEventListener(ApplicationLogEventListener listener) {
        eventListeners.add(listener);
        Logger.debug("Registered ApplicationLogWriterEvent listener");
        return listener;
    }

    public static void deregisterLogEventListener(ApplicationLogEventListener listener) {
        eventListeners.remove(listener);
        Logger.debug("Deregistered ApplicationLogWriterEvent listener");
    }

    public ApplicationLogWriter(Map<String, String> properties) {
        super(properties);
    }

    @Override
    public void write(LogEntry logEntry) {
        ApplicationLogEvent event = new ApplicationLogEvent(this, logEntry, render(logEntry));
        eventListeners.forEach(listener -> listener.onLogEvent(event));
    }

    @Override
    public void flush() {}

    @Override
    public void close() {}
}
