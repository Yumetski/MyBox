package org.lec.boxpluginweb.config;

import jakarta.annotation.Resource;
import org.lec.boxpluginweb.event.JarListUpdatedEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
@ConfigurationProperties(prefix = "box-plugin")
public class JarList {
    private List<String> jarlist;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    public List<String> getJarlist() {
        return jarlist;
    }

    public void setJarlist(List<String> jarlist) {
        this.jarlist = jarlist;
        // 发布更新事件
        eventPublisher.publishEvent(new JarListUpdatedEvent(this));
    }
}
