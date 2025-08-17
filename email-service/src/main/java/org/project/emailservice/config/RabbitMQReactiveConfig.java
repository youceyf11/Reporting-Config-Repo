package org.project.emailservice.config;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.SenderOptions;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.RabbitFlux;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.AmqpAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration pour RabbitMQ réactif.
 * 
 * Cette configuration crée les beans Sender et Receiver nécessaires
 * pour une communication 100% non-bloquante avec RabbitMQ.
 * 
 * @author Votre nom
 * @since 1.0
 */
@Configuration
@Profile("!test")
@Slf4j
public class RabbitMQReactiveConfig {

    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String URGENT_PRIORITY_QUEUE_NAME = "email.queue.urgent";
    public static final String HIGH_PRIORITY_QUEUE_NAME = "email.queue.high";
    public static final String NORMAL_QUEUE_NAME = "email.queue.normal";
    public static final String LOW_PRIORITY_QUEUE_NAME = "email.queue.low";

    public static final String URGENT_PRIORITY_ROUTING_KEY = "email.urgent";
    public static final String HIGH_PRIORITY_ROUTING_KEY = "email.high";
    public static final String NORMAL_PRIORITY_ROUTING_KEY = "email.normal";
    public static final String LOW_PRIORITY_ROUTING_KEY = "email.low";

    public static final String DLX_NAME = "email.exchange.dlx";
    public static final String URGENT_DLQ = "email.queue.urgent.dlq";
    public static final String HIGH_DLQ = "email.queue.high.dlq";
    public static final String NORMAL_DLQ = "email.queue.normal.dlq";
    public static final String LOW_DLQ = "email.queue.low.dlq";

    @Value("${rabbitmq.routing.key.urgent}")
    private String urgentRoutingKey;

    @Value("${rabbitmq.routing.key.high}")
    private String highRoutingKey;

    @Value("${rabbitmq.routing.key.normal}")
    private String normalRoutingKey;

    @Value("${rabbitmq.routing.key.low}")
    private String lowRoutingKey;

    @Value("${spring.rabbitmq.host:localhost}")
    private String host;

    @Value("${spring.rabbitmq.port:5672}")
    private int port;

