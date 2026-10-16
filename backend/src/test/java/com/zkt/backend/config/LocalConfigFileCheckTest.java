package com.zkt.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalConfigFileCheckTest {
    @Test void failsWithInstructionsWhenTheDefaultLocalProfileHasNoConfigFile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setDefaultProfiles("local");
        assertThatThrownBy(() -> LocalConfigFileCheck.check(environment, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("application-local.yml.example");
    }

    @Test void passesWhenTheFileExists() {
        MockEnvironment environment = new MockEnvironment();
        environment.setDefaultProfiles("local");
        assertThatCode(() -> LocalConfigFileCheck.check(environment, true)).doesNotThrowAnyException();
    }

    @Test void ignoresOtherProfiles() {
        for (String profile : new String[]{"h2", "test", "prod"}) {
            MockEnvironment environment = new MockEnvironment();
            environment.setActiveProfiles(profile);
            assertThatCode(() -> LocalConfigFileCheck.check(environment, false)).doesNotThrowAnyException();
        }
    }
}
