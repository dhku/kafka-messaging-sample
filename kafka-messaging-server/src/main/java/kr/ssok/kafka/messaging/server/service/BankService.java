package kr.ssok.kafka.messaging.server.service;

import kr.ssok.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankService {

    /**
     * 프로미스 요청에 대한 카프카 리스너
     * 요청한 내용을 확인후 응답을 반환합니다.
     * (kafkaListenerReplyContainerFactory 사용)
     *
     * @param record        레코드
     * @param replyTopic    응답을 보내는 토픽
     * @param correlationId 상관 ID
     * @param cmd           통신 프로토콜
     * @return
     */
    @KafkaListener(topics = "${spring.kafka.request-topic}", groupId = "request-server-group", containerFactory = "kafkaListenerReplyContainerFactory")
    @SendTo // 응답은 헤더에 지정된 replyTopic으로 전송됨
    public String handleTransferRequest(ConsumerRecord<String, String> record,
                                        @Header(KafkaHeaders.REPLY_TOPIC) String replyTopic,
                                        @Header(KafkaHeaders.CORRELATION_ID) String correlationId,
                                        @Header(value = "CMD", required = false) String cmd) {

        log.info("Received TransferRequest in bank service: {}", record.value());
        log.info("Received CMD: {}", cmd);
        log.info("Correlation ID: {}", correlationId);
        log.info("Reply topic: {}", replyTopic);

        // 실제 은행 송금 처리 로직 구현 (여기서는 간단히 시뮬레이션)
        // 레코드에서 record.value()를 DTO 타입으로 캐스팅하여 사용할 것
        TransferRequest request = JsonUtil.fromJson(record.value(), TransferRequest.class);
        TransferResponse response = processTransferInBank(request);

        if (cmd == null) return JsonUtil.toJson(response);

        switch (cmd)
        {
            case CommunicationProtocol.SEND_TEST_MESSAGE:
                log.info("Called SEND_TEST_MESSAGE!");
                break;
            case CommunicationProtocol.REQUEST_DEPOSIT:
                log.info("Called REQUEST_DEPOSIT!");
                break;
            case CommunicationProtocol.REQUEST_WITHDRAW:
                log.info("Called REQUEST_WITHDRAW!");
                break;
        }

        log.info("Transfer processed, sending response: {}", response);
        return JsonUtil.toJson(response);
    }

    /**
     * 단방향 메세지 요청에 대한 카프카 리스너
     * (kafkaListenerUnidirectionalContainerFactory 사용)
     *
     * @param cmd    통신 프로토콜
     * @param record 레코드
     */
    @KafkaListener(topics = "${spring.kafka.push-topic}", containerFactory = "kafkaListenerUnidirectionalContainerFactory")
    public void receiveMessage(@Header(value = "CMD", required = false) String cmd,
                               ConsumerRecord<String, String> record) {

        log.info("Received unidirectional message in bank service: {}", record.value());
        log.info("Received CMD: {}", cmd);

        if (cmd == null) return;
        switch (cmd) {
            // 로그 확인
            case CommunicationProtocol.SEND_TEST_MESSAGE:
                log.info("Called SEND_TEST_MESSAGE!");
                break;
            case CommunicationProtocol.REQUEST_DEPOSIT:
                log.info("Called REQUEST_DEPOSIT!");
                break;
            case CommunicationProtocol.REQUEST_WITHDRAW:
                log.info("Called REQUEST_WITHDRAW!");
                break;
        }
    }

    // 실제 은행 시스템에서의 송금 처리 로직 (시뮬레이션)
    private TransferResponse processTransferInBank(TransferRequest request) {
        try {
            // 실제로는 여기서 은행 핵심 시스템과의 통합 로직이 들어갈 것임
            // 계좌 유효성 검증, 잔액 확인, 송금 처리 등
            // 송금 처리
            return TransferResponse.builder()
                    .requestId(request.getRequestId())
                    .transactionId(UUID.randomUUID().toString())
                    .status(TransferStatus.SUCCESS)
                    .message("Transfer completed successfully")
                    .processedTime(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error processing transfer in bank", e);
            return TransferResponse.builder()
                    .requestId(request.getRequestId())
                    .status(TransferStatus.FAILED)
                    .message("Bank system error: " + e.getMessage())
                    .processedTime(LocalDateTime.now())
                    .build();
        }
    }

    private TransferResponse processTransferInBank2(TransferRequest request) {
        return this.processTransferInBank(request);
    }

}
