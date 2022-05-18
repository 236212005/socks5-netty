# agent

代理客户端，安装在客户端机器上。用于将请求转发至socks5服务器。

## 运行

java -jar agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar [-l true|false] [-p port] [-a true|false]

-l: 是否打开流量日志，可选项为true/false，默认为false.

-p: 指定服务器监听端口号，默认为11080.

-a: 客户端链接socks5是否需要鉴权.

## 配置

- password.properties
    - user=password 鉴权用户密码，每行一个

- src/main/resources/logback.xml
    - 日志级别测试：<root level="info">
    - 日志级别开发：<root level="debug">
    - 日志级别开发：<root level="trace">

## 扩展

- 自定义鉴权方式

  实现PasswordAuth接口，通过proxyServer.passwordAuth()方法设置。系统自带的是PropertiesPasswordAuth，基于properties文件的鉴权

- 自定义代理日志

  实现ProxyFlowLog接口，通过proxyServer.proxyFlowLog()方法设置。系统自带的是ProxyFlowLog4j，基于log4j的日志记录
