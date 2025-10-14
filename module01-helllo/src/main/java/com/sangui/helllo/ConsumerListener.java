package com.sangui.helllo;


import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Author: sangui
 * @CreateTime: 2025-10-14
 * @Description: ConsumerListener
 * @Version: 1.0
 */
@Component
@RocketMQMessageListener(topic = "TestTopic", consumerGroup = "consumer-group")  // 匹配 Topic 和组
public class ConsumerListener implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        System.out.println("Received Message: " + message);
    }
}
