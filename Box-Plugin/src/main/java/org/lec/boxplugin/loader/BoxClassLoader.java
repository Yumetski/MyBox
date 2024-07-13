package org.lec.boxplugin.loader;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BoxClassLoader extends URLClassLoader {

    private Map<String, Class<?>> loaderClasses = new ConcurrentHashMap<>();

    public BoxClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public Map<String, Class<?>> getLoaderClasses(){
        return loaderClasses;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clazz = loaderClasses.get(name);
        if (clazz != null){
            return clazz;
        }
        try {
            // 调用父类的findClass方法加载指定名称的类
            clazz = super.findClass(name);
            // 将加载的类添加到已加载的类集合中
            loaderClasses.put(name, clazz);
            return clazz;
        }catch (ClassNotFoundException e){
            e.printStackTrace();
            return null;
        }
    }

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
