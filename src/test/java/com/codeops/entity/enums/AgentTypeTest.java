package com.codeops.entity.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the {@link AgentType} enum contains all expected values
 * across all three tiers (Core, Conditional, Adversarial).
 */
class AgentTypeTest {

    @Test
    void agentType_has16Values() {
        assertEquals(16, AgentType.values().length,
                "AgentType enum should have exactly 16 values (12 standard + 4 adversarial)");
    }

    @Test
    void agentType_includesAllTier1CoreWorkers() {
        Set<String> names = Arrays.stream(AgentType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertTrue(names.contains("SECURITY"), "Missing SECURITY");
        assertTrue(names.contains("CODE_QUALITY"), "Missing CODE_QUALITY");
        assertTrue(names.contains("BUILD_HEALTH"), "Missing BUILD_HEALTH");
        assertTrue(names.contains("COMPLETENESS"), "Missing COMPLETENESS");
    }

    @Test
    void agentType_includesAllTier2ConditionalWorkers() {
        Set<String> names = Arrays.stream(AgentType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertTrue(names.contains("API_CONTRACT"), "Missing API_CONTRACT");
        assertTrue(names.contains("TEST_COVERAGE"), "Missing TEST_COVERAGE");
        assertTrue(names.contains("UI_UX"), "Missing UI_UX");
        assertTrue(names.contains("DOCUMENTATION"), "Missing DOCUMENTATION");
        assertTrue(names.contains("DATABASE"), "Missing DATABASE");
        assertTrue(names.contains("PERFORMANCE"), "Missing PERFORMANCE");
        assertTrue(names.contains("DEPENDENCY"), "Missing DEPENDENCY");
        assertTrue(names.contains("ARCHITECTURE"), "Missing ARCHITECTURE");
    }

    @Test
    void agentType_includesAllTier3AdversarialWorkers() {
        Set<String> names = Arrays.stream(AgentType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertTrue(names.contains("CHAOS_MONKEY"), "Missing CHAOS_MONKEY");
        assertTrue(names.contains("HOSTILE_USER"), "Missing HOSTILE_USER");
        assertTrue(names.contains("COMPLIANCE_AUDITOR"), "Missing COMPLIANCE_AUDITOR");
        assertTrue(names.contains("LOAD_SABOTEUR"), "Missing LOAD_SABOTEUR");
    }

    @Test
    void agentType_valueOfAdversarialTypes() {
        assertEquals(AgentType.CHAOS_MONKEY, AgentType.valueOf("CHAOS_MONKEY"));
        assertEquals(AgentType.HOSTILE_USER, AgentType.valueOf("HOSTILE_USER"));
        assertEquals(AgentType.COMPLIANCE_AUDITOR, AgentType.valueOf("COMPLIANCE_AUDITOR"));
        assertEquals(AgentType.LOAD_SABOTEUR, AgentType.valueOf("LOAD_SABOTEUR"));
    }
}
