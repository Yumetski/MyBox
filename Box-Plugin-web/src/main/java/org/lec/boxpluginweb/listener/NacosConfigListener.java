package org.lec.boxpluginweb.listener;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.lec.boxpluginweb.config.JarList;
import org.lec.boxpluginweb.event.JarListUpdatedEvent;
import org.lec.boxpluginweb.loader.DynamicLoad;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Component
public class NacosConfigListener {

    private List<String> nowJarFiles = new ArrayList<>();

    private static final Pattern pattern = Pattern.compile("[0-9]");

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
        String property = System.getProperty("user.dir");
        String property1 = System.getProperty("java.class.path");
        String applicationJarFilePath = property + File.separatorChar + property1;

        List<String> classJars = getDependence(applicationJarFilePath);
        log.info("主jar已经引用的jar：{}",classJars);


        Iterator<String> iterator = jarNameList.iterator();

        // 将新添加的jar给加载进jvm
        while(iterator.hasNext()){
            String jarName = iterator.next();
            if (!nowJarFiles.contains(jarName) && validateJarAndVersion(classJars, jarName)){
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

    private List<String> getDependence(String jarPath) {
        List<String> result = new LinkedList<>();
        if (!jarPath.endsWith(".jar")) {
            return result;
        }
        try {
            log.info("jarPath:"+jarPath);
            JarFile jarFile = new JarFile(jarPath);
            for (Enumeration<JarEntry> ea = jarFile.entries(); ea.hasMoreElements(); ) {
                JarEntry jarEntry = ea.nextElement();
                if (jarEntry.getName().startsWith("BOOT-INF/lib/") && jarEntry.getName().endsWith(".jar")) {
                    result.add(jarEntry.getName().substring((jarEntry.getName().lastIndexOf("/") + 1)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    //判断是否存在同样的依赖jar包名，判断版本号高低
    private boolean validateJarAndVersion(List<String> dependenceList, String jarName) {
        boolean result = true;
        int firstNumIndex = getFirstNumIndex(jarName);
        String jarSimpleName = jarName.substring(0, firstNumIndex);
        for (String dependence : dependenceList) {
            if (dependence.startsWith(jarSimpleName)) {
                //判断版本号高低
                DefaultArtifactVersion version1 = new DefaultArtifactVersion(dependence);
                DefaultArtifactVersion version2 = new DefaultArtifactVersion(jarName);
                int res = version1.compareTo(version2);
                if(res >= 0){
                    //已经引入最新版本
                    log.info("dependence:{};jarName:{};res:{}",dependence,jarName,res);
                    result = false;
                }
                break;
            }
        }
        return result;
    }

    //获取名字中第一个出现数字的下标
    private int getFirstNumIndex(String jarName) {
        Matcher matcher = pattern.matcher(jarName);
        if (matcher.find()) {
            return matcher.start();
        } else {
            return -1;
        }
    }
}
