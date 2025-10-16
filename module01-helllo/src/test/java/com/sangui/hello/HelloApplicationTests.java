package com.sangui.hello;


import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


@SpringBootTest
class HelloApplicationTests {
    public static final String NAME_SERVER_ADDRESS = "172.26.16.1:9876";
    //public static final String NAME_SERVER_ADDRESS = "127.0.0.1:9876";
    //public static final String NAME_SERVER_ADDRESS = "172.18.0.2:9876";


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

    @Test
    void TestProducer3() throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");  // 强制IPv4，避免IPv6干扰

        DefaultMQProducer producer = new DefaultMQProducer("test-producer-group");
        producer.setNamesrvAddr(NAME_SERVER_ADDRESS);
        producer.setVipChannelEnabled(false);  // 禁用VIP通道（用10911主端口，避免10909握手）
        producer.setSendMsgTimeout(30000);  // 延长超时
        producer.setRetryAnotherBrokerWhenNotStoreOK(true);  // 重试其他Broker（虽单节点，但稳）

        producer.start();

        // 调试：打印路由，确认Broker地址
        List<MessageQueue> queues = producer.fetchPublishMessageQueues("testTopic");
        System.out.println("「调试」路由队列: " + queues);  // 应显示 [MessageQueue [topic=testTopic, brokerName=broker-a, queueId=0..7] @ 172.26.16.1]

        Message message = new Message("testTopic", "Hello RocketMQ!".getBytes());
        SendResult sendResult = producer.send(message);  // 或sendOneway(message)忽略结果
        System.out.println("「日志」发送信息的状态码: " + sendResult.getSendStatus());
        System.out.println("「日志」完整结果: " + sendResult);  // 看msgId、queue等

        producer.shutdown();
    }

    @Test
    void TestProducer4() throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");  // 强制IPv4

        DefaultMQProducer producer = new DefaultMQProducer("test-producer-group");
        producer.setNamesrvAddr(NAME_SERVER_ADDRESS);
        producer.setVipChannelEnabled(false);  // 禁用VIP（用10911主端口）
        producer.setSendMsgTimeout(30000);     // 超时30s
        producer.setRetryTimesWhenSendFailed(2);  // 重试2次
        producer.setDefaultTopicQueueNums(4);  // 默认队列数（匹配你的8个）

        producer.start();

        // 强制刷新并打印路由（确认Broker地址和队列）
        List<MessageQueue> queues = producer.fetchPublishMessageQueues("testTopic");
        System.out.println("「调试」路由队列列表: " + queues);  // 预期: MessageQueue [topic=testTopic, brokerName=broker-a, queueId=0..7]，内部地址172.26.16.1:10911
        if (queues.isEmpty()) {
            throw new Exception("路由为空，Topic未创建或传播延迟");
        }

        // 创建Topic如果未存在（客户端自动，但显式确保）
        // producer.createTopic("testTopic", "testTopic", 8);  // 已autoCreate

        Message message = new Message("testTopic", "Hello RocketMQ!".getBytes());
        SendResult sendResult = producer.send(message);  // 同步发送
        System.out.println("「日志」发送状态: " + sendResult.getSendStatus());  // 应SEND_OK
        System.out.println("「日志」消息ID: " + sendResult.getMsgId());  // 确认存储
        System.out.println("「日志」队列: " + sendResult.getMessageQueue());  // queueId

        producer.shutdown();
    }

    @Test
    void TestProducer5() throws Exception {
        // 创建一个生产者（制定一个组名）
        DefaultMQProducer producer = new DefaultMQProducer("test-producer-group");
        // 连接 NameServer
        producer.setNamesrvAddr(NAME_SERVER_ADDRESS);
        // 启动
        producer.start();



        // 创建消息
        Message message = new Message("testTopic","Hello RocketMQ!".getBytes());


        SendResult sendResult = producer.send(message, new MessageQueueSelector() {
            @Override
            public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                int queueId = (int) (arg); // 选固定队列
                return mqs.get(queueId % mqs.size());
            }
        }, 0); // arg=0 选队列0

        // 发送消息
        //SendResult sendResult = producer.send(message);
        System.out.println("「日志」发送信息的状态码：" + sendResult.getSendStatus());
        // 关闭生产者
        producer.shutdown();
    }
}
