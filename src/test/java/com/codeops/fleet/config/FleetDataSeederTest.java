package com.codeops.fleet.config;

import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.fleet.entity.ServiceProfile;
import com.codeops.fleet.entity.SolutionProfile;
import com.codeops.fleet.entity.SolutionService;
import com.codeops.fleet.entity.WorkstationProfile;
import com.codeops.fleet.entity.WorkstationSolution;
import com.codeops.fleet.entity.enums.RestartPolicy;
import com.codeops.fleet.repository.*;
import com.codeops.registry.entity.ServiceRegistration;
import com.codeops.registry.entity.enums.ServiceType;
import com.codeops.registry.repository.ServiceRegistrationRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FleetDataSeeder}.
 *
 * <p>Covers idempotency checks, service profile generation from Registry,
 * default solution and workstation creation, and edge cases.</p>
 */
@ExtendWith(MockitoExtension.class)
class FleetDataSeederTest {

    @Mock ServiceProfileRepository serviceProfileRepository;
    @Mock SolutionProfileRepository solutionProfileRepository;
    @Mock SolutionServiceRepository solutionServiceRepository;
    @Mock WorkstationProfileRepository workstationProfileRepository;
    @Mock WorkstationSolutionRepository workstationSolutionRepository;
    @Mock PortMappingRepository portMappingRepository;
    @Mock ServiceRegistrationRepository serviceRegistrationRepository;
    @Mock UserRepository userRepository;
    @Mock TeamRepository teamRepository;

    @InjectMocks FleetDataSeeder seeder;

    private Team team;
    private User adam;

    @BeforeEach
    void setUp() {
        team = new Team();
        team.setId(UUID.randomUUID());
        team.setName("Test Team");

        adam = new User();
        adam.setId(UUID.randomUUID());
        adam.setEmail("adam@allard.com");
    }

    @Nested
    class Run {

        @Test
        void skipsWhenTeamNotAvailable() throws Exception {
            when(userRepository.findByEmail("adam@allard.com")).thenReturn(Optional.of(adam));
            when(teamRepository.findAll()).thenReturn(List.of());

            seeder.run();

            verifyNoInteractions(serviceProfileRepository);
        }

        @Test
        void skipsWhenUserNotAvailable() throws Exception {
            when(userRepository.findByEmail("adam@allard.com")).thenReturn(Optional.empty());
            when(teamRepository.findAll()).thenReturn(List.of(team));

            seeder.run();

            verifyNoInteractions(serviceProfileRepository);
        }

        @Test
        void skipsWhenAlreadySeeded() throws Exception {
            when(userRepository.findByEmail("adam@allard.com")).thenReturn(Optional.of(adam));
            when(teamRepository.findAll()).thenReturn(List.of(team));
            when(serviceProfileRepository.findByTeamId(team.getId()))
                    .thenReturn(List.of(mock(ServiceProfile.class)));

            seeder.run();

            verify(serviceProfileRepository, never()).save(any());
        }

