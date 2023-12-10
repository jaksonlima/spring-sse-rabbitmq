package com.br.spring.springserversendevent.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseRabbitMQSubscriber {

    private static final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String queue = "sse.queue.%s".formatted(UUID.randomUUID());
    private static final String exchange = "sse.exchange";
    private static final String sseName = "sse.stream";

    private final AmqpTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;

    public SseRabbitMQSubscriber(
            final AmqpTemplate rabbitTemplate,
            final AmqpAdmin amqpAdmin
    ) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate);
        this.amqpAdmin = Objects.requireNonNull(amqpAdmin);
    }

    @PreDestroy
    public void onDestroy() {
        amqpAdmin.deleteQueue(queue);
    }

    @Bean
    public Queue queue() {
        return new Queue(queue, false);
    }

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(exchange);
    }

    @Bean
    public Binding binding(final Queue queue, final FanoutExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange);
    }

    @Bean
    public MessageListenerAdapter adapter() {
        return new MessageListenerAdapter(new RabbitListenerCustomer(), "handleEvent");
    }

    @Bean
    public SimpleMessageListenerContainer listener(
            final ConnectionFactory factory,
            final MessageListenerAdapter adapter
    ) {
        final var listener = new SimpleMessageListenerContainer();
        listener.setConnectionFactory(factory);
        listener.setMessageListener(adapter);
        listener.setQueueNames(queue);
        return listener;
    }


    static class RabbitListenerCustomer {
        public void handleEvent(final byte[] message) {
            try {
                final var converted = objectMapper.readValue(message, Map.class);

                for (final var emitter : emitters) {
                    try {
                        final var data = SseEmitter.event()
                                .id(UUID.randomUUID().toString())
                                .name(sseName)
                                .data(converted);

                        emitter.send(data);

                    } catch (Exception e) {
                        emitter.complete();

                        emitters.remove(emitter);

                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void publishMessage(final byte[] message) {
        rabbitTemplate.convertAndSend(exchange, "", message);
    }

    public SseEmitter subscribe() {
        final var emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        return emitter;
    }

}
