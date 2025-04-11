import org.TestifyFW.framework.BaseTest;
import org.TestifyFW.framework.ActionHandler;
import org.TestifyFW.model.TestCase;
import org.TestifyFW.model.TestStep;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;

public class SampleTest extends BaseTest {

    @Test(description = "Test login functionality")
    public void loginTester() {
        // Create test case programmatically
        TestCase testCase = new TestCase();
        testCase.setName("Login Test");

        List<TestStep> steps = new ArrayList<>();

        // Step 1: Open the website
        TestStep step1 = new TestStep();
        step1.setAction("open url");
        step1.setValue("https://www.saucedemo.com");
        step1.setCritical(true); // This is critical - if it fails, other steps won't execute
        steps.add(step1);

        // Step 2: Enter username
        TestStep step2 = new TestStep();
        step2.setAction("input text");
        step2.setSelectorType("id");
        step2.setSelector("user-name");
        step2.setValue("standard_user");
        steps.add(step2);

       /* // Step 3: Enter password
        TestStep step3 = new TestStep();
        step3.setAction("input text");
        step3.setSelectorType("id");
        step3.setSelector("pasword");
        step3.setValue("secret_sauce");
        steps.add(step3);

        // Step 4: Click login button
        TestStep step4 = new TestStep();
        step4.setAction("click");
        step4.setSelectorType("id");
        step4.setSelector("login-button");
        step4.setCritical(true); // Critical step
        steps.add(step4);

        // Step 5: Verify successful login
        TestStep step5 = new TestStep();
        step5.setAction("verify_text");
        step5.setSelectorType("class");
        step5.setSelector("app_logo");
        step5.setExpected("Swag Labs");
        steps.add(step5);*/

        testCase.setSteps(steps);

        // Execute the test case
        ActionHandler handler = new ActionHandler(driver);
        handler.executeTestCase(testCase);
    }
/*
    @Test(description = "Test with dependencies", dependsOnMethods = {"loginTester"})
    public void profileTest() {
        TestCase testCase = new TestCase();
        testCase.setName("Profile Test");

        List<TestStep> steps = new ArrayList<>();

        // Test steps for profile functionality
        TestStep step1 = new TestStep();
        step1.setAction("click");
        step1.setSelectorType("id");
        step1.setSelector("profileLink");
        steps.add(step1);

        TestStep step2 = new TestStep();
        step2.setAction("verify_text");
        step2.setSelectorType("id");
        step2.setSelector("profileHeader");
        step2.setExpected("User Profile");
        steps.add(step2);

        testCase.setSteps(steps);

        ActionHandler handler = new ActionHandler(driver);
        handler.executeTestCase(testCase);
    }*/
}