package com.sangui.helllo;


import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: sangui
 * @CreateTime: 2025-10-14
 * @Description: ProducerService
 * @Version: 1.0
 */
@Service
public class ProducerService {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void sendMessage(String topic, String msg) {
        SendResult sendResult = rocketMQTemplate.syncSend(topic + ":tagA", msg);  // 加 Tag 可选
        System.out.println("Send Result: " + sendResult);
    }
}
