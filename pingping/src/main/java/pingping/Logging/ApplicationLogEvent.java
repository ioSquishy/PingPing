package pingping.Logging;

import java.util.EventObject;

import org.tinylog.core.LogEntry;

public class ApplicationLogEvent extends EventObject {
    private final LogEntry logEntry;
    private final String renderedEntry;

    public ApplicationLogEvent(Object source, LogEntry logEntry, String renderedEntry) {
        super(source);
        this.logEntry = logEntry;
        this.renderedEntry = renderedEntry;
    }

    public LogEntry getLogEntry() {
        return logEntry;
    }

    public String getRenderedEntry() {
        return renderedEntry;
    }
    
}
