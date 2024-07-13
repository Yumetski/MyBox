# Box-Plugin框架
***

## 项目背景

本人在美团实习的时候了解到该技术，并且在美团内部，尤其是我们部门很多服务都是采用这样的插件化部署，这样不仅增加了程序的可扩展性，可以动态的修改业务逻辑代码，并且还节省了服务器等资源。

但由于我没办法查看美团该框架的源码，只能查看相关文档，再经过查阅网上的资料，最终做出来这个低配版的动态加载框架。


## 项目架构
```angular2html
MyBox
---Box-Plugin               //框架核心代码
---Box-Plugin-ext           //依赖Box-Plugin-interface，实现了公共模块中的接口
---Box-Plugin-interface     //测试的公共模块，只是提供依赖
---Box-Plugin-web           //测试使用的web服务，同样依赖Box-Plugin-interface
```

整个Git项目结构如上，核心代码是`Box-Plugin`, 具体使用参考下面三个服务。

## 框架功能

目前实现的功能有：

- [x] 根据配置文件动态加载和卸载jar包中的类
- [x] 根据插件中的spring注解，将含有spring注解的类注入到服务的spring容器中
- [ ] 将插件中的mapper文件解析和mapper类并加入到spring容器中
- [ ] 与某些的rpc框架进行适配
- [ ] 完整的管理后台去管理插件包，检测插件的具体情况

大概想到还有这些功能，如果有想法欢迎来讨论。

## 使用方法

Nacos 的配置文件如下
```yaml
box-plugin: 
 path: D:/project/box-plugin-path/                    //插件jar包所在目录，所有插件jar包需要放在一个目录下
 jarlist: 
  - jarName: Box-Plugin-ext-0.0.1-SNAPSHOT.jar        //jar包的名字
    classLoadPackage: org/lec/boxpluginext            //jar包里面的源根路径
```

有多个jar需要加载的话就按照以上格式在`box-plugin.jarlist`的下面继续添加就可以了

同时，如果选择去依赖`Box-Plugin`整个框架，那么在springboot的启动类需要去手动指定spring扫描的路径，如下：
```java
@SpringBootApplication(scanBasePackages = {"org.lec.boxplugin", "org.lec.boxpluginweb"})
@EnableDiscoveryClient
public class BoxPluginApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoxPluginApplication.class, args);
    }

}
```
也就是说需要让spring去扫描`org.lec.boxplugin`这个路径，不然框架中的很多bean是没有办法注入到spring容器中（这里有可能是我这个项目是父子多模块项目的原因，总之如果出现找不到`bean`的情况就手动指定扫描路径）

## 注意事项 

使用idea启动服务的话会出现`java.io.IOException: java.nio.file.InvalidPathException: Illegal char <:> at index 18: D:\project\MyBox\......`，这个异常并不会影响服务动态加载插件，并且将服务打成jar包后启动就不会出现这个异常

出现这个异常的原因是在加载过程中会尝试获取该服务依赖的jar包，但是使用idea启动依赖的直接是maven仓库的jar包，无法从服务的根路径下获取，所以会出现报错，但对服务本身并无太大影响，可能会导致该服务自己已经依赖的jar包，又去加载这个jar包（正常人都不会这么做吧...)，感兴趣的可以去看看源码
