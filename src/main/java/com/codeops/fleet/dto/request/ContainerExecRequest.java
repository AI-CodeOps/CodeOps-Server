package com.codeops.fleet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to execute a command inside a running container. */
public record ContainerExecRequest(
        @NotBlank @Size(max = 2000) String command,
        boolean attachStdout,
        boolean attachStderr
) {}
