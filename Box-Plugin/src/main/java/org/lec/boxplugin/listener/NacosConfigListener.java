package org.lec.boxplugin.listener;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.lec.boxplugin.config.JarList;
import org.lec.boxplugin.loader.DynamicLoad;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class NacosConfigListener implements ApplicationListener<EnvironmentChangeEvent> {

    private List<String> nowJarFiles = new ArrayList<>();
    @Resource
    private JarList jarList;

    @Resource
    private DynamicLoad dynamicLoad;

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        log.info("NacosConfigListener 检测到 nacos 配置文件变动，开始更新插件信息，新的加载信息为: {}", jarList.toString());
        List<String> jarNameList = jarList.getJarlist();

        unLoad(jarNameList);
        load(jarNameList);
    }

    @PostConstruct
    public void init(){
        List<String> jarNameList = jarList.getJarlist();
        log.info("读取到 nacos 配置信息，Spring 启动加载插件：{}", jarNameList.toString());
        load(jarNameList);
    }


    private void unLoad(List<String> jarNameList){
        Iterator<String> iterator = nowJarFiles.iterator();
        // 将配置文件中移除的jar，在jvm里进行卸载
        while(iterator.hasNext()){
            String jarName = iterator.next();
            if (!jarNameList.contains(jarName)){
                try {
                    dynamicLoad.unloadJar(jarName);
                    iterator.remove();
                    log.info("{} 动态卸载成功", jarName);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void load(List<String> jarNameList){
        Iterator<String> iterator = jarNameList.iterator();
        // 将新添加的jar给加载进jvm
        while(iterator.hasNext()){
            String jarName = iterator.next();
            if (!nowJarFiles.contains(jarName)){
                try {
                    dynamicLoad.loadJar(jarName, false);
                    nowJarFiles.add(jarName);
                } catch (Exception e) {
                    log.error("{} 动态加载失败", jarName);
                    throw new RuntimeException(e);
                }
                log.info("{} 动态加载成功", jarName);
            }
        }
    }
}
