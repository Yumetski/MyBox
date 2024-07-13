package org.lec.boxplugin.config;

import jakarta.annotation.Resource;
import lombok.Data;
import org.lec.boxplugin.event.JarListUpdatedEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
@ConfigurationProperties(prefix = "box-plugin")
public class JarInfoList {
    private List<JarInfo> jarlist;

    @Data
    public static class JarInfo{
        private String jarName;
        private String classLoadPackage;
    }



    @Resource
    private ApplicationEventPublisher eventPublisher;

    public List<JarInfo> getJarlist() {
        return jarlist;
    }

    public void setJarlist(List<JarInfo> jarlist) {
        this.jarlist = jarlist;
        // 当发现nacos的配置发生更改后会发送事件，提醒服务nacos的配置已经变更
        eventPublisher.publishEvent(new JarListUpdatedEvent(this));
    }

    public boolean isContains(String jarName){
        for (JarInfo jarInfo : jarlist){
            if (jarInfo.getJarName().equals(jarName)){
                return true;
            }
        }
        return false;
    }
}
