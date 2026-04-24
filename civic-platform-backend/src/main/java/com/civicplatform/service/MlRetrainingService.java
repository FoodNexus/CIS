package com.civicplatform.service;

import com.civicplatform.dto.response.MlRetrainRunResponse;
import com.civicplatform.entity.MlRetrainJobRun;
import com.civicplatform.repository.MlRetrainJobRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates scheduled and manual retraining triggers for the ML service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MlRetrainingService {

    private final MlServiceClient mlServiceClient;
    private final MlRetrainJobRunRepository retrainJobRunRepository;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${app.ml.retrain.enabled:true}")
    private boolean retrainEnabled;

    /**
     * Executes retraining on a fixed interval when enabled.
     */
    @Scheduled(
            initialDelayString = "${app.ml.retrain.initial-delay-ms:60000}",
            fixedDelayString = "${app.ml.retrain.interval-ms:21600000}"
    )
    public void runScheduledRetrain() {
        if (!retrainEnabled) {
            return;
        }
        triggerRetrain("scheduled");
    }

    /**
     * Triggers retraining if a previous run is not currently in progress.
     */
    @Transactional
    public void triggerRetrain(String source) {
        if (!running.compareAndSet(false, true)) {
            log.info("Skipping ML retrain trigger ({}) because another run is active", source);
            return;
        }
        LocalDateTime started = LocalDateTime.now();
        String status = "SUCCESS";
        String message = "Retrain trigger sent to ML service";
        try {
            mlServiceClient.triggerRetrain();
            log.info("ML retrain trigger completed ({})", source);
        } catch (Exception ex) {
            status = "FAILED";
            message = ex.getMessage();
            log.error("ML retrain trigger failed ({}): {}", source, ex.getMessage(), ex);
        } finally {
            retrainJobRunRepository.save(MlRetrainJobRun.builder()
                    .startedAt(started)
                    .finishedAt(LocalDateTime.now())
                    .status(status)
                    .message(source + ": " + message)
                    .build());
            running.set(false);
        }
    }

    /**
     * Returns paginated retrain run history.
     */
    @Transactional(readOnly = true)
    public Page<MlRetrainRunResponse> listRuns(Pageable pageable) {
        return retrainJobRunRepository.findAllByOrderByStartedAtDesc(pageable)
                .map(row -> MlRetrainRunResponse.builder()
                        .id(row.getId())
                        .startedAt(row.getStartedAt())
                        .finishedAt(row.getFinishedAt())
                        .status(row.getStatus())
                        .message(row.getMessage())
                        .build());
    }
}
