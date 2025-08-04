package kr.ssok.kafka.messaging.client.comm;

import jakarta.annotation.PostConstruct;
import kr.ssok.kafka.messaging.client.comm.promise.CommQueryPromise;
import kr.ssok.model.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaCommModuleImpl implements KafkaCommModule {
    private final ReplyingKafkaTemplate<String, Object, Object> replyingKafkaTemplate;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.request-topic}")
    private String requestTopic;

    @Value("${spring.kafka.push-topic}")
    private String pushTopic;

    @PostConstruct
    public void init() {
        replyingKafkaTemplate.start();
    }

    @Override
    public CommQueryPromise sendPromiseQuery(String cmd, Object request) {
        return this.sendPromiseQuery(cmd, request, 30);
    }

    @Override
    public CommQueryPromise sendPromiseQuery(String cmd, Object request, int timeout) {
        String msgKey = UUID.randomUUID().toString();
        return this.sendPromiseQuery(msgKey, cmd, request, timeout);
    }

    @Override
    public CommQueryPromise sendPromiseQuery(String key, String cmd, Object request, int timeout) {
        ProducerRecord<String, Object> record =
                new ProducerRecord<>(requestTopic, key , request instanceof String ? request : JsonUtil.toJson(request));
        record.headers().add("CMD", cmd.getBytes(StandardCharsets.UTF_8));

        log.info("Sending Promise Request: {}", request);

        RequestReplyFuture<String, Object, Object> future =
                this.replyingKafkaTemplate.sendAndReceive(record, Duration.ofSeconds(timeout));

        return new CommQueryPromise(future);
    }

    @Override
    public Message sendMessage(String cmd, Object request) {
        return this.sendMessage(cmd, request, null);
    }

    @Override
    public Message sendMessage(String cmd, Object request, BiConsumer<? super SendResult<String, Object>, ? super Throwable> callback) {
        String msgKey = UUID.randomUUID().toString();
        return this.sendMessage(msgKey, cmd, request, callback);
    }

    @Override
    public Message sendMessage(String key, String cmd, Object request, BiConsumer<? super SendResult<String, Object>, ? super Throwable> callback) {
        ProducerRecord<String, Object> record =
                new ProducerRecord<>(pushTopic, key, request instanceof String ? request : JsonUtil.toJson(request));
        record.headers().add("CMD", cmd.getBytes(StandardCharsets.UTF_8));

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);

        return new Message(future, callback);
    }

    @Override
    public ReplyingKafkaTemplate<String, Object, Object> getReplyingKafkaTemplate() {
        return replyingKafkaTemplate;
    }

}
