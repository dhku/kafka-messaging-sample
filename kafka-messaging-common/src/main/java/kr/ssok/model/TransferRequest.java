package kr.ssok.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    private String requestId;  // 요청 고유 ID (멱등성 보장을 위함)
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String description;
    private LocalDateTime requestTime;
}
