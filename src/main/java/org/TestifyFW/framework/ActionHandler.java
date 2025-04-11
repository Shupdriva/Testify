package org.TestifyFW.framework;

import org.TestifyFW.GUI.TestifyGUI;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.TestifyFW.model.TestCase;
import org.TestifyFW.model.TestStep;
import org.TestifyFW.reporter.TestReporter;
import org.testng.Assert;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import org.TestifyFW.jira.JiraIssueReporter;

import javax.swing.*;

public class ActionHandler {
    private WebDriver driver;
    private WebDriverWait wait;

    public ActionHandler(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        TestReporter.setCurrentDriver(driver);
    }

    public void executeTestCase(TestCase testCase) {
        TestReporter.initializeReporter();
        TestReporter.setCurrentTestName(testCase.getName());
        System.out.println("Executing: " + testCase.getName());
        boolean testFailed = false;
        Exception failureException = null;
        AssertionError failureAException = null;

        try {
            for (int i = 0; i < testCase.getSteps().size(); i++) {
                TestStep step = testCase.getSteps().get(i);
                String stepName = "Step " + (i + 1) + ": " + step.getAction();

                try {
                    TestReporter.logInfo(stepName, "Starting execution");
                    executeStep(step);
                    TestReporter.logStep(stepName, step.getAction(), true);
                }
                catch (Exception e) {
                    testFailed = true;
                    failureException = e;
                    TestReporter.logStep(stepName, step.getAction(), false);
                    TestReporter.fail("Step failed: " + e.getMessage());
                    JOptionPane.showMessageDialog(new TestifyGUI(),
                            "A step has failed. Check the console for results.",
                            "Step Failed", JOptionPane.INFORMATION_MESSAGE);

                    // If this step is critical, break execution
                    if (step.isCritical()) {
                        TestReporter.logInfo("Critical Step Failed", "Stopping test execution");
                        break;
                    }

                }
                catch (AssertionError ae) {
                    testFailed = true;
                    failureAException = ae;
                    TestReporter.logStep(stepName, step.getAction(), false);
                    TestReporter.fail("Assertion failed: " + ae.getMessage());
                    JOptionPane.showMessageDialog(new TestifyGUI(),
                            "An assertion has failed. Check the console for results.",
                            "Assertion Failed", JOptionPane.INFORMATION_MESSAGE);

                    // If this step is critical, break execution
                    if (step.isCritical()) {
                        TestReporter.logInfo("Critical Step Failed", "Stopping test execution");
                        break;
                    }
                }
            }
        } finally {
            // Generate the report regardless of success or failure
            TestReporter.generateReport();

            // Report to JIRA if test failed
            if (testFailed) {
                reportToJira(testCase, failureException);

                // The assertAll will throw an AssertionError if any steps failed
                TestReporter.assertAll(); // This will throw an exception
            }
        }
    }

    private void reportToJira(TestCase testCase, Exception failureException) {
        try {
            // Create a JIRA reporter
            JiraIssueReporter jiraReporter = new JiraIssueReporter();

            // Only proceed if JIRA integration is enabled
            if (jiraReporter.isEnabled()) {
                // Create a summary for the issue
                String summary = "Test Failure: " + testCase.getName();

                // Build a detailed description
                StringBuilder description = new StringBuilder();
                description.append("*Test Name:* ").append(testCase.getName()).append("\n\n");
                description.append("*Failure Message:* ").append(failureException != null ?
                        failureException.getMessage() : "Test failed").append("\n\n");

                // Add test steps
                description.append("*Test Steps:*\n");
                List<String> steps = TestReporter.getStepLog();
                for (String step : steps) {
                    description.append("# ").append(step).append("\n");
                }

                // Add stack trace if available
                if (failureException != null) {
                    description.append("\n*Stack Trace:*\n{noformat}\n");
                    for (StackTraceElement element : failureException.getStackTrace()) {
                        description.append(element.toString()).append("\n");
                    }
                    description.append("{noformat}\n");
                }

                // Add test execution date
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                description.append("\n*Execution Date:* ").append(dateFormat.format(new Date()));

                // Create the issue in Jira
                String issueKey = jiraReporter.createIssue(summary, description.toString(), testCase.getName());

                if (issueKey != null) {
                    System.out.println("Jira issue created: " + issueKey);
                    TestReporter.setJiraIssue(issueKey, jiraReporter.getJiraUrl());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to create Jira issue: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void executeStep(TestStep step) {
        // Handle the case where element is null or empty for actions like "open url"
        if (step.getElement() == null && step.getSelectorType() != null && step.getSelector() != null) {
            step.setElement(step.getSelectorType() + "=" + step.getSelector());
        }
        switch (step.getAction()) {
            case "end_the_test":
                if (step.isEndOfTest()) {
                    driver.quit();
                    return;
                }
                break;

            case "open_browser":
            case "open url":
                String url = step.getUrl();
                if (url != null && !url.isEmpty()) {
                    System.out.println("Opening URL: " + url);
                    driver.get(url);
                } else if (step.getValue() != null && !step.getValue().isEmpty()) {
                    // Fall back to value field if URL is not set
                    System.out.println("Opening URL from value: " + step.getValue());
                    driver.get(step.getValue());
                }
                break;

            case "input text":
            case "input_text":
                WebElement inputElement = findElement(step.getElement());
                inputElement.clear();
                inputElement.sendKeys(step.getValue());
                break;

            case "click":
                WebElement clickElement = findElement(step.getElement());
                clickElement.click();
                break;

            case "wait":
                try {
                    long waitTime = 2000;
                    if (step.getValue() != null && !step.getValue().isEmpty()) {
                        try {
                            waitTime = Long.parseLong(step.getValue());
                        } catch (NumberFormatException e) {
                            // Use default wait time
                        }
                    }
                    System.out.println("Waiting for " + waitTime + "ms");
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;

            case "verify_text":
                WebElement verifyElement = findElement(step.getElement());
                String actualText = verifyElement.getText();
                try
                {
                   Assert.assertEquals(actualText, step.getExpected(), "Text verification failed!");
                }
                catch(AssertionError e)
                {
                   TestReporter.fail("Text verification failed! Expected: " + step.getExpected() + ", Actual: " + actualText);
                   throw e;
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown action: " + step.getAction());
        }
    }

    private WebElement findElement(String locator) {
        if (locator != null && locator.contains("=")) {
            String[] parts = locator.split("=", 2);
            String locatorType = parts[0];
            String locatorValue = parts[1];

            try {
                switch (locatorType) {
                    case "id":
                        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(locatorValue)));
                    case "css":
                        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(locatorValue)));
                    case "xpath":
                        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(locatorValue)));
                    case "class":
                        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.className(locatorValue)));
                    default:
                        throw new IllegalArgumentException("Unknown selector type: " + locatorType);
                }
            } catch (TimeoutException e) {
                throw new NoSuchElementException("Element not found: " + locator);
            }
        }
        throw new IllegalArgumentException("Invalid locator format: " + locator);
    }
}