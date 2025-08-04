package kr.ssok.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    private String requestId;  // 원본 요청 ID
    private String transactionId;  // 은행에서 생성한 거래 ID
    private TransferStatus status;
    private String message;
    private LocalDateTime processedTime;
}