        @Test
        void seedsFullDataFromRegistrations() throws Exception {
            when(userRepository.findByEmail("adam@allard.com")).thenReturn(Optional.of(adam));
            when(teamRepository.findAll()).thenReturn(List.of(team));
            when(serviceProfileRepository.findByTeamId(team.getId())).thenReturn(List.of());

            var reg1 = buildRegistration("backend-api", ServiceType.SPRING_BOOT_API);
            var reg2 = buildRegistration("frontend", ServiceType.REACT_SPA);
            when(serviceRegistrationRepository.findByTeamId(team.getId())).thenReturn(List.of(reg1, reg2));

            when(serviceProfileRepository.save(any(ServiceProfile.class))).thenAnswer(inv -> {
                ServiceProfile p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
            when(solutionProfileRepository.save(any(SolutionProfile.class))).thenAnswer(inv -> {
                SolutionProfile p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
            when(workstationProfileRepository.save(any(WorkstationProfile.class))).thenAnswer(inv -> {
                WorkstationProfile p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            seeder.run();

            // Verify service profiles created
            var profileCaptor = ArgumentCaptor.forClass(ServiceProfile.class);
            verify(serviceProfileRepository, times(2)).save(profileCaptor.capture());
            List<ServiceProfile> profiles = profileCaptor.getAllValues();
            assertThat(profiles.get(0).getServiceName()).isEqualTo("backend-api");
            assertThat(profiles.get(0).getImageName()).isEqualTo("eclipse-temurin");
            assertThat(profiles.get(0).isAutoGenerated()).isTrue();
            assertThat(profiles.get(1).getServiceName()).isEqualTo("frontend");
            assertThat(profiles.get(1).getImageName()).isEqualTo("nginx");

            // Verify port mappings created (both have default ports)
            verify(portMappingRepository, times(2)).save(any());

            // Verify solution profile created
            var solutionCaptor = ArgumentCaptor.forClass(SolutionProfile.class);
            verify(solutionProfileRepository).save(solutionCaptor.capture());
            assertThat(solutionCaptor.getValue().getName()).isEqualTo("Default Solution");
            assertThat(solutionCaptor.getValue().isDefault()).isTrue();

            // Verify solution services linked
            verify(solutionServiceRepository, times(2)).save(any(SolutionService.class));

            // Verify workstation profile created
            var workstationCaptor = ArgumentCaptor.forClass(WorkstationProfile.class);
            verify(workstationProfileRepository).save(workstationCaptor.capture());
            assertThat(workstationCaptor.getValue().getName()).isEqualTo("Default Workstation");
            assertThat(workstationCaptor.getValue().isDefault()).isTrue();
            assertThat(workstationCaptor.getValue().getUser()).isEqualTo(adam);

            // Verify workstation solution linked
            verify(workstationSolutionRepository).save(any(WorkstationSolution.class));
        }

        @Test
        void seedsWithNoRegistrations() throws Exception {
            when(userRepository.findByEmail("adam@allard.com")).thenReturn(Optional.of(adam));
            when(teamRepository.findAll()).thenReturn(List.of(team));
            when(serviceProfileRepository.findByTeamId(team.getId())).thenReturn(List.of());
            when(serviceRegistrationRepository.findByTeamId(team.getId())).thenReturn(List.of());

            when(solutionProfileRepository.save(any(SolutionProfile.class))).thenAnswer(inv -> {
                SolutionProfile p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
            when(workstationProfileRepository.save(any(WorkstationProfile.class))).thenAnswer(inv -> {
                WorkstationProfile p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            seeder.run();

            // No service profiles created
            verify(serviceProfileRepository, never()).save(any());

            // Solution and workstation still created (empty solution)
            verify(solutionProfileRepository).save(any(SolutionProfile.class));
            verify(workstationProfileRepository).save(any(WorkstationProfile.class));
        }
    }

    @Nested
    class ImageMapping {

        @Test
        void mapsDatabaseService() throws Exception {
            when(userRepository.findByEmail("adam@allard.com")).thenReturn(Optional.of(adam));
            when(teamRepository.findAll()).thenReturn(List.of(team));
            when(serviceProfileRepository.findByTeamId(team.getId())).thenReturn(List.of());

            var reg = buildRegistration("my-db", ServiceType.DATABASE_SERVICE);
            when(serviceRegistrationRepository.findByTeamId(team.getId())).thenReturn(List.of(reg));

            when(serviceProfileRepository.save(any(ServiceProfile.class))).thenAnswer(inv -> {
                ServiceProfile p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
            when(solutionProfileRepository.save(any(SolutionProfile.class))).thenAnswer(inv -> {
                SolutionProfile p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
            when(workstationProfileRepository.save(any(WorkstationProfile.class))).thenAnswer(inv -> {
                WorkstationProfile p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            seeder.run();

            var captor = ArgumentCaptor.forClass(ServiceProfile.class);
            verify(serviceProfileRepository).save(captor.capture());
            assertThat(captor.getValue().getImageName()).isEqualTo("postgres");
            assertThat(captor.getValue().getImageTag()).isEqualTo("16");
            assertThat(captor.getValue().getRestartPolicy()).isEqualTo(RestartPolicy.UNLESS_STOPPED);
        }
    }

    private ServiceRegistration buildRegistration(String name, ServiceType type) {
        return ServiceRegistration.builder()
                .name(name)
                .slug(name)
                .serviceType(type)
                .teamId(team.getId())
                .build();
    }
}
