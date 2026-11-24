package com.sor999.dalmoi.record.service;

import com.sor999.dalmoi.record.repository.RecordJobRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "dalmoi.record.worker-enabled", havingValue = "true", matchIfMissing = true)
public class RecordJobWorker {

    private final RecordJobRepository recordJobRepository;

    private final RecordProcessingService recordProcessingService;

    private final RecordJobFailureService recordJobFailureService;

    private final Clock clock;

    @Scheduled(fixedDelay = 1000)
    public void run() {
        for (Long id : recordJobRepository.findReadyIds(clock.instant().minusSeconds(5), PageRequest.of(0, 20))) {
            try {
                recordProcessingService.process(id);
            } catch (RuntimeException exception) {
                log.error("기록 정산 실패: 참가자 {}", id, exception);
                try {
                    recordJobFailureService.fail(id);
                } catch (RuntimeException failure) {
                    log.error("정산 실패 상태 저장 실패: 참가자 {}", id, failure);
                }
            }
        }
    }
}
