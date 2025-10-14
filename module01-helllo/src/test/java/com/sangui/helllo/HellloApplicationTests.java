package com.sangui.helllo;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;


@SpringBootTest
class HellloApplicationTests {

    @Test
    void TestProducer() throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("test-group");
        producer.setNamesrvAddr("localhost:9876");  // Docker 主机地址
        producer.start();
        Message msg = new Message("TestTopic", "Hello RocketMQ".getBytes());
        producer.send(msg);
        System.out.println("Sent!");
        producer.shutdown();
    }

}
