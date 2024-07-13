package org.lec.boxpluginweb.registrar;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PluginImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginImportBeanDefinitionRegistrar.class);

    //匹配数字的正则
    private static Pattern pattern = Pattern.compile("[0-9]");

    /**
     * jar包存放路径
     */
    private String libPath;

    /**
     * 动态加载jar包名称，多个用英文逗号隔开
     */
    private String loadJarNames;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        //获取要动态加载的jar列表
        List<String> jarList = new LinkedList<>();
        if (Strings.isNotBlank(loadJarNames)) {
            jarList.addAll(Arrays.asList(loadJarNames.split(",")));
        }
        ApplicationHome home = new ApplicationHome();
        File applicationJarFile = home.getSource();
        String applicationJarFilePath = applicationJarFile.getAbsolutePath();
        List<String> classJars = getDependence(applicationJarFilePath);
        //开始加载jar包
        try {
            if (jarList.size() > 0) {
                //循环按顺序加载
                for (String jarName : jarList) {
                    if (validateJarAndVersion(classJars, jarName)) {
                        LOGGER.info("开始热加载jar包 ---------------> {}", jarName);
                        ClassPathResource classPathResource = new ClassPathResource(libPath + "/" + jarName);
                        File jar = new File("/temp/" + jarName);
                        FileUtils.copyToFile(classPathResource.getInputStream(), jar);
                        JarFile jarFile = new JarFile(jar);
                        URI uri = jar.toURI();
                        URL url = uri.toURL();
                        //获取classloader
                        URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
                        //利用反射获取方法
                        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                        if (!method.isAccessible()) {
                            method.setAccessible(true);
                        }
                        method.invoke(classLoader, url);
                        for (Enumeration<JarEntry> ea = jarFile.entries(); ea.hasMoreElements(); ) {
                            JarEntry jarEntry = ea.nextElement();
                            String name = jarEntry.getName();
                            if (name != null && name.endsWith(".class")) {
                                String loadName = name.replace("/", ".").substring(0, name.length() - 6);
                                //加载class
                                Class<?> c = classLoader.loadClass(loadName);
                                //注册bean
                                insertBean(c, registry);
                            }
                        }
                        LOGGER.info("结束热加载jar包 ---------------> {}", jarName);
                        jar.delete();
                    } else {
                        LOGGER.info("依赖中已存在该jar包 ---------------> {}", jarName);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("热加载jar包异常");
            e.printStackTrace();
        }
    }

    private void insertBean(Class<?> c, BeanDefinitionRegistry registry) {
        if (isSpringBeanClass(c)) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(c);
            BeanDefinition beanDefinition = builder.getBeanDefinition();
            registry.registerBeanDefinition(c.getName(), beanDefinition);
        }
    }

    //获取依赖的jar包名称列表
    private List<String> getDependence(String jarPath) {
        List<String> result = new LinkedList<>();
        if (!jarPath.endsWith(".jar")) {
            return result;
        }
        URL url;
        try {
            url = new URL("jar:file:/" + jarPath + "/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return result;
        }
        try {
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            JarFile jarFile = connection.getJarFile();
            for (Enumeration<JarEntry> ea = jarFile.entries(); ea.hasMoreElements(); ) {
                JarEntry jarEntry = ea.nextElement();
                if (jarEntry.getName().startsWith("/BOOT-INF/lib/") && jarEntry.getName().endsWith(".jar")) {
                    result.add(jarEntry.getName().substring((jarEntry.getName().lastIndexOf("/") + 1)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    //判断是否存在同样的依赖jar包名，TODO：判断版本号高低
    private boolean validateJarAndVersion(List<String> dependenceList, String jarName) {
        boolean result = true;
        int firstNumIndex = getFirstNumIndex(jarName);
        String jarSimpleName = jarName.substring(0, firstNumIndex);
        for (String dependence : dependenceList) {
            if (dependence.startsWith(jarSimpleName)) {
                result = false;
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

    /**
     * 方法描述 判断class对象是否带有spring的注解
     *
     * @param cla jar中的每一个class
     * @return true 是spring bean   false 不是spring bean
     * @method isSpringBeanClass
     */
    public boolean isSpringBeanClass(Class<?> cla) {
        if (cla == null) {
            return false;
        }
        //是否是接口
        if (cla.isInterface()) {
            return false;
        }
        //是否是抽象类
        if (Modifier.isAbstract(cla.getModifiers())) {
            return false;
        }
        if (cla.getAnnotation(Component.class) != null) {
            return true;
        }
        if (cla.getAnnotation(Repository.class) != null) {
            return true;
        }
        if (cla.getAnnotation(Service.class) != null) {
            return true;
        }
        return false;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.libPath = environment.getProperty("libPath");
        this.loadJarNames = environment.getProperty("loadJarNames");
    }
}