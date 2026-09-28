package com.apilens.application.service;

import com.apilens.api.dto.CreateApiRequest;
import com.apilens.domain.exception.ResourceNotFoundException;
import com.apilens.domain.model.Api;
import com.apilens.domain.model.User;
import com.apilens.domain.repository.ApiRepository;
import com.apilens.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiManagementServiceTest {

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private UserRepository userRepository;

    private ApiManagementService service;
    private User systemUser;

    @BeforeEach
    void setUp() {
        service = new ApiManagementService(apiRepository, userRepository);
        systemUser = mock(User.class);
        when(systemUser.getId()).thenReturn(User.SYSTEM_USER_ID);
    }

    @Test
    void createApiPersistsWithSystemUserAsOwnerAndTrimsBlankOptionalUrls() {
        when(userRepository.findById(User.SYSTEM_USER_ID)).thenReturn(Optional.of(systemUser));
        when(apiRepository.save(any(Api.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateApiRequest request = new CreateApiRequest("GitHub API", "https://api.github.com", "  ", null);

        Api result = service.createApi(request);

        assertThat(result.getName()).isEqualTo("GitHub API");
        assertThat(result.getBaseUrl()).isEqualTo("https://api.github.com");
        assertThat(result.getOpenApiUrl()).isNull();
        assertThat(result.getOwner().getId()).isEqualTo(User.SYSTEM_USER_ID);
        verify(apiRepository).save(any(Api.class));
    }

    @Test
    void getApiThrowsWhenNotFound() {
        when(userRepository.findById(User.SYSTEM_USER_ID)).thenReturn(Optional.of(systemUser));
        UUID missingId = UUID.randomUUID();
        when(apiRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getApi(missingId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getApiThrowsWhenOwnedBySomeoneElse() {
        when(userRepository.findById(User.SYSTEM_USER_ID)).thenReturn(Optional.of(systemUser));

        User otherOwner = mock(User.class);
        when(otherOwner.getId()).thenReturn(UUID.randomUUID());
        Api api = new Api(UUID.randomUUID(), otherOwner, "Other", "https://example.com", null, null);
        when(apiRepository.findById(api.getId())).thenReturn(Optional.of(api));

        assertThatThrownBy(() -> service.getApi(api.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
