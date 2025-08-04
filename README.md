# Kafka-Messaging-Sample
카프카를 이용한 클라이언트/서버 예제입니다. 



## 특징

* 단방향 메세지 전송 및 프로미스 Kafka Request-Reply 패턴 지원

* Spring Boot에 바로 적용 가능한 통신 인터페이스 제공

* DTO(Data Transfer Object) 클래스 관련 직렬화/역직렬화 지원

  

## 프로젝트 구조

* kafka-messaging-client / 클라이언트 - Spring Boot (포트 8080)
* kafka-messaging-server / 서버 - Spring Boot (포트 8081)
* kafka-messaging-common / 공통 모듈

Client는 오픈뱅킹 서버 , Server는 은행 서버로 예제가 구성되어 있으니 참고하시길 바랍니다.



## 사전 준비

1. 예제를 실습하기 위해서는 Docker를 이용한 kafka가 미리 실행되어 있어야합니다.

   프로젝트내 docker-compose-kafka.yml을 이용하여 카프카를 실행하세요.	

   ```shell
   docker compose -f docker-compose-kafka.yml up -d // 카프카 실행
   docker compose -f docker-compose-kafka.yml down // 카프카 중단
   ```

2. DB 연결 실패시 kafka-messaging-client와 kafka-messaging-server의 application.yml의 DB 설정(username,password)을 확인하세요.



## KafkaCommModule

* 카프카와 통신할수 있는 인터페이스 모듈입니다. 

* 클라이언트에서 서버에 메세지를 보내기 위해서는 서비스에 KafkaCommModule을 의존성 주입 후 사용합니다.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenBankingService {

    private final KafkaCommModule commModule;
    ...       
}
```

* 인터페이스

```java
/**
 * 카프카 통신 모듈
 * 프로미스, 단방향 메세지 전송을 지원합니다.
 */
public interface KafkaCommModule {
    /**
     * 프로미스 쿼리를 서버에 요청합니다.
     * 요청을 보내면 그에대한 응답 메세지를 받습니다. (타임아웃: 30초)
     *
     * @param cmd     통신 프로토콜
     * @param request DTO 객체
     * @return CommQueryPromise
     */
    public CommQueryPromise sendPromiseQuery(String cmd, Object request);

    /**
     * 프로미스 쿼리를 서버에 요청합니다.
     * 요청을 보내면 그에대한 응답 메세지를 받습니다.
     *
     * @param cmd     통신 프로토콜
     * @param request DTO 객체
     * @param timeout 타임아웃 (초)
     * @return CommQueryPromise
     */
    public CommQueryPromise sendPromiseQuery(String cmd, Object request, int timeout);

    /**
     * 프로미스 쿼리를 서버에 요청합니다.
     * 요청을 보내면 그에대한 응답 메세지를 받습니다.
     * 카프카 메세지 키를 추가로 입력 받습니다.
     *
     * @param key     카프카 메세지 키
     * @param cmd     통신 프로토콜
     * @param request DTO 객체
     * @param timeout 타임아웃 (초)
     * @return
     */
    public CommQueryPromise sendPromiseQuery(String key, String cmd, Object request, int timeout);

    /**
     * 단방향 메세지를 전송합니다.
     *
     * @param cmd     통신 프로토콜
     * @param request DTO 객체
     * @return Message
     */
    public Message sendMessage(String cmd, Object request);

    /**
     * 단방향 메세지를 전송합니다.
     * 전송결과를 비동기 콜백함수를 통해 확인할 수 있습니다.
     *
     * @param cmd      통신 프로토콜
     * @param request  DTO 객체
     * @param callback 콜백 함수
     * @return Message
     */
    public Message sendMessage(String cmd, Object request, BiConsumer<? super SendResult<String, Object>, ? super Throwable> callback);

    /**
     * 단방향 메세지를 전송합니다.
     * 전송결과를 비동기 콜백함수를 통해 확인할 수 있습니다.
     * 카프카 메세지 키를 추가로 입력 받습니다.
     *
     * @param key      카프카 메세지 키
     * @param cmd      통신 프로토콜
     * @param request  DTO 객체
     * @param callback 콜백 함수
     * @return
     */
    public Message sendMessage(String key, String cmd, Object request, BiConsumer<? super SendResult<String, Object>, ? super Throwable> callback);

