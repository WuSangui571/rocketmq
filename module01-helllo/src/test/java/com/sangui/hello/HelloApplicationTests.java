package com.sangui.hello;


import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


@SpringBootTest
class HelloApplicationTests {
    public static final String NAME_SERVER_ADDRESS = "127.0.0.1:9876";

    @Test
    void TestProducer() throws Exception {
        // 创建一个生产者（制定一个组名）
        DefaultMQProducer producer = new DefaultMQProducer("test-producer-group");
        // 连接 NameServer
        producer.setNamesrvAddr(NAME_SERVER_ADDRESS);
        // 启动
        producer.start();
        // 创建消息
        Message message = new Message("testTopic","Hello RocketMQ!".getBytes());
        // 发送消息
        SendResult sendResult = producer.send(message);
        System.out.println("「日志」发送信息的状态码：" + sendResult.getSendStatus());
        // 关闭生产者
        producer.shutdown();
    }

    @Test
    void TestProducer2() throws Exception {
        // 强制IPv4，避免IPv6干扰
        System.setProperty("java.net.preferIPv4Stack", "true");

        DefaultMQProducer producer = new DefaultMQProducer("test-producer-group");
        producer.setNamesrvAddr(NAME_SERVER_ADDRESS);

        // 参数：延长超时，禁用VIP
        producer.setSendMsgTimeout(30000);  // 30秒发送超时
        producer.setVipChannelEnabled(false);  // 用主端口10911
        producer.setPollNameServerInterval(10000);  // 轮询间隔
        producer.setHeartbeatBrokerInterval(10000);  // 心跳间隔

        producer.start();

        // 调试：公共方法强制从NameServer更新并获取Topic的发布队列（路由信息）
        // 这会查询NameServer，返回MessageQueue列表（包含brokerName和queueId）
        List<MessageQueue> queues = producer.fetchPublishMessageQueues("testTopic");
        System.out.println("「调试」客户端路由队列数: " + queues.size() + ", 细节: " + queues);
        // 预期输出: 8个队列，如 [MessageQueue [topic=testTopic, brokerName=broker-a, queueId=0], ...]
        // 如果大小=8且brokerName=broker-a，路由完美（隐含地址127.0.0.1:10911）

        if (queues.isEmpty()) {
            throw new Exception("路由队列为空，NameServer未返回数据 - 等几秒重试或检查Topic");
        }

        Message message = new Message("testTopic", "Hello RocketMQ!".getBytes());

        // 发送带重试
        SendResult sendResult = null;
        for (int i = 0; i < 5; i++) {  // 加到5次重试
            try {
                sendResult = producer.send(message);
                System.out.println("「日志」发送信息的状态码: " + sendResult.getSendStatus());
                break;
            } catch (Exception e) {
                System.out.println("「警告」第 " + (i+1) + " 次重试失败: " + e.getMessage());
                Thread.sleep(3000);  // 等3秒
            }
        }

        if (sendResult == null) {
            throw new Exception("所有重试失败");
        }

        producer.shutdown();
    }
}
