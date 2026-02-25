package com.codeops.relay.dto.request;

import com.codeops.relay.entity.enums.MemberRole;
import jakarta.validation.constraints.NotNull;

/** Request to change a channel member's role. */
public record UpdateMemberRoleRequest(
        @NotNull MemberRole role
) {}
