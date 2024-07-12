package org.lec.boxplugin.util;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.lec.boxplugin.config.NacosConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NacosConfigUtil {

    @Resource
    private NacosConfig nacosConfig;

    private String dataId = "";

    @Value("${spring.nacos.config.group}")
    private String group;

//    public void addJarName(String jarName) throws NacosException, JsonProcessingException {
//        ConfigService configService = nacosConfig.configService();
//        String content = configService.getConfig(dataId, group, 5000);
//        YAMLMapper yamlMapper = new YAMLMapper();
//        ObjectMapper jsonMapper = new ObjectMapper();
//        Object yamlObject = yamlMapper.readValue(content, Object.class);
//
//        String jsonString = jsonMapper.writeValueAsString(yamlObject);
//        JSONObject jsonObject = JSONObject.parseObject(jsonString);
//        List<String> loadJars;
//        if (jsonObject.containsKey("loadjars")){
//            loadJars = (List<String>) jsonObject.get("loadjars");
//        }else {
//            loadJars = new ArrayList<>();
//        }
//
//        if (!loadJars.contains(jarName)){
//            loadJars.add(jarName);
//        }
//
//        jsonObject.put("loadjars", loadJars);
//
//        Object yaml = yamlMapper.readValue(jsonMapper.writeValueAsString(jsonObject), Object.class);
//        String newYamlString = yamlMapper.writeValueAsString(yaml);
//        boolean flag = configService.publishConfig(dataId, group, newYamlString);
//
//        if (flag){
//            log.info("nacos 配置更新成功");
//        }else {
//            log.info("nacos 配置更新失败");
//        }
//
//    }
}
