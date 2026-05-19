package com.sfaas.amr_control_system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfaas.amr_control_system.repository.AmrCommandRepository;
import com.sfaas.amr_control_system.repository.AmrRepository;
import com.sfaas.amr_control_system.repository.AmrStatusLogRepository;
import com.sfaas.amr_control_system.repository.AmrTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AmrServiceTest {

    @Test
    void listAmrsRejectsUnsupportedStatusQueryValue() {
        AmrRepository amrRepository = mock(AmrRepository.class);
        AmrStatusLogRepository amrStatusLogRepository = mock(AmrStatusLogRepository.class);
        AmrTaskRepository amrTaskRepository = mock(AmrTaskRepository.class);
        AmrCommandRepository amrCommandRepository = mock(AmrCommandRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
        AmrService amrService = new AmrService(
                amrRepository,
                amrStatusLogRepository,
                amrTaskRepository,
                amrCommandRepository,
                objectMapper,
                applicationEventPublisher
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> amrService.listAmrs(null, null, "FOO", null, null, null, null)
        );

        assertTrue(exception.getMessage().contains("Unsupported status value: 'FOO'"));
        verifyNoInteractions(amrRepository, amrStatusLogRepository, amrTaskRepository, amrCommandRepository);
    }
}
