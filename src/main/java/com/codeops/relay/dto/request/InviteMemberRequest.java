package com.codeops.relay.dto.request;

import com.codeops.relay.entity.enums.MemberRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Request to invite a user to a channel. Role defaults to MEMBER if null. */
public record InviteMemberRequest(
        @NotNull UUID userId,
        MemberRole role
) {}
