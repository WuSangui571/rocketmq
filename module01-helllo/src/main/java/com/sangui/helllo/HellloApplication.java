package com.sangui.helllo;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HellloApplication {
    @Autowired
    private ProducerService producerService;
    public static void main(String[] args) {
        SpringApplication.run(HellloApplication.class, args);
    }
    @PostConstruct
    public void init() {
        // 启动后自动发送测试消息
        producerService.sendMessage("TestTopic", "Hello RocketMQ from Spring Boot!");
    }

}
