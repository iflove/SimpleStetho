# SimpleStetho [![Build Status](https://travis-ci.org/facebook/stetho.svg?branch=master)](https://travis-ci.org/facebook/stetho)

[SimpleStetho ](https://github.com/iflove/SimpleStetho) 一款基于[Stetho ](https://github.com/facebook/stetho) 为Android应用程序提供数据库、LOG日志信息调试桥梁简单工具库。开发人员可以使用 `MQTT`  框架使用它的功能。



## Set-up

### Download
 Gradle:
```groovy
compile 'com.roogle.simple.stetho:stetho-mqtt:3.1.1'
```
or Maven:
```xml
<dependency>
  <groupId>com.roogle.simple.stetho</groupId>
  <artifactId>stetho-mqtt</artifactId>
  <version>3.1.1</version>
  <type>pom</type>
</dependency>
```

不过，你项目中已有 `MQTT` 也可以单独使用stetho:

```groovy
compile 'com.roogle.simple.stetho:stetho:3.1.1'
```


### Using

SimpleStetho 保留了database 相关大部分api,提供了`DatabaseProvider`  访问数据库, `LogcatProvider` 访问日志信息, 初始化 `SimpleStetho` class:

```kotlin
val simpleStetho = SimpleStetho(this)
//开启收集Logcat日志、建议Application Create 时收集
simpleStetho.logcatProvider.startGatherLogcatInfo()
//通过数据库名获取表信息
simpleStetho.databaseProvider.getTableNamesTableText("dbName")
```
当然也可以使用`StethoMqttClient`  :

```kotlin
StethoMqttClient(simpleStetho, serverUri, clientId, "topic/client/${clientId}", "topic/server/${clientId}")
stethoMqttClient.connect()               
```

具体用法参见Demo.

#### SimpleStetho 作用
SimpleStetho 应用在物联网各种设备上如TV/车载/贩卖机等，十分便利获取你所需的信息:

采用 **MQTT** 与设备（client端）通信.

服务端订阅主题以及发布消息主题

```java
topic/server/paho-30505377805850
topic/client/paho-30505377805850
```

设备端订阅主题以及发布消息主题

```java
topic/client/paho-30505377805850
topic/server/paho-30505377805850
```



####  服务器发送查询指令

##### 查询数据库指令格式 （query_database）

```json
{
    "action": "query_database",
    "message": {
        "sql": "sql 语句"
    }
}
```
默认返回表格形式的字符串。可以根据个人需求重写数据库查询返回

```
|=============================================================================================|
|  _id  |  uuid                              |  name             |  updated                   |
|=============================================================================================|
|  1    |  4af98cdb0a8b4bdd84abf5005ef0d42c  |  呦呦奶茶（原味） |  2017-12-28T04:30:24.028Z  |
|---------------------------------------------------------------------------------------------|
|  2    |  72979b7d6e094dbf92c0e6cb5a6fb8bf  |  士力架           |  2017-12-28T04:30:24.094Z  |
|---------------------------------------------------------------------------------------------|
|  3    |  d5f0dd29fba642aa80d7fb16f9828010  |  伊利纯牛奶       |  2017-12-28T04:30:24.114Z  |
|---------------------------------------------------------------------------------------------|

```

- 执行成功直接返回Table表格信息
- 执行失败直接返回错误信息(SQL 语法错误不返回)



##### 查询Logcat指令格式（query_logcat）

```json
{
    "action": "query_logcat",
    "message": {
        "begin": "UTC string(2018-01-03T03:02:55.011Z)",
        "end": "UTC string"
    }
}
```
> 接收到 **query_logcat** 命令并处理成功时

```
2017-05-27.txt
日志文本信息...
```


##### 查询anr 日志指令格式（query_logcat_anr）

```json
{"action":"query_logcat_anr"}
```
- 需要有读取 `/data/anr/traces.txt` 权限

### NOTE

    1. 执行**query_database** 查询返回row 限制每页 250 条
    2. 执行**query_logcat** 查询返回日志文本大小最大限制 1MB ，begin end超过一天的，直接返回错误。
    3. 另外只保存30天日志文件在`/sdcard/Android/data/com.baoda.vending.youzi/files/logs` 目录下



## 走得更远

### 自定义查询数据相关指令

> 直接用sql 语句作为查询，并 不安全。你可以使用DatabaseProvider 中api 完全实现新的指令



## 改善 SimpleStetho!
欢迎提出问题



## MQTT 工具

mqtt 工具有很多

- [eclipse-paho](https://repo.eclipse.org/content/repositories/paho-releases/org/eclipse/paho/org.eclipse.paho.ui.app/) 



## License

Stetho is BSD-licensed. We also provide an additional patent grant.
