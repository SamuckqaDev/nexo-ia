package com.nexoia.permission.service;

import static com.nexoia.permission.model.CapabilityDecision.ALLOWED;
import static com.nexoia.permission.model.CapabilityDecision.DENIED;
import static com.nexoia.permission.model.CapabilityDecision.REQUIRES_APPROVAL;
import static com.nexoia.permission.model.CapabilityFamily.EXTERNAL_READ;
import static com.nexoia.permission.model.CapabilityFamily.EXTERNAL_WRITE;
import static com.nexoia.permission.model.CapabilityFamily.INFERENCE;
import static com.nexoia.permission.model.CapabilityFamily.KNOWLEDGE_READ;
import static com.nexoia.permission.model.CapabilityFamily.KNOWLEDGE_WRITE;
import static com.nexoia.permission.model.CapabilityFamily.PERSONAL_MEMORY;
import static com.nexoia.permission.model.CapabilityFamily.PLANNING;
import static com.nexoia.permission.model.CapabilityFamily.SECRETS;
import static com.nexoia.permission.model.CapabilityFamily.SYSTEM_CONTROL;
import static com.nexoia.permission.model.CapabilityFamily.WORKSPACE_READ;
import static com.nexoia.permission.model.CapabilityFamily.WORKSPACE_WRITE;
import static org.assertj.core.api.Assertions.assertThat;

import com.nexoia.permission.dto.ResolvedPermissions;
import com.nexoia.permission.model.BuiltInProfiles;
import com.nexoia.permission.model.CapabilityFamily;
import com.nexoia.permission.model.ContentStance;
import com.nexoia.permission.model.UnlockLevel;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Exhaustively pins the deterministic resolution rules: ceilings, target gating, the model-capability
 * clamp, hard prohibitions, and the independence of the content axis from capability decisions.
 */
class PermissionEngineTest {

    private static final Set<CapabilityFamily> ALL_TARGETS = EnumSet.of(
            KNOWLEDGE_WRITE, EXTERNAL_READ, EXTERNAL_WRITE, WORKSPACE_READ, WORKSPACE_WRITE);
    private static final Set<CapabilityFamily> NO_TARGETS = EnumSet.noneOf(CapabilityFamily.class);

    private final PermissionEngine engine = new PermissionEngine();

    @Test
    void readerInChatGroundsOnOwnDataAndBlocksEverythingExternal() {
        ResolvedPermissions resolved = engine.resolve(
                BuiltInProfiles.reader(), UnlockLevel.L1_GROUNDED, true, ALL_TARGETS, ContentStance.STANDARD);

        assertThat(resolved.effectiveLevel()).isEqualTo(UnlockLevel.L1_GROUNDED);
        assertThat(resolved.decision(INFERENCE)).isEqualTo(ALLOWED);
        assertThat(resolved.decision(KNOWLEDGE_READ)).isEqualTo(ALLOWED);
        assertThat(resolved.decision(KNOWLEDGE_WRITE)).isEqualTo(ALLOWED);
        assertThat(resolved.decision(PERSONAL_MEMORY)).isEqualTo(ALLOWED);
        assertThat(resolved.decision(PLANNING)).isEqualTo(DENIED);
        assertThat(resolved.decision(EXTERNAL_READ)).isEqualTo(DENIED);
    }

    @Test
    void researcherInAgentUnlocksPlanningAndExternalReadOnly() {
        ResolvedPermissions resolved = engine.resolve(
                BuiltInProfiles.researcher(), UnlockLevel.L5_OPERATOR, true, ALL_TARGETS, ContentStance.STANDARD);

        assertThat(resolved.effectiveLevel()).isEqualTo(UnlockLevel.L2_CONNECTED);
        assertThat(resolved.decision(PLANNING)).isEqualTo(ALLOWED);
        assertThat(resolved.decision(EXTERNAL_READ)).isEqualTo(ALLOWED);
        assertThat(resolved.decision(EXTERNAL_WRITE)).isEqualTo(DENIED);
        assertThat(resolved.decision(WORKSPACE_READ)).isEqualTo(DENIED);
        assertThat(resolved.decision(SYSTEM_CONTROL)).isEqualTo(DENIED);
    }

    @Test
    void lockedProfileLeavesOnlyInference() {
        ResolvedPermissions resolved = engine.resolve(
                BuiltInProfiles.locked(), UnlockLevel.L5_OPERATOR, true, ALL_TARGETS, ContentStance.STANDARD);

        assertThat(resolved.effectiveLevel()).isEqualTo(UnlockLevel.L0_OBSERVER);
        assertThat(resolved.attachable()).containsExactly(INFERENCE);
    }

    @Test
    void builderGatesWritesBehindApprovalButNotSystemControl() {
        ResolvedPermissions resolved = engine.resolve(
                BuiltInProfiles.builder(), UnlockLevel.L5_OPERATOR, true, ALL_TARGETS, ContentStance.STANDARD);

        assertThat(resolved.effectiveLevel()).isEqualTo(UnlockLevel.L4_BUILDER);
        assertThat(resolved.decision(WORKSPACE_READ)).isEqualTo(ALLOWED);
        assertThat(resolved.decision(EXTERNAL_WRITE)).isEqualTo(REQUIRES_APPROVAL);
        assertThat(resolved.decision(WORKSPACE_WRITE)).isEqualTo(REQUIRES_APPROVAL);
        assertThat(resolved.decision(SYSTEM_CONTROL)).isEqualTo(DENIED);
    }

    @Test
    void operatorGatesSystemControlBehindApproval() {
        ResolvedPermissions resolved = engine.resolve(
                BuiltInProfiles.operator(), UnlockLevel.L5_OPERATOR, true, ALL_TARGETS, ContentStance.STANDARD);

        assertThat(resolved.effectiveLevel()).isEqualTo(UnlockLevel.L5_OPERATOR);
        assertThat(resolved.decision(SYSTEM_CONTROL)).isEqualTo(REQUIRES_APPROVAL);
    }

    @Test
    void automationCannotEscalateSoApprovalFamiliesBecomeDenied() {
        ResolvedPermissions resolved = engine.resolve(
                BuiltInProfiles.automation(UnlockLevel.L5_OPERATOR), UnlockLevel.L5_OPERATOR, true,
                ALL_TARGETS, ContentStance.STANDARD);

        assertThat(resolved.decision(EXTERNAL_READ)).isEqualTo(ALLOWED);
        assertThat(resolved.decision(EXTERNAL_WRITE)).isEqualTo(DENIED);
        assertThat(resolved.decision(WORKSPACE_WRITE)).isEqualTo(DENIED);
        assertThat(resolved.decision(SYSTEM_CONTROL)).isEqualTo(DENIED);
    }

    @Test
    void aModelWithoutToolCallingIsClampedToGrounded() {
        ResolvedPermissions resolved = engine.resolve(
                BuiltInProfiles.researcher(), UnlockLevel.L5_OPERATOR, false, ALL_TARGETS, ContentStance.STANDARD);

        assertThat(resolved.effectiveLevel()).isEqualTo(UnlockLevel.L1_GROUNDED);
        assertThat(resolved.decision(KNOWLEDGE_READ)).isEqualTo(ALLOWED);
        assertThat(resolved.decision(PLANNING)).isEqualTo(DENIED);
        assertThat(resolved.decision(EXTERNAL_READ)).isEqualTo(DENIED);
    }

    @Test
    void knowledgeWriteIsDeniedWithoutAWritableVaultTarget() {
        ResolvedPermissions resolved = engine.resolve(
                BuiltInProfiles.reader(), UnlockLevel.L1_GROUNDED, true, NO_TARGETS, ContentStance.STANDARD);

        assertThat(resolved.decision(KNOWLEDGE_READ)).isEqualTo(ALLOWED);
        assertThat(resolved.decision(KNOWLEDGE_WRITE)).isEqualTo(DENIED);
    }

    @Test
    void secretsAreNeverExposedEvenToTheOperator() {
        ResolvedPermissions resolved = engine.resolve(
                BuiltInProfiles.operator(), UnlockLevel.L5_OPERATOR, true, ALL_TARGETS, ContentStance.STANDARD);

        assertThat(resolved.decision(SECRETS)).isEqualTo(DENIED);
    }

    @Test
    void contentStanceIsPassedThroughAndNeverChangesCapabilityDecisions() {
        ResolvedPermissions standard = engine.resolve(
                BuiltInProfiles.researcher(), UnlockLevel.L5_OPERATOR, true, ALL_TARGETS, ContentStance.STANDARD);
        ResolvedPermissions restricted = engine.resolve(
                BuiltInProfiles.researcher(), UnlockLevel.L5_OPERATOR, true, ALL_TARGETS, ContentStance.RESTRICTED);

        assertThat(standard.contentStance()).isEqualTo(ContentStance.STANDARD);
        assertThat(restricted.contentStance()).isEqualTo(ContentStance.RESTRICTED);
        assertThat(restricted.decisions()).isEqualTo(standard.decisions());
    }
}
