package org.lec.boxplugin.loader;

import java.io.IOException;
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
