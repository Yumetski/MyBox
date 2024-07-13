package org.lec.boxplugin.event;

import org.springframework.context.ApplicationEvent;

public class JarListUpdatedEvent extends ApplicationEvent {
    public JarListUpdatedEvent(Object source) {
        super(source);
    }
}
