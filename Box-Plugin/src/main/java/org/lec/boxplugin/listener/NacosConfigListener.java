package org.lec.boxplugin.listener;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.lec.boxplugin.config.JarList;
import org.lec.boxplugin.event.JarListUpdatedEvent;
import org.lec.boxplugin.loader.DynamicLoad;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
public class NacosConfigListener {

    private List<String> nowJarFiles = new ArrayList<>();
    @Resource
    private JarList jarList;

    @Resource
    private DynamicLoad dynamicLoad;

    @Resource
    private NacosConfigManager nacosConfigManager;

    @Resource
    private NacosConfigProperties nacosConfigProperties;

    @EventListener(JarListUpdatedEvent.class)
    public void handleJarListUpdated(JarListUpdatedEvent event){
        List<String> jarNameList = jarList.getJarlist();
        log.info("监听到 nacos 更新配置信息，更新后的配置信息为：{}", jarNameList.toString());
        unLoad(jarNameList);
        load(jarNameList);
    }

    @PostConstruct
    public void init(){
        List<String> jarNameList = jarList.getJarlist();
        log.info("读取到 nacos 配置信息，Spring 启动加载插件：{}", jarNameList.toString());
        load(jarNameList);

//        try {
//            nacosConfigManager.getConfigService().addListener("mybox-dev.yaml", "DEFAULT_GROUP", new Listener() {
//                @Override
//                public Executor getExecutor() {
//                    return null;
//                }
//
//                @Override
//                public void receiveConfigInfo(String s) {
//                    log.info("监听到 nacos 更新配置信息，更新后的配置信息为：{}", jarNameList.toString());
//                    unLoad(jarNameList);
//                    load(jarNameList);
//                }
//            });
//        } catch (NacosException e) {
//            throw new RuntimeException(e);
//        }
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
