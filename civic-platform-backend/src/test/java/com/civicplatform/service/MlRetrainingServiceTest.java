package com.civicplatform.service;

import com.civicplatform.entity.MlRetrainJobRun;
import com.civicplatform.repository.MlRetrainJobRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MlRetrainingServiceTest {

    @Mock private MlServiceClient mlServiceClient;
    @Mock private MlRetrainJobRunRepository repository;

    private MlRetrainingService service;

    @BeforeEach
    void setUp() {
        service = new MlRetrainingService(mlServiceClient, repository);
        ReflectionTestUtils.setField(service, "retrainEnabled", true);
    }

    @Test
    void triggerRetrain_persistsSuccessRun() {
        service.triggerRetrain("manual-test");

        ArgumentCaptor<MlRetrainJobRun> captor = ArgumentCaptor.forClass(MlRetrainJobRun.class);
        verify(repository).save(captor.capture());
        assertEquals("SUCCESS", captor.getValue().getStatus());
    }

    @Test
    void triggerRetrain_persistsFailedRunWhenClientThrows() {
        doThrow(new RuntimeException("ML offline")).when(mlServiceClient).triggerRetrain();

        service.triggerRetrain("manual-test");

        ArgumentCaptor<MlRetrainJobRun> captor = ArgumentCaptor.forClass(MlRetrainJobRun.class);
        verify(repository).save(captor.capture());
        assertEquals("FAILED", captor.getValue().getStatus());
    }
}
