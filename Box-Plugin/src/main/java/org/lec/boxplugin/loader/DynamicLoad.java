package org.lec.boxplugin.loader;

import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.lec.boxplugin.util.SpringAnnotationUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


@Slf4j
@Component
public class DynamicLoad {

    @Resource
    private ApplicationContext applicationContext;

    private Map<String, BoxClassLoader> boxClassLoaderMap = new ConcurrentHashMap<>();

    @Value("${box-plugin.path}")
    private String path;

    public void loadJar(String fileName, Boolean isRegisterXxlJob) throws Exception {
        File file = new File(path + "/" + fileName);
        HashMap<String, String> jobPar = new HashMap<>();
        // 获取beanFactory
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        try {
            // URLClassLoader加载jar包规范必须这么写
            URL url = new URL("jar:file:" + file.getAbsolutePath() + "!/");
            URLConnection urlConnection = url.openConnection();
            JarURLConnection jarURLConnection = (JarURLConnection) urlConnection;
            //获取jar文件
            JarFile jarFile = jarURLConnection.getJarFile();
            Enumeration<JarEntry> entries = jarFile.entries();

            //创建自定义类加载器，并添加到map中方便管理
            BoxClassLoader boxClassLoader = new BoxClassLoader(new URL[]{url}, ClassLoader.getSystemClassLoader());
            boxClassLoaderMap.put(fileName, boxClassLoader);
            HashSet<Class> initBeanClass = new HashSet<>(jarFile.size());
            while(entries.hasMoreElements()){
                JarEntry jarEntry = entries.nextElement();
                if (jarEntry.getName().endsWith(".class")){
                    // 加载类到jvm中
                    // 获取类的全路径名
                    String className = jarEntry.getName().replace('/', '.').substring(0, jarEntry.getName().length() - 6);
                    // 进行反射
                    boxClassLoader.loadClass(className);
                }
            }
            Map<String, Class<?>> loaderClasses = boxClassLoader.getLoaderClasses();
//            XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
            for (Map.Entry<String, Class<?>> entry : loaderClasses.entrySet()){
                String className = entry.getKey();
                Class<?> clazz = entry.getValue();
                // 将有@spring注解的类交给spring管理
                // 判断是否注入spring
                Boolean flag = SpringAnnotationUtil.hasSpringAnnotation(clazz);
                if (flag){
                    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
                    AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
                    //此处beanName使用全路径名是为了防止beanName重复
                    String packageName = className.substring(0, className.lastIndexOf(".") + 1);
                    String beanName = className.substring(className.lastIndexOf(".") + 1);
                    beanName = packageName + beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
                    //注册到spring的beanFactory中
                    beanFactory.registerBeanDefinition(beanName, beanDefinition);
                    // 允许注入和反向注入
                    beanFactory.autowireBean(clazz);
                    beanFactory.initializeBean(clazz, className);
                    initBeanClass.add(clazz);
                }

                // 带有XxlJob注解的方法注册任务
                //过滤方法
//                Map<Method, XxlJob> annotatedMethods = null;
//                try {
//                    annotatedMethods = MethodIntrospector.selectMethods(clazz, new MethodIntrospector.MetadataLookup<XxlJob>() {
//                        @Override
//                        public XxlJob inspect(Method method) {
//                            return AnnotatedElementUtils.findMergedAnnotation(method, XxlJob.class);
//                        }
//                    });
//                }catch (Throwable e){
//                    log.error("出现错误:{}", e.getMessage());
//                }
//                // 生成并注册方法的jobHandler
//                for (Map.Entry<Method, XxlJob> methodXxlJobEntry : annotatedMethods.entrySet()){
//                    Method executeMethod = methodXxlJobEntry.getKey();
//                    XxlJob xxlJob = executeMethod.getAnnotation(XxlJob.class);
//                    if (Objects.isNull(xxlJob)) {
//                        throw new Exception(executeMethod.getName() + "(), 没有添加@XxlJob注解配置定时策略");
//                    }
//                    if (CronExpression.isValidExpression(xxlJob.value())){
//                        throw new Exception(executeMethod.getName() + "(),@XxlJob参数内容错误");
//                    }
//                    XxlJob value = methodXxlJobEntry.getValue();
//                    jobPar.put(xxlJob.value(), xxlJob.value());
//                    if (isRegisterXxlJob){
//                        executeMethod.setAccessible(true);
//                        //regist
//                        Method initMethod = null;
//                        Method destroyMethod = null;
//                        // todo xxljob加载没有写完
//                    }
//                }
            }
            initBeanClass.forEach(beanFactory :: getBean);
        } catch (IOException e) {
            log.error("读取{}文件异常", fileName);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void unloadJar(String fileName) throws NoSuchFieldException, IllegalAccessException {
        //获取加载当前jar的类加载器
        BoxClassLoader boxClassLoader = boxClassLoaderMap.get(fileName);

        //获取jobHandlerRepository私有属性，为了卸载xxlJob任务
//        Field privateFiled = XxlJobExecutor.class.getDeclaredField("jobHandlerRepository");
        //设置私有属性可以访问
//        privateFiled.setAccessible(true);
//        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
//        ConcurrentHashMap<String, IJobHandler> jobHandlerRepository = (ConcurrentHashMap<String, IJobHandler>) privateFiled.get(xxlJobSpringExecutor);
        // 获取beanFactory, 准备从spring中卸载
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        Map<String, Class<?>> loaderClasses = boxClassLoader.getLoaderClasses();

        HashSet<String> beanNames = new HashSet<>();
        for (Map.Entry<String, Class<?>> entry : loaderClasses.entrySet()){
            // 将xxljob任务从执行器中移除
            // 截取beanName
            String key = entry.getKey();
            String packageName = key.substring(0, key.lastIndexOf(".") + 1);
            String beanName = key.substring(key.lastIndexOf(".") + 1);
            beanName = packageName + beanName.substring(0, 1).toLowerCase() + beanName.substring(1);

            // 获取bean，如果获取失败，表明这个类没有加入到Spring容器中，则跳出本次循环
            Object bean = null;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception e){
                continue;
            }

//            Map<Method, XxlJob> annotatedMethods = null;
//            try {
//                annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(), new MethodIntrospector.MetadataLookup<XxlJob>() {
//                    @Override
//                    public XxlJob inspect(Method method) {
//                        return AnnotatedElementUtils.findMergedAnnotation(method, XxlJob.class);
//                    }
//                });
//            }catch (Throwable e){
//                e.printStackTrace();
//            }
            //将job从执行器中移除
//            for (Map.Entry<Method, XxlJob> methodXxlJobEntry : annotatedMethods.entrySet()){
//                XxlJob xxlJob = methodXxlJobEntry.getValue();
//                jobHandlerRepository.remove(xxlJob.value());
//            }
            // 从Spring中移除，这里的移除仅仅移除的bean，并未移除bean定义
            beanNames.add(beanName);
            beanFactory.destroyBean(beanName, bean);
        }

        Field mergedBeanDefinitions = beanFactory.getClass().getSuperclass().getSuperclass().getDeclaredField("mergedBeanDefinitions");
        mergedBeanDefinitions.setAccessible(true);
        Map<String, RootBeanDefinition> rootBeanDefinitionMap = (Map<String, RootBeanDefinition>) mergedBeanDefinitions.get(beanFactory);
        for (String beanName : beanNames){
            beanFactory.removeBeanDefinition(beanName);
            // 父类bean定义去除
            rootBeanDefinitionMap.remove(beanName);
        }
        mergedBeanDefinitions.setAccessible(false);
        // 卸载父任务，子任务已经在循环中卸载
//        jobHandlerRepository.remove(fileName);
        try {
            Field field = ClassLoader.class.getDeclaredField("classes");
            field.setAccessible(true);
            Vector<Class<?>> classes = (Vector<Class<?>>) field.get(boxClassLoader);
            classes.removeAllElements();
            boxClassLoaderMap.remove(fileName);
            boxClassLoader.unload();
        }catch (NoSuchFieldException e){
            log.error("动态卸载的类，从类加载器中卸载失败");
            e.printStackTrace();
        }catch (IllegalAccessException e){
            log.error("动态卸载的类，从类加载器中卸载失败");
            e.printStackTrace();
        }
    }

}