    @Value("${spring.rabbitmq.username:guest}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    private final AmqpAdmin amqpAdmin;

    public RabbitMQReactiveConfig(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    @Bean
    TopicExchange emailExchange() {
        return new TopicExchange(EMAIL_EXCHANGE);
    }

    @Bean
    Queue urgentPriorityQueue() {
        return QueueBuilder.durable(URGENT_PRIORITY_QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", URGENT_PRIORITY_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue highPriorityQueue() {
        return QueueBuilder.durable(HIGH_PRIORITY_QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", HIGH_PRIORITY_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue normalPriorityQueue() {
        return QueueBuilder.durable(NORMAL_QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", NORMAL_PRIORITY_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue lowPriorityQueue() {
        Map<String, Object> lowArgs = new HashMap<>();
        lowArgs.put("x-dead-letter-exchange", DLX_NAME);
        lowArgs.put("x-dead-letter-routing-key", LOW_PRIORITY_ROUTING_KEY);
        lowArgs.put("x-message-ttl", 5000);
        return QueueBuilder.durable(LOW_PRIORITY_QUEUE_NAME).withArguments(lowArgs).build();
    }

    @Bean
    Queue urgentDlq() {
        return QueueBuilder.durable(URGENT_DLQ).build();
    }

    @Bean
    Queue highDlq() {
        return QueueBuilder.durable(HIGH_DLQ).build();
    }

    @Bean
    Queue normalDlq() {
        return QueueBuilder.durable(NORMAL_DLQ).build();
    }

    @Bean
    Queue lowDlq() {
        return QueueBuilder.durable(LOW_DLQ).build();
    }

    /**
     * Déclare l'exchange DLX et les 4 DLQ correspondantes.
     */
    @Bean
    public Declarables dlxDeclarables() {
        TopicExchange dlx = ExchangeBuilder.topicExchange(DLX_NAME).durable(true).build();

        Queue urgentDlq = QueueBuilder.durable(URGENT_DLQ).build();
        Queue highDlq   = QueueBuilder.durable(HIGH_DLQ).build();
        Queue normalDlq = QueueBuilder.durable(NORMAL_DLQ).build();
        Queue lowDlq    = QueueBuilder.durable(LOW_DLQ).build();

        return new Declarables(
                dlx,
                urgentDlq, highDlq, normalDlq, lowDlq,
                BindingBuilder.bind(urgentDlq).to(dlx).with(URGENT_PRIORITY_ROUTING_KEY),
                BindingBuilder.bind(highDlq)  .to(dlx).with(HIGH_PRIORITY_ROUTING_KEY),
                BindingBuilder.bind(normalDlq).to(dlx).with(NORMAL_PRIORITY_ROUTING_KEY),
                BindingBuilder.bind(lowDlq)   .to(dlx).with(LOW_PRIORITY_ROUTING_KEY)
        );
    }

    @Bean
    Binding urgentPriorityBinding(Queue urgentPriorityQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(urgentPriorityQueue).to(emailExchange).with(urgentRoutingKey);
    }

    @Bean
    Binding highPriorityBinding(Queue highPriorityQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(highPriorityQueue).to(emailExchange).with(highRoutingKey);
    }

    @Bean
    Binding normalPriorityBinding(Queue normalPriorityQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(normalPriorityQueue).to(emailExchange).with(normalRoutingKey);
    }

    @Bean
    Binding lowPriorityBinding(Queue lowPriorityQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(lowPriorityQueue).to(emailExchange).with(lowRoutingKey);
    }

    /**
     * Crée une ConnectionFactory configurée pour RabbitMQ.
     * 
     * @return ConnectionFactory configurée avec les paramètres de connexion
     */
    private ConnectionFactory createConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        
        // Configuration pour la résilience
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(5000); // 5 secondes
        
        return factory;
    }

    /**
     * Bean Sender réactif pour publier des messages vers RabbitMQ.
     * 
     * @return Sender configuré
     */
    @Bean
    public Sender sender() {
        ConnectionFactory connectionFactory = createConnectionFactory();
        SenderOptions senderOptions = new SenderOptions()
            .connectionFactory(connectionFactory)
            .resourceManagementScheduler(reactor.core.scheduler.Schedulers.boundedElastic());
        return RabbitFlux.createSender(senderOptions);
    }

    /**
     * Bean Receiver réactif pour consommer des messages de RabbitMQ.
     * 
     * @return Receiver configuré
     */
    @Bean
    public Receiver receiver() {
        ConnectionFactory connectionFactory = createConnectionFactory();
        ReceiverOptions receiverOptions = new ReceiverOptions()
            .connectionFactory(connectionFactory)
            .connectionSubscriptionScheduler(reactor.core.scheduler.Schedulers.boundedElastic());
        return RabbitFlux.createReceiver(receiverOptions);
    }

    @PostConstruct
    public void ensureQueuesExist() {
        log.info("🔧 Ensuring email exchange and all queues exist...");
        
        try {
            // Declare exchange first
            amqpAdmin.declareExchange(new TopicExchange(EMAIL_EXCHANGE));
            TopicExchange emailExchange = new TopicExchange(EMAIL_EXCHANGE);
            log.info("✅ Email exchange ensured: {}", EMAIL_EXCHANGE);

            // Declare all priority queues
            amqpAdmin.declareQueue(new Queue(URGENT_PRIORITY_QUEUE_NAME, true));
            log.info("✅ URGENT queue ensured: {}", URGENT_PRIORITY_QUEUE_NAME);

            amqpAdmin.declareQueue(new Queue(HIGH_PRIORITY_QUEUE_NAME, true));
            log.info("✅ HIGH queue ensured: {}", HIGH_PRIORITY_QUEUE_NAME);

            amqpAdmin.declareQueue(new Queue(NORMAL_QUEUE_NAME, true));
            log.info("✅ NORMAL queue ensured: {}", NORMAL_QUEUE_NAME);

            // LOW priority queue with TTL (5 seconds) and DLX
            Map<String, Object> lowArgs = new HashMap<>();
            lowArgs.put("x-dead-letter-exchange", DLX_NAME);
            lowArgs.put("x-dead-letter-routing-key", LOW_PRIORITY_ROUTING_KEY);
            lowArgs.put("x-message-ttl", 5000);
            amqpAdmin.declareQueue(new Queue(LOW_PRIORITY_QUEUE_NAME, true, false, false, lowArgs));
            log.info("✅ LOW queue ensured: {}", LOW_PRIORITY_QUEUE_NAME);

            // Declare DLX and DLQ queues
            amqpAdmin.declareExchange(new TopicExchange(DLX_NAME));
            amqpAdmin.declareQueue(new Queue(URGENT_DLQ, true));
            amqpAdmin.declareQueue(new Queue(HIGH_DLQ, true));
            amqpAdmin.declareQueue(new Queue(NORMAL_DLQ, true));
            amqpAdmin.declareQueue(new Queue(LOW_DLQ, true));
            log.info("✅ DLX and DLQs ensured");

            // Bind priority queues to email exchange
            amqpAdmin.declareBinding(BindingBuilder.bind(new Queue(URGENT_PRIORITY_QUEUE_NAME, true)).to(emailExchange).with("email.urgent"));
            amqpAdmin.declareBinding(BindingBuilder.bind(new Queue(HIGH_PRIORITY_QUEUE_NAME, true)).to(emailExchange).with("email.high"));
            amqpAdmin.declareBinding(BindingBuilder.bind(new Queue(NORMAL_QUEUE_NAME, true)).to(emailExchange).with("email.normal"));
            amqpAdmin.declareBinding(BindingBuilder.bind(new Queue(LOW_PRIORITY_QUEUE_NAME, true)).to(emailExchange).with("email.low"));
            log.info("✅ Priority queues bound to exchange");

            // Bind DLQ queues to DLX so that dead-lettered messages are routed correctly
            amqpAdmin.declareBinding(BindingBuilder.bind(new Queue(URGENT_DLQ, true)).to(new TopicExchange(DLX_NAME)).with(URGENT_PRIORITY_ROUTING_KEY));
            amqpAdmin.declareBinding(BindingBuilder.bind(new Queue(HIGH_DLQ, true)).to(new TopicExchange(DLX_NAME)).with(HIGH_PRIORITY_ROUTING_KEY));
            amqpAdmin.declareBinding(BindingBuilder.bind(new Queue(NORMAL_DLQ, true)).to(new TopicExchange(DLX_NAME)).with(NORMAL_PRIORITY_ROUTING_KEY));
            amqpAdmin.declareBinding(BindingBuilder.bind(new Queue(LOW_DLQ, true)).to(new TopicExchange(DLX_NAME)).with(LOW_PRIORITY_ROUTING_KEY));

            log.info("🎯 Email exchange, queues, bindings and DLQs are ready for consumption");
            
        } catch (Exception e) {
            log.error("❌ Failed to ensure exchange/queues exist", e);
        }
    }
}