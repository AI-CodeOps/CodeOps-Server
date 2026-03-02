package com.codeops.entity.enums;

/**
 * Defines the types of AI analysis agents available in the CodeOps QA platform.
 *
 * <p>Agents are organized into three tiers:</p>
 * <ul>
 *   <li><strong>Tier 1 — Core Workers:</strong> Always run on every audit.
 *       ({@link #SECURITY}, {@link #CODE_QUALITY}, {@link #BUILD_HEALTH}, {@link #COMPLETENESS})</li>
 *   <li><strong>Tier 2 — Conditional Workers:</strong> Spawned based on project type and configuration.
 *       ({@link #API_CONTRACT}, {@link #TEST_COVERAGE}, {@link #UI_UX}, {@link #DOCUMENTATION},
 *        {@link #DATABASE}, {@link #PERFORMANCE}, {@link #DEPENDENCY}, {@link #ARCHITECTURE})</li>
 *   <li><strong>Tier 3 — Adversarial Workers:</strong> Prove resilience beyond correctness.
 *       ({@link #CHAOS_MONKEY}, {@link #HOSTILE_USER}, {@link #COMPLIANCE_AUDITOR}, {@link #LOAD_SABOTEUR})</li>
 * </ul>
 */
public enum AgentType {

    // Tier 1: Core Workers (always run)
    SECURITY,
    CODE_QUALITY,
    BUILD_HEALTH,
    COMPLETENESS,

    // Tier 2: Conditional Workers (based on project type)
    API_CONTRACT,
    TEST_COVERAGE,
    UI_UX,
    DOCUMENTATION,
    DATABASE,
    PERFORMANCE,
    DEPENDENCY,
    ARCHITECTURE,

    // Tier 3: Adversarial Workers (prove resilience)
    CHAOS_MONKEY,
    HOSTILE_USER,
    COMPLIANCE_AUDITOR,
    LOAD_SABOTEUR
}
