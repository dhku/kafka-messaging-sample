package kr.ssok.kafka.messaging.client.service;


import kr.ssok.kafka.messaging.client.comm.KafkaCommModule;
import kr.ssok.kafka.messaging.client.comm.promise.CommQueryPromise;
import kr.ssok.kafka.messaging.client.comm.promise.PromiseMessage;
import kr.ssok.model.CommunicationProtocol;
import kr.ssok.model.TransferRequest;
import kr.ssok.model.TransferResponse;
import kr.ssok.model.TransferStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenBankingService {

    private final KafkaCommModule commModule;

    @Value("${spring.kafka.request-topic}")
    private String requestTopic;

    @Value("${spring.kafka.push-topic}")
    private String pushTopic;

    /**
     * 1. KafkaCommModule의 sendPromiseQuery를 사용하는 방식
     *
     * @param request
     * @return
     */
    public TransferResponse processTransfer_1(TransferRequest request) {
        try {

            // 요청 ID 생성 (없는 경우)
            if (request.getRequestId() == null) {
                request.setRequestId(UUID.randomUUID().toString());
            }

            // 요청 시간 설정
            request.setRequestTime(LocalDateTime.now());

            // sendPromiseQuery 호출
            CommQueryPromise promise = this.commModule.sendPromiseQuery(CommunicationProtocol.REQUEST_DEPOSIT, request, 30);

            // Future로 응답 메세지를 가져옴
            PromiseMessage msg = promise.get();

            // 응답 데이터 사용
            TransferResponse result = msg.getDataObject(TransferResponse.class);

            log.info("Received Promise response: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Error processing Promise", e);
            return TransferResponse.builder()
                    .requestId(request.getRequestId())
                    .status(TransferStatus.FAILED)
                    .message("Failed to Promise: " + e.getMessage())
                    .processedTime(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * 2. KafkaCommModule을 사용하지 않는 방식 (legacy)
     *
     * @param request
     * @return
     */
    public TransferResponse processTransfer_2(TransferRequest request) {
        try {

            // 요청 ID 생성 (없는 경우)
            if (request.getRequestId() == null) {
                request.setRequestId(UUID.randomUUID().toString());
            }

            String cmd = CommunicationProtocol.REQUEST_DEPOSIT;

            // 요청 시간 설정
            request.setRequestTime(LocalDateTime.now());

            // Kafka를 통해 은행에 송금 요청 전송
            ProducerRecord<String, Object> record =
                    new ProducerRecord<>(requestTopic, request.getRequestId(), request);
            record.headers().add("CMD", cmd.getBytes(StandardCharsets.UTF_8));

            log.info("Sending transfer request: {}", request);

            // RequestReplyFuture를 사용하여 응답 대기
            RequestReplyFuture<String, Object, Object> future =
                    this.commModule.getReplyingKafkaTemplate().sendAndReceive(record, Duration.ofSeconds(10));

            // 응답 대기 및 처리
            ConsumerRecord<String, Object> response = future.get();
            TransferResponse result = (TransferResponse) response.value();

            log.info("Received transfer response: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Error processing transfer request", e);
            return TransferResponse.builder()
                    .requestId(request.getRequestId())
                    .status(TransferStatus.FAILED)
                    .message("Failed to process transfer: " + e.getMessage())
                    .processedTime(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * 단방향으로 메세지를 전송합니다.
     *
     * @param message
     */
    public void sendUnidirectionalMessage(String message) {
        this.commModule.sendMessage(CommunicationProtocol.SEND_TEST_MESSAGE, (Object) message, (sendResult, throwable) -> {
            if (throwable != null) {
                log.error("메시지 전송 실패: ", throwable);
            } else {
                log.info("메시지 전송 성공!");
            }
        });
    }

}

