package com.sangui.hello;


import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
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
        // 创建一个生产者（指定一个组名）
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
        // 创建一个生产者（指定一个组名）
        DefaultMQProducer producer = new DefaultMQProducer("test-producer-group");
        // 连接 NameServer
        producer.setNamesrvAddr(NAME_SERVER_ADDRESS);
        // 启动
        producer.start();

        for (int i = 0; i < 10; i++) {
            // 创建消息
            Message message = new Message("testTopic","Hello RocketMQ!".getBytes());

            // 发送消息
            SendResult sendResult = producer.send(message);
            System.out.println("「日志」发送信息的状态码：" + sendResult.getSendStatus());
        }

        // 关闭生产者
        producer.shutdown();
    }
    /**
     * 测试消费者
     *
     * @throws Exception 异常
     */
    @Test
    public void testConsumer() throws Exception {
        // 创建一个消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("test-consumer-group");
        // consumer 连接 nameServer 的地址
        consumer.setNamesrvAddr(NAME_SERVER_ADDRESS);
        // 订阅一个主题，* 表示订阅这个主题的所有消息，后期会有消息过滤
        consumer.subscribe("testTopic", "*");
        // 写一个监听器（一直监听）
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            // 匿名内部类
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                            ConsumeConcurrentlyContext context) {

                System.out.println("「日志」我是消费者，第 0 条消息：" + new String(msgs.get(0).getBody()));
                System.out.println("「日志」消费上下文：" + context.toString());
                System.out.println(Thread.currentThread().getName() + "----" + msgs);

                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        // 启动消费者，这个 start一定要写在 registerMessageListener 下面
        consumer.start();
        // 挂起当前线程的 GVM（监听的线程不会挂起），目的是持续监听。
        System.in.read();
    }
}
