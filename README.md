RocketMQ 是阿里巴巴 2016 年开发的 MQ 中间件，使用 Java 语言开发，RocketMQ 是一款开源的**分布式消息系统**，基于高可用分布式集群技术，提供低延时的、高可靠的消息发布与订阅服务。同时，广泛应用于多个领域，包括异步通信解耦、企业解决方案、金融支付、电信、电子商务、快递物流、广告营销、社交、即时通信、移动应用、手游、视频、物联网、车联网等。具有以下特点：

1. 能够保证严格的消息顺序
2. 提供丰富的消息拉取模式
3. 高效的订阅者水平扩展能力
4. 实时的消息订阅机制
5. 亿级消息堆积能力

RocketMQ 对比 RabbitMQ 的核心组件和结构差异：

| 方面                 | RabbitMQ                                                     | RocketMQ                                                     |
| -------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| **核心组件**         | **Broker**：单一服务器（或集群），负责消息存储、路由和持久化。<br />**Exchange**：消息路由器，根据类型（Direct、Topic、Fanout、Headers）分发消息。<br />**Queue**：消息存储队列，绑定到 Exchange，支持镜像队列（HA 高可用）。<br />**Channel**：客户端与 Broker 的连接通道（基于 AMQP）。 | **NameServer**：轻量级命名服务（类似 ZooKeeper），管理 Broker 和 Topic 路由信息，无状态、可集群。<br />**Broker**：消息存储和转发服务器，支持 Master-Slave 复制（异步/同步），处理读写分离。 <br />**Producer Group**：生产者组，负载均衡发送消息。<br />**Consumer Group**：消费者组，支持 Push/Pull 模式，负载均衡消费。<br />**Topic & Queue**：Topic 下分多个 Queue（物理队列），实现分区存储和并行消费。 |
| **消息路由机制**     | 通过 Exchange 和 Binding Key 实现复杂路由（e.g., Topic Exchange 支持通配符）。<br /> 消息先到 Exchange，再路由到 Queue。 | 通过 Topic 和 Tag（子标签）过滤，路由到 Broker 的 Queue。<br />NameServer 提供路由表，Producer/Consumer 动态发现 Broker。 |
| **存储结构**         | 队列基于内存 + 磁盘持久化（Mnesia 数据库），支持镜像队列（多节点复制）。 <br /> 单 Broker 易瓶颈，集群需 Federation 或 Shovel 插件扩展。 | 分布式存储：消息存 CommitLog（顺序日志文件），ConsumeQueue（消费索引）。<br />支持多 Master 多 Slave，水平扩展强，Queue 可动态扩容。 |
| **集群与高可用**     | 集群模式：节点间镜像队列（Quorum Queue in 3.8+），但 Federation 跨集群较复杂。<br />依赖 Erlang OTP 框架，容错好但扩展需插件。 | 无中心化集群：NameServer 集群 + Broker 集群，Broker 支持多组 Master-Slave。 <br />内置 DLedger（Raft 协议）实现同步复制，易于云部署和高可用。 |
| **协议与通信**       | 标准 AMQP 0-9-1 协议，支持多种语言客户端。<br /> TCP 长连接，Channel 多路复用。 | 自定义二进制协议（基于 Netty），Java 原生优化。<br />支持长轮询（Pull 模式），减少网络开销。 |
| **扩展性与性能影响** | 适合中小规模，路由灵活但在海量消息下性能受 Exchange 影响（万级 TPS）。 | 设计为分布式，Queue 分片存储，易水平扩容（数十万 TPS），适合大数据场景。 |

> Docker 安装 RocketMQ

下面列举 Windows 端通过 Docker Desktop 安装 RocketMQ，Linux 端的安装可参照此博客： [博客链接](https://blog.csdn.net/Acloasia/article/details/130548105) ，已实际测试过可行。

1. **拉取 RocketMQ 镜像**

   ```cmd
   docker pull apache/rocketmq:latest
   ```

2. **创建容器共享网络 rocketmq**

   ```
   docker network create rocketmq
   ```

3. **启动容器** `NameServer`

   ```cmd
   docker run -d -p 9876:9876 -v D:/04-ProgramFiles/Docker/rocketmq/data/namesrv/logs:/root/logs -v D:/04-ProgramFiles/Docker/rocketmq/data/namesrv/store:/root/store --name rmqnamesrv -e "MAX_POSSIBLE_HEAP=100000000" rocketmqinc/rocketmq sh mqnamesrv
   ```

   要先创建好对应的本地文件目录。

4. **启动容器 Broker**

   ```cmd
   docker run -d -p 10911:10911 -p 10909:10909 -v D:/04-ProgramFiles/Docker/rocketmq/data/broker/logs:/opt/rocketmq-4.4.0/log -v D:/04-ProgramFiles/Docker/rocketmq/data/broker/store:/opt/rocketmq-4.4.0/store -v D:/04-ProgramFiles/Docker/rocketmq/conf/broker.conf:/opt/rocketmq-4.4.0/conf/broker.conf --name rmqbroker --link rmqnamesrv:namesrv -e "NAMESRV_ADDR=namesrv:9876" -e "JAVA_OPT=-Xms256m -Xmx256m -Xmn128m" rocketmqinc/rocketmq sh mqbroker -c /opt/rocketmq-4.4.0/conf/broker.conf
   ```

   要先创建好对应的本地文件目录，在对应文件夹下创建 `broker.conf` ，文件内容：

   ```conf
   brokerClusterName = DefaultCluster
   brokerName = broker-a
   brokerId = 0
   brokerIP1 = 0.0.0.0  
   brokerRole = ASYNC_MASTER
   flushDiskType = ASYNC_FLUSH
   deleteWhen = 04
   fileReservedTime = 72
   autoCreateTopicEnable = true
   autoCreateSubscriptionGroup = true
   tlsTestModeEnable = false
   
   
   listenPort = 10911  
   fastListenPort = 10909  
   ```

5. **拉取RocketMQ控制台（rocketmq-dashboard）**

   ```cmd
   docker pull apacherocketmq/rocketmq-dashboard:latest
   ```

6. **启动容器 Rocketmq-dashboard**

   ```cmd
   docker run -d --restart=always --name rmq-dashboard -p 8080:8082 --network rocketmq -e "JAVA_OPTS=-Xms256m -Xmx256m -Xmn128m -Drocketmq.namesrv.addr=rmqnamesrv:9876 -Dcom.rocketmq.sendMessageWithVIPChannel=false" apacherocketmq/rocketmq-dashboard:latest
   ```

7. **访问RMQ控制台**

   http://localhost:8080/

> 测试

+ 生产者端
  1. 创建消息生产者 producer，并制定生产者组名
  2. 指定 Nameserver 地址
  3. 启动 producer
  4. 创建消息对象，指定主题 Topic、Tag 和消息体等
  5. 发送消息
  6. 关闭生产者 producer
+ 消费者端
  1. 创建消费者 consumer，制定消费者组名
  2. 指定 Nameserver 地址
  3. 创建监听订阅主题 Topic 和 Tag 等
  4. 处理消息
  5. 启动消费者 consumer

引入依赖

```xml
<!--RocketMQ 依赖-->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-client</artifactId>
    <version>5.3.3</version>
</dependency>
```

