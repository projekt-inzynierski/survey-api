package com.survey.api.integration;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MSSQLServerContainer;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("Test")
public class IntegrationTestDatabaseInitializer implements BeforeAllCallback {
    @SuppressWarnings("rawtypes")
    public static MSSQLServerContainer mssqlServerContainer = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2019-latest");

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        mssqlServerContainer.start();
        System.setProperty("spring.datasource.url", mssqlServerContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", mssqlServerContainer.getUsername());
        System.setProperty("spring.datasource.password", mssqlServerContainer.getPassword());
    }
}
