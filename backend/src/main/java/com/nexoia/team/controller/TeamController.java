package com.nexoia.team.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.knowledge.vault.dto.VaultResponse;
import com.nexoia.shared.api.BaseResponse;
import com.nexoia.team.dto.AddTeamMemberRequest;
import com.nexoia.team.dto.CreateTeamRequest;
import com.nexoia.team.dto.CreateTeamVaultRequest;
import com.nexoia.team.dto.TeamMemberResponse;
import com.nexoia.team.dto.TeamResponse;
import com.nexoia.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService service;

    @GetMapping
    @Operation(summary = "List the Teams the authenticated user belongs to")
    public ResponseEntity<BaseResponse<TeamResponse>> list(
            @AuthenticationPrincipal NexoUserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(200, "Teams retrieved",
                service.listMyTeams(principal.userId())));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @Operation(summary = "Create a Team owned by the authenticated administrator")
    public ResponseEntity<BaseResponse<TeamResponse>> create(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Team created", service.createTeam(principal, request)));
    }

    @GetMapping("/{teamId}/members")
    @Operation(summary = "List the members of a Team the caller administers")
    public ResponseEntity<BaseResponse<TeamMemberResponse>> members(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID teamId) {
        return ResponseEntity.ok(BaseResponse.success(200, "Team members retrieved",
                service.listMembers(principal, teamId)));
    }

    @PostMapping("/{teamId}/members")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @Operation(summary = "Add a user to a Team with a bounded capability profile")
    public ResponseEntity<BaseResponse<TeamMemberResponse>> addMember(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID teamId,
            @Valid @RequestBody AddTeamMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Team member added", service.addMember(principal, teamId, request)));
    }

    @PostMapping("/{teamId}/vaults")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @Operation(summary = "Create a shared Knowledge Vault owned by a Team the caller administers")
    public ResponseEntity<BaseResponse<VaultResponse>> createVault(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID teamId,
            @Valid @RequestBody CreateTeamVaultRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Team Vault created", service.createVault(principal, teamId, request)));
    }
}