    /**
     * ReplyingKafkaTemplate을 반환합니다.
     *
     * @return ReplyingKafkaTemplate
     */
    public ReplyingKafkaTemplate<String, Object, Object> getReplyingKafkaTemplate();
}
```



## 클라이언트/서버에서 사용할 KEY 정의

클라이언트와 서버에서 메세지를 주고 받을때 사용하는 공용키를 정의합니다. 

``` java
public class CommunicationProtocol {

    public static final String REQUEST_DEPOSIT = "kr.ssok.kafka.messaging.request.deposit";
    public static final String REQUEST_WITHDRAW = "kr.ssok.kafka.messaging.request.withdraw";
    public static final String SEND_TEST_MESSAGE = "kr.ssok.kafka.messaging.test.message";
    
}
```



## 메세지 전송 방식 (클라이언트에서 수행)

보내는쪽에서 메세지 전송 방식은 **2가지 방식**을 제공합니다. (프로미스 메세지, 단방향 메세지)

#### 1. 프로미스 (요청-응답)

전통적인 요청-응답 패턴입니다, 요청 메세지 전송후 응답 메세지를 받습니다.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaClientService {

    //KafkaCommModule 의존성 주입
    private final KafkaCommModule commModule;

    /**
     * KafkaCommModule의 sendPromiseQuery를 사용하는 방식
     * @param request
     * @return
     */
    public TransferResponse processTransfer(TransferRequest request) {
        try {

            // sendPromiseQuery 호출
            CommQueryPromise promise = this.commModule.sendPromiseQuery(CommunicationProtocol.REQUEST_DEPOSIT, request, 30);

            // CommQueryPromise의 Future로 PromiseMessage(응답 메세지)를 가져옵니다.
            // 요청에 대한 응답을 받기 전까지 해당 스레드를 대기합니다.
            PromiseMessage msg = promise.get();

            // PromiseMessage에서 응답받은 데이터를 가져옵니다.
            TransferResponse result = msg.getDataObject(TransferResponse.class);

            log.info("Received Promise response: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Error processing Promise", e);
            return TransferResponse.builder()
                    .status(TransferStatus.FAILED)
                    .build();
        }
    }
    
 	...   
        
}
```

#### 2. 단방향 메세지

클라이언트에서 서버에 단방향 메세지를 전송합니다.

```JAVA
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaClientService {

    //KafkaCommModule 의존성 주입
    private final KafkaCommModule commModule;
    
    /**
     * 단방향으로 메세지를 전송합니다.
     * @param message
     */
    public void sendUnidirectionalMessage(String message)
    {
        this.commModule.sendMessage(CommunicationProtocol.SEND_TEST_MESSAGE, (Object) message , (sendResult, throwable) -> {
            if (throwable != null) log.error("메시지 전송 실패: ", throwable);
            else log.info("메시지 전송 성공!");
        });
    }
    
    ...
    
}
```



## 메세지 응답 방식 (서버에서 수행)

받는쪽에서 메세지 응답 방식은 다음과 같습니다. 

#### 1. 프로미스

전통적인 요청-응답 패턴입니다, 요청 받은 메시지를 확인하고 카프카 리스너에서 응답을 반환합니다.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaServerService {
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
        
        switch (cmd) {
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
}
```

#### 2. 단방향 메세지

클라이언트에서 전달받은 메세지를 확인합니다.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaServerService {
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
}
```



## API TEST 

#### 1. 프로미스

```
POST http://localhost:8080/api/openbanking/transfer
```

* 요청 Body (json)

  ```json
  {
    "fromAccount": "098765432166",
    "toAccount": "09876543212",
    "amount": 1000,
    "currency": "KRW",
    "description": "테스트 송금2"
  }
  ```

* 응답 성공시

  ```json
  {
      "requestId": "7334cc10-b27d-4885-9165-2169559ea023",
      "transactionId": "0265cac9-1fb4-4db7-8e1a-552478bdcf1e",
      "status": "SUCCESS",
      "message": "Transfer completed successfully",
      "processedTime": "2025-04-30T15:15:50.1886822"
  }
  ```

  

#### 2. 단방향 메시지

```
POST http://localhost:8080/api/openbanking/send?msg=Hello
```

* 응답 성공시 => 200 OK

  
