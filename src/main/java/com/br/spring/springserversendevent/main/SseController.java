package com.br.spring.springserversendevent.main;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;

@RestController
public class SseController {

    private final SseRabbitMQSubscriber sseRabbitMQSubscriber;
    private final ApplicationContext context;

    public SseController(
            final SseRabbitMQSubscriber sseRabbitMQSubscriber,
            final ApplicationContext context
    ) {
        this.sseRabbitMQSubscriber = Objects.requireNonNull(sseRabbitMQSubscriber);
        this.context = Objects.requireNonNull(context);
    }

    @GetMapping(path = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return sseRabbitMQSubscriber.subscribe();
    }

    @PostMapping(path = "/published")
    public ResponseEntity<?> publishedMessageQueue(@RequestBody byte[] message) {
        sseRabbitMQSubscriber.publishMessage(message);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/shutdown")
    public ResponseEntity<?> shutdownContext() {
        ((ConfigurableApplicationContext) context).close();

        return ResponseEntity.noContent().build();
    }

}

