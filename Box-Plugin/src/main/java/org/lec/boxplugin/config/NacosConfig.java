package org.lec.boxplugin.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.annotation.NacosProperties;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.annotation.NacosConfigListener;
import com.alibaba.nacos.api.exception.NacosException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.lec.boxplugin.loader.DynamicLoad;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Properties;
@Slf4j
public class NacosConfig {

    @Resource
    private JarList jarList;


//    @Resource
//    private DynamicLoad dynamicLoad;


    @NacosConfigListener(dataId = "mybox-dev.yaml")
    public void reConfig(String config){
        log.info("检测到nacos配置文件变动，开始更新插件信息");
        List<String> jarlist = jarList.getJarlist();
        jarlist.forEach(System.out::println);
    }
}
