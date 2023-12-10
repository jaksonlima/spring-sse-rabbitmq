package com.br.spring.springserversendevent;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableRabbit
@SpringBootApplication
public class SpringServerSendEventApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringServerSendEventApplication.class, args);
    }

}
