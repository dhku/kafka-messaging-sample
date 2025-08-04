package kr.ssok.kafka.messaging.client.controller;

import kr.ssok.kafka.messaging.client.service.OpenBankingService;
import kr.ssok.model.TransferRequest;
import kr.ssok.model.TransferResponse;
import kr.ssok.model.TransferStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/openbanking")
@RequiredArgsConstructor
public class OpenBankingController {

    private final OpenBankingService openBankingService;

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transferMoney(@RequestBody TransferRequest request)
    {
        TransferResponse response = openBankingService.processTransfer_1(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send")
    public void transferMoney(@RequestParam String msg)
    {
        openBankingService.sendUnidirectionalMessage(msg);
    }

}
