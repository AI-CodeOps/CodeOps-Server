package com.codeops.fleet.service;

import com.codeops.config.AppConstants;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.fleet.dto.mapper.NetworkConfigMapper;
import com.codeops.fleet.dto.mapper.ServiceProfileMapper;
import com.codeops.fleet.dto.mapper.VolumeMountMapper;
import com.codeops.fleet.dto.request.CreateServiceProfileRequest;
import com.codeops.fleet.dto.request.UpdateServiceProfileRequest;
import com.codeops.fleet.dto.response.NetworkConfigResponse;
import com.codeops.fleet.dto.response.ServiceProfileDetailResponse;
import com.codeops.fleet.dto.response.ServiceProfileResponse;
import com.codeops.fleet.dto.response.VolumeMountResponse;
import com.codeops.fleet.entity.PortMapping;
import com.codeops.fleet.entity.ServiceProfile;
import com.codeops.fleet.entity.enums.RestartPolicy;
import com.codeops.fleet.repository.NetworkConfigRepository;
import com.codeops.fleet.repository.PortMappingRepository;
import com.codeops.fleet.repository.ServiceProfileRepository;
import com.codeops.fleet.repository.VolumeMountRepository;
import com.codeops.registry.entity.ServiceRegistration;
import com.codeops.registry.entity.enums.ServiceType;
import com.codeops.registry.repository.ServiceRegistrationRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for managing Docker service profiles.
 *
 * <p>Provides full CRUD operations for service profiles and automatic profile generation
 * from Registry service registrations. Auto-generated profiles map a service type to a
 * default Docker image and port, allowing one-click containerization of registered services.</p>
 *
 * @see ServiceProfile
 * @see ContainerManagementService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceProfileService {

    private final ServiceProfileRepository serviceProfileRepository;
    private final PortMappingRepository portMappingRepository;
    private final VolumeMountRepository volumeMountRepository;
    private final NetworkConfigRepository networkConfigRepository;
    private final ServiceProfileMapper serviceProfileMapper;
    private final VolumeMountMapper volumeMountMapper;
    private final NetworkConfigMapper networkConfigMapper;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final ServiceRegistrationRepository serviceRegistrationRepository;
    private final AuditLogService auditLogService;

    // ═══════════════════════════════════════════════════════════════════
    //  CRUD Operations
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Creates a new service profile for a team.
     *
     * @param teamId  the team ID
     * @param request the create service profile request
     * @return the created service profile detail response
     * @throws AuthorizationException if the user is not a team member
     * @throws ValidationException    if a profile with the same name exists or the limit is reached
     */
    @Transactional
    public ServiceProfileDetailResponse createServiceProfile(UUID teamId, CreateServiceProfileRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        if (serviceProfileRepository.existsByTeamIdAndServiceName(teamId, request.serviceName())) {
            throw new ValidationException("Service profile already exists with name: " + request.serviceName());
        }

        long count = serviceProfileRepository.findByTeamId(teamId).size();
        if (count >= AppConstants.FLEET_MAX_SERVICE_PROFILES) {
            throw new ValidationException("Team has reached the maximum number of service profiles: "
                    + AppConstants.FLEET_MAX_SERVICE_PROFILES);
        }

        ServiceProfile profile = serviceProfileMapper.toEntity(request);
        profile.setTeam(teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team", teamId)));

        if (request.serviceRegistrationId() != null) {
            ServiceRegistration registration = serviceRegistrationRepository.findById(request.serviceRegistrationId())
                    .orElseThrow(() -> new NotFoundException("ServiceRegistration", request.serviceRegistrationId()));
            profile.setServiceRegistration(registration);
        }

        profile = serviceProfileRepository.save(profile);
        log.info("Created service profile '{}' for team {}", profile.getServiceName(), teamId);

        auditLogService.log(userId, teamId, "CREATE_SERVICE_PROFILE", "ServiceProfile",
                profile.getId(), "Created service profile " + profile.getServiceName());

        return buildDetailResponse(profile);
    }

    /**
     * Updates an existing service profile.
     *
     * @param teamId    the team ID
     * @param profileId the service profile ID
     * @param request   the update request with fields to modify
     * @return the updated service profile detail response
     * @throws NotFoundException      if the profile is not found
     * @throws AuthorizationException if the user is not a team member or the profile belongs to another team
     */
    @Transactional
    public ServiceProfileDetailResponse updateServiceProfile(UUID teamId, UUID profileId,
                                                              UpdateServiceProfileRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        ServiceProfile profile = findProfileForTeam(profileId, teamId);

        if (request.displayName() != null) {
            profile.setDisplayName(request.displayName());
        }
        if (request.description() != null) {
            profile.setDescription(request.description());
        }
        if (request.imageName() != null) {
            profile.setImageName(request.imageName());
        }
        if (request.imageTag() != null) {
            profile.setImageTag(request.imageTag());
        }
        if (request.command() != null) {
            profile.setCommand(request.command());
        }
        if (request.workingDir() != null) {
            profile.setWorkingDir(request.workingDir());
        }
        if (request.envVarsJson() != null) {
            // envVarsJson is stored via child entities, but kept on profile for quick reference
        }
        if (request.portsJson() != null) {
            // portsJson is stored via child entities, but kept on profile for quick reference
        }
        if (request.healthCheckCommand() != null) {
            profile.setHealthCheckCommand(request.healthCheckCommand());
        }
        if (request.healthCheckIntervalSeconds() != null) {
            profile.setHealthCheckIntervalSeconds(request.healthCheckIntervalSeconds());
        }
        if (request.healthCheckTimeoutSeconds() != null) {
            profile.setHealthCheckTimeoutSeconds(request.healthCheckTimeoutSeconds());
        }
        if (request.healthCheckRetries() != null) {
            profile.setHealthCheckRetries(request.healthCheckRetries());
        }
        if (request.restartPolicy() != null) {
            profile.setRestartPolicy(request.restartPolicy());
        }
        if (request.memoryLimitMb() != null) {
            profile.setMemoryLimitMb(request.memoryLimitMb());
        }
        if (request.cpuLimit() != null) {
            profile.setCpuLimit(request.cpuLimit());
        }
        if (request.isEnabled() != null) {
            profile.setEnabled(request.isEnabled());
        }
        if (request.startOrder() != null) {
            profile.setStartOrder(request.startOrder());
        }

        profile = serviceProfileRepository.save(profile);
        log.info("Updated service profile '{}' for team {}", profile.getServiceName(), teamId);

        auditLogService.log(userId, teamId, "UPDATE_SERVICE_PROFILE", "ServiceProfile",
                profile.getId(), "Updated service profile " + profile.getServiceName());

        return buildDetailResponse(profile);
    }

    /**
     * Retrieves a service profile by ID.
     *
     * @param teamId    the team ID
     * @param profileId the service profile ID
     * @return the service profile detail response
     * @throws NotFoundException      if the profile is not found
     * @throws AuthorizationException if the user is not a team member or the profile belongs to another team
     */
    @Transactional(readOnly = true)
    public ServiceProfileDetailResponse getServiceProfile(UUID teamId, UUID profileId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        ServiceProfile profile = findProfileForTeam(profileId, teamId);
        return buildDetailResponse(profile);
    }

    /**
     * Lists all service profiles for a team.
     *
     * @param teamId the team ID
     * @return list of service profile summary responses
     * @throws AuthorizationException if the user is not a team member
     */
    @Transactional(readOnly = true)
    public List<ServiceProfileResponse> listServiceProfiles(UUID teamId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        List<ServiceProfile> profiles = serviceProfileRepository.findByTeamId(teamId);
        return serviceProfileMapper.toResponseList(profiles);
    }

    /**
     * Deletes a service profile.
     *
     * @param teamId    the team ID
     * @param profileId the service profile ID
     * @throws NotFoundException      if the profile is not found
     * @throws AuthorizationException if the user is not a team member or the profile belongs to another team
     */
    @Transactional
    public void deleteServiceProfile(UUID teamId, UUID profileId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        ServiceProfile profile = findProfileForTeam(profileId, teamId);
        String profileName = profile.getServiceName();

        serviceProfileRepository.delete(profile);
        log.info("Deleted service profile '{}' from team {}", profileName, teamId);

        auditLogService.log(userId, teamId, "DELETE_SERVICE_PROFILE", "ServiceProfile",
                profileId, "Deleted service profile " + profileName);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Registry Auto-Generation
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Auto-generates a service profile from a Registry service registration.
     *
     * <p>Maps the registration's service type to a default Docker image and port.
     * This operation is idempotent — if a profile already exists for the registration,
     * the existing profile is returned without modification.</p>
     *
     * @param teamId                the team ID
     * @param serviceRegistrationId the Registry service registration ID
     * @return the auto-generated (or existing) service profile detail response
     * @throws NotFoundException      if the service registration is not found
     * @throws AuthorizationException if the user is not a team member
     * @throws ValidationException    if the profile limit is reached
     */
    @Transactional
    public ServiceProfileDetailResponse autoGenerateFromRegistry(UUID teamId, UUID serviceRegistrationId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyTeamAccess(teamId, userId);

        // Idempotent: return existing profile if already generated
        var existing = serviceProfileRepository.findByServiceRegistrationId(serviceRegistrationId);
        if (existing.isPresent()) {
            log.debug("Profile already exists for registration {}, returning existing", serviceRegistrationId);
            return buildDetailResponse(existing.get());
        }

        ServiceRegistration registration = serviceRegistrationRepository.findById(serviceRegistrationId)
                .orElseThrow(() -> new NotFoundException("ServiceRegistration", serviceRegistrationId));

        long count = serviceProfileRepository.findByTeamId(teamId).size();
        if (count >= AppConstants.FLEET_MAX_SERVICE_PROFILES) {
            throw new ValidationException("Team has reached the maximum number of service profiles: "
                    + AppConstants.FLEET_MAX_SERVICE_PROFILES);
        }

        // Map service type to default image and port
        ImageMapping mapping = getImageMapping(registration.getServiceType());

        ServiceProfile profile = ServiceProfile.builder()
                .serviceName(registration.getName())
                .displayName(registration.getName())
                .description("Auto-generated from Registry: " + registration.getName())
                .imageName(mapping.imageName())
                .imageTag(mapping.imageTag())
                .restartPolicy(RestartPolicy.UNLESS_STOPPED)
                .isAutoGenerated(true)
                .isEnabled(true)
                .startOrder(0)
                .serviceRegistration(registration)
                .team(teamRepository.findById(teamId)
                        .orElseThrow(() -> new NotFoundException("Team", teamId)))
                .build();

        profile = serviceProfileRepository.save(profile);

        // Create default port mapping if applicable
        if (mapping.defaultPort() != null) {
            PortMapping portMapping = PortMapping.builder()
                    .hostPort(mapping.defaultPort())
                    .containerPort(mapping.defaultPort())
                    .protocol("tcp")
                    .serviceProfile(profile)
                    .build();
            portMappingRepository.save(portMapping);
        }

        log.info("Auto-generated service profile '{}' from registration {} for team {}",
                profile.getServiceName(), serviceRegistrationId, teamId);

        auditLogService.log(userId, teamId, "AUTO_GENERATE_SERVICE_PROFILE", "ServiceProfile",
                profile.getId(), "Auto-generated profile from Registry: " + registration.getName());

        return buildDetailResponse(profile);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Private Helpers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Verifies that the user is a member of the specified team.
     *
     * @param teamId the team ID
     * @param userId the user ID
     * @throws AuthorizationException if the user is not a team member
     */
    private void verifyTeamAccess(UUID teamId, UUID userId) {
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new AuthorizationException("Not a member of this team"));
    }

    /**
     * Finds a service profile and verifies it belongs to the specified team.
     *
     * @param profileId the service profile ID
     * @param teamId    the team ID
     * @return the service profile
     * @throws NotFoundException      if the profile is not found
     * @throws AuthorizationException if the profile belongs to another team
     */
    private ServiceProfile findProfileForTeam(UUID profileId, UUID teamId) {
        ServiceProfile profile = serviceProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("ServiceProfile", profileId));
        if (!profile.getTeam().getId().equals(teamId)) {
            throw new AuthorizationException("Service profile does not belong to this team");
        }
        return profile;
    }

    /**
     * Builds a full detail response for a service profile including volumes and networks.
     *
     * @param profile the service profile entity
     * @return the detail response DTO
     */
    private ServiceProfileDetailResponse buildDetailResponse(ServiceProfile profile) {
        List<VolumeMountResponse> volumes = volumeMountMapper.toResponseList(
                volumeMountRepository.findByServiceProfileId(profile.getId()));
        List<NetworkConfigResponse> networks = networkConfigMapper.toResponseList(
                networkConfigRepository.findByServiceProfileId(profile.getId()));
        return serviceProfileMapper.toDetailResponse(profile, volumes, networks);
    }

    /**
     * Maps a Registry service type to the default Docker image and port for auto-generation.
     *
     * @param serviceType the Registry service type
     * @return the image mapping with image name, tag, and default port
     */
    private ImageMapping getImageMapping(ServiceType serviceType) {
        return switch (serviceType) {
            case SPRING_BOOT_API -> new ImageMapping("eclipse-temurin", "21-jre", 8080);
            case FLUTTER_WEB -> new ImageMapping("nginx", "alpine", 80);
            case REACT_SPA -> new ImageMapping("nginx", "alpine", 3000);
            case EXPRESS_API -> new ImageMapping("node", "20-alpine", 3000);
            case FASTAPI -> new ImageMapping("python", "3.12-slim", 8000);
            case DOTNET_API -> new ImageMapping("mcr.microsoft.com/dotnet/aspnet", "8.0", 5000);
            case GO_API -> new ImageMapping("golang", "1.22-alpine", 8080);
            case DATABASE_SERVICE -> new ImageMapping("postgres", "16", 5432);
            case MESSAGE_BROKER -> new ImageMapping("confluentinc/cp-kafka", "7.5.0", 9092);
            case CACHE_SERVICE -> new ImageMapping("redis", "7-alpine", 6379);
            default -> new ImageMapping("alpine", "latest", null);
        };
    }

    /**
     * Record holding the default Docker image mapping for a service type.
     *
     * @param imageName   the Docker image name
     * @param imageTag    the Docker image tag
     * @param defaultPort the default port for the service, or null if none
     */
    private record ImageMapping(String imageName, String imageTag, Integer defaultPort) {}
}
