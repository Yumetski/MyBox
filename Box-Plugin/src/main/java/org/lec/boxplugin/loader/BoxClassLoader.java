package org.lec.boxplugin.loader;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BoxClassLoader extends URLClassLoader {

    private Map<String, Class<?>> loaderClasses = new ConcurrentHashMap<>();

    public BoxClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public Map<String, Class<?>> getLoaderClasses(){
        return loaderClasses;
    }

    @Override
    protected Class<?> findClass(String name){
        Class<?> clazz = loaderClasses.get(name);
        if (clazz != null){
            return clazz;
        }
        try {
            /**
             * 获取当前线程上下文加载器
             * 因为有可能会加载spring的某些类或者注解，比如@Service，需要注入Spring容器，为避免重复加载和加载的类不一致等原因，先尝试使用当前线程上下文加载器加载
             * 如果没有，则再尝试使用自定义的类加载器
             */
            ClassLoader appClassLoader = Thread.currentThread().getContextClassLoader();
            clazz = appClassLoader.loadClass(name);
            return clazz;
        }catch (ClassNotFoundException e){
            log.error("当前线程上下文加载器没有找到该类，调用子类加载器 ClassNotFoundException ：{}", e.toString());
            try {
                // 将加载的类添加到已加载的类集合中，避免重复加载
                clazz = super.findClass(name);
                loaderClasses.put(name, clazz);
            } catch (ClassNotFoundException ex) {
                log.error("子类加载器也没有该类，ClassNotFoundException：{}", ex.toString());
            }
        }
        return clazz;
    }

    /**
     * 在网上看了很多卸载类的实现，大多数是将关于该类的所有引用给删除，再手动去gc，在一个运行的服务中频繁的gc是很影响性能的，所以这里只是将该类的classLoad给关闭，让jvm自己去判断gc的时间
     */
    public void unloadClasses() {
        // 复制一份类的引用
        Map<String, Class<?>> classesToUnload = new ConcurrentHashMap<>(loaderClasses);

        // 遍历要卸载的类并清理
        for (Map.Entry<String, Class<?>> entry : classesToUnload.entrySet()) {
            String className = entry.getKey();
            Class<?> clazz = entry.getValue();

            // 从缓存中移除类
            loaderClasses.remove(className);
        }

        // 尝试关闭类加载器
        try {
            close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Deprecated
    public void unload(){
        try {
            for (Map.Entry<String, Class<?>> entry : loaderClasses.entrySet()){
                String className = entry.getKey();
                loaderClasses.remove(className);
                try {
                    // 调用destroy方法，回收资源
                    Class<?> clazz = entry.getValue();
                    Method destroy = clazz.getDeclaredMethod("destroy", new Class<?>[0]);
                    destroy.invoke(clazz);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

