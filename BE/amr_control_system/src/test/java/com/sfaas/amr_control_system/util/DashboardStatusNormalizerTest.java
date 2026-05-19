package com.sfaas.amr_control_system.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DashboardStatusNormalizerTest {

    @Test
    void normalizeAmrQueryStatusAcceptsSupportedStatusValues() {
        assertEquals(
                DashboardStatusNormalizer.STATUS_ERROR,
                DashboardStatusNormalizer.normalizeAmrQueryStatus(" error ")
        );
        assertEquals(
                DashboardStatusNormalizer.STATUS_EMERGENCY_STOP,
                DashboardStatusNormalizer.normalizeAmrQueryStatus("EMERGENCY_STOP")
        );
    }

    @Test
    void normalizeAmrQueryStatusRejectsUnsupportedAliases() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DashboardStatusNormalizer.normalizeAmrQueryStatus("WAITING")
        );
    }

    @Test
    void normalizeAmrQueryStatusRejectsUnknownStatuses() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DashboardStatusNormalizer.normalizeAmrQueryStatus("FOO")
        );
    }
}
