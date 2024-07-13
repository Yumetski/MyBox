package org.lec.boxpluginweb.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.lang.reflect.Modifier;
import java.util.Objects;

@Slf4j
public class SpringAnnotationUtil {

    public static Boolean hasSpringAnnotation(Class<?> clazz){
        if (Objects.isNull(clazz)){
            return false;
        }
        if (clazz.isInterface()){
            return false;
        }

        if (Modifier.isAbstract(clazz.getModifiers())){
            return false;
        }
        try {
            if (clazz.getAnnotation(Component.class) != null ||
            clazz.getAnnotation(Repository.class) != null ||
            clazz.getAnnotation(Service.class) != null ||
            clazz.getAnnotation(Controller.class) != null ||
            clazz.getAnnotation(Configuration.class) != null) {
                return true;
            }
        }catch (Exception e){
            log.error("出现异常：{}", e.getMessage());
        }
        return false;
    }
}
