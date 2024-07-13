package org.lec.boxpluginweb;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScans;


@SpringBootApplication(scanBasePackages = {"org.lec.boxplugin", "org.lec.boxpluginweb"})
@EnableDiscoveryClient
public class BoxPluginApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoxPluginApplication.class, args);
    }

}
