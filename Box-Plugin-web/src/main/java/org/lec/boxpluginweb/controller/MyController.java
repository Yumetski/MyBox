package org.lec.boxpluginweb.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.lec.boxplugin.config.JarInfoList;
import org.lec.boxplugininterface.service.MyService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class MyController {

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private JarInfoList jarInfoList;


    @GetMapping("/hello")
    public String hello(){
        log.info("jarList 中的值为：{}", jarInfoList.getJarlist().toString());
        try {
            MyService myService = (MyService) applicationContext.getBean("org.lec.boxpluginext.service.myServiceImpl");
            log.info("成功获取到bean：{}", myService);
            return myService.hello();
        } catch (BeansException e) {
            log.error("出现异常:{}", e.getMessage());
        }
        return "这是本地方法";
    }
}
