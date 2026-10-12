package com.example.quanlydetai.tools.sample;

import com.example.quanlydetai.entity.TaiKhoan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

/** Explicit command-line import tool. Never packaged into the web application. */
@EnableAutoConfiguration
@EntityScan(basePackageClasses = TaiKhoan.class)
@Import(SampleDataInitializer.class)
public class SampleDataImportApplication {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        var app = new SpringApplication(SampleDataImportApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setDefaultProperties(
                Map.of(
                        "spring.profiles.active", "dev",
                        "app.sample-data.enabled", "true",
                        "app.sample-data.password", "${SAMPLE_DATA_PASSWORD:}",
                        "app.sample-data.path",
                                "${SAMPLE_DATA_PATH:../Database/sample-data/fit-hcmute.json}",
                        "app.sample-data.refresh-existing", "${SAMPLE_DATA_REFRESH:false}"));
        try (var context = app.run(args)) {
            // ApplicationRunner completes its transaction before run returns; close the pool.
        }
    }
}
