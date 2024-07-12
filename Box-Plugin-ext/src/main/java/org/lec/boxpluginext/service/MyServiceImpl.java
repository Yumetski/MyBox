package org.lec.boxpluginext.service;

import org.lec.boxplugininterface.service.MyService;
import org.springframework.stereotype.Service;

@Service
public class MyServiceImpl implements MyService {
    @Override
    public String hello() {
        return "这是插件实现的方法";
    }
}
