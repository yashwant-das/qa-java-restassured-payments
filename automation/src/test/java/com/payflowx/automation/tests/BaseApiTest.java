package com.payflowx.automation.tests;

import com.payflowx.automation.clients.RestClientFactory;
import com.payflowx.automation.db.DatabaseManager;
import org.testng.annotations.BeforeSuite;

public abstract class BaseApiTest {
    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        RestClientFactory.configure();
        DatabaseManager.queryOne("SELECT 1 AS health_check");
    }
}
