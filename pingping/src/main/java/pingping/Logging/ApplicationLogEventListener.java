package pingping.Logging;

import java.util.EventListener;

@FunctionalInterface
public interface ApplicationLogEventListener extends EventListener {
    void onLogEvent(ApplicationLogEvent event);
}
