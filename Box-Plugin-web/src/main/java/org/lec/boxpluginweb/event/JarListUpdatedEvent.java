package org.lec.boxpluginweb.event;

import org.springframework.context.ApplicationEvent;

public class JarListUpdatedEvent extends ApplicationEvent {
    public JarListUpdatedEvent(Object source) {
        super(source);
    }
}
