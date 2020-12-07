package com.tomtom.james.tomcat;

import com.tomtom.james.newagent.JVMAgentCleaner;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Helper class to clean up James resources after context in Tomcat is stopped
 */
public class JamesCleanupListener implements LifecycleListener {

    private static final Log log = LogFactory.getLog(JamesCleanupListener.class);

    @Override
    public void lifecycleEvent(final LifecycleEvent event) {
        final Lifecycle lifecycle = event.getLifecycle();
        if (Lifecycle.AFTER_STOP_EVENT.equals(event.getType()) && lifecycle instanceof Context) {
            log.info("Clean up James resources");
            try {
                JVMAgentCleaner.close();
            } catch (Exception e) {
                log.error("Clean up James failed", e);
            }
        }
    }
}
