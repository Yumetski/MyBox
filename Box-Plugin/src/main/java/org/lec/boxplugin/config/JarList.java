package org.lec.boxplugin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@RefreshScope
@Configuration
@Data
@ConfigurationProperties(prefix = "box-plugin")
public class JarList {
    private List<String> jarlist;
}
