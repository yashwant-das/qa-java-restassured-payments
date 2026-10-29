package com.qaframework.payments.automation;

import com.qaframework.payments.automation.db.Database;
import io.restassured.RestAssured;
import org.testng.annotations.BeforeSuite;

public abstract class BaseApiTest {
    @BeforeSuite(alwaysRun = true)
    public void checkEnvironment() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        Database.queryOne("SELECT 1 AS ok");
    }
}
