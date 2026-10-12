package com.example.quanlydetai.tools.sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SampleDataProfileTest {
    @Test
    void neverRegistersTheImporterInProductionEvenWhenRequested() {
        new ApplicationContextRunner()
                .withUserConfiguration(SampleDataInitializer.class)
                .withPropertyValues("spring.profiles.active=prod", "app.sample-data.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(SampleDataInitializer.class));
    }

    @Test
    void importerIsDisabledByDefaultInDevelopment() {
        new ApplicationContextRunner()
                .withUserConfiguration(SampleDataInitializer.class)
                .withPropertyValues("spring.profiles.active=dev")
                .run(context -> assertThat(context).doesNotHaveBean(SampleDataInitializer.class));
    }
}
