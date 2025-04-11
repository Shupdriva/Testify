package org.TestifyFW.reporter;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.Reporter;
import org.testng.asserts.SoftAssert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestReporter {
    private static ThreadLocal<String> jiraIssueKey = new ThreadLocal<>();
    private static ThreadLocal<String> jiraUrl = new ThreadLocal<>();
    private static ThreadLocal<SoftAssert> softAssert = new ThreadLocal<>();
    private static ThreadLocal<List<String>> stepLog = new ThreadLocal<>();
    private static ThreadLocal<List<StepResult>> detailedStepLog = new ThreadLocal<>();
    private static ThreadLocal<String> currentTestName = new ThreadLocal<>();
    private static ThreadLocal<Date> testStartTime = new ThreadLocal<>();
    private static ThreadLocal<WebDriver> currentDriver = new ThreadLocal<>();
    private static ThreadLocal<String> reportDirectory = new ThreadLocal<>();
    private static ThreadLocal<String> reportPath = new ThreadLocal<>();

    // Enum for step status
    public enum Status {
        PASSED, FAILED, SKIPPED, INFO
    }
    public static void setJiraIssue(String key, String url) {
        jiraIssueKey.set(key);
        jiraUrl.set(url);
    }

    public static String getJiraIssueKey() {
        return jiraIssueKey.get();
    }


    public static String getJiraUrl() {
        return jiraUrl.get();
    }
    // Class to store detailed step information
    public static class StepResult {
        private String stepName;
        private String action;
        private Status status;
        private String details;
        private String screenshotPath;
        private long timestamp;

        public StepResult(String stepName, String action, Status status, String details) {
            this.stepName = stepName;
            this.action = action;
            this.status = status;
            this.details = details;
            this.timestamp = System.currentTimeMillis();
        }

        public String getStepName() { return stepName; }
        public String getAction() { return action; }
        public Status getStatus() { return status; }
        public String getDetails() { return details; }
        public String getScreenshotPath() { return screenshotPath; }
        public long getTimestamp() { return timestamp; }

        public void setScreenshotPath(String path) {
            this.screenshotPath = path;
        }
    }

    public static void initializeReporter() {
        softAssert.set(new SoftAssert());
        stepLog.set(new ArrayList<>());
        detailedStepLog.set(new ArrayList<>());
        testStartTime.set(new Date());

        // Create report directory
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());

        String baseDir = "test-output/html-reports";
        String testDir = baseDir + "/test-run-" + timestamp;

        try {
            Files.createDirectories(Paths.get(testDir));
            Files.createDirectories(Paths.get(testDir + "/screenshots"));
            reportDirectory.set(testDir);
        } catch (IOException e) {
            System.err.println("Failed to create report directory: " + e.getMessage());
        }
    }

    public static void setCurrentDriver(WebDriver driver) {
        currentDriver.set(driver);
    }

    public static void setCurrentTestName(String testName) {
        currentTestName.set(testName);
    }

    public static void logStep(String stepName, String action, boolean status) {
        Status stepStatus = status ? Status.PASSED : Status.FAILED;
        String stepResult = status ? "PASSED" : "FAILED";
        String logMessage = String.format("Step: %s | Action: %s | Status: %s",
                stepName, action, stepResult);

        // Log to TestNG reporter
        Reporter.log(logMessage, true);

        // Add to our step log
        getStepLog().add(logMessage);

        // Add to detailed step log
        StepResult result = new StepResult(stepName, action, stepStatus,
                status ? "Step executed successfully" : "Step execution failed");

        // Take screenshot if step failed and driver is available
        if (!status && currentDriver.get() != null) {
            String screenshotPath = takeScreenshot(stepName);
            if (screenshotPath != null) {
                result.setScreenshotPath(screenshotPath);
            }
        }

        getDetailedStepLog().add(result);

        // If step failed, collect for soft assert
        if (!status) {
            getSoftAssert().fail(logMessage);
        }
    }

    public static void logInfo(String stepName, String message) {
        String logMessage = String.format("INFO: %s | %s", stepName, message);
        Reporter.log(logMessage, true);
        getStepLog().add(logMessage);

        StepResult result = new StepResult(stepName, "INFO", Status.INFO, message);
        getDetailedStepLog().add(result);
    }

    public static String takeScreenshot(String stepName) {
        WebDriver driver = currentDriver.get();
        if (driver == null) return null;

        if (driver instanceof TakesScreenshot) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("HHmmss");
                String timestamp = dateFormat.format(new Date());
                String screenshotName = stepName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".png";
                String dir = reportDirectory.get() + "/screenshots";
                String path = dir + "/" + screenshotName;

                // Take screenshot
                byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

                // Save to file
                try (FileOutputStream out = new FileOutputStream(path)) {
                    out.write(screenshotBytes);
                }

                return "screenshots/" + screenshotName;
            } catch (Exception e) {
                System.err.println("Failed to take screenshot: " + e.getMessage());
            }
        }
        return null;
    }

    public static void assertAll() {
        try {
            getSoftAssert().assertAll();
        } finally {
            // Don't remove the logs here, they'll be needed for reporting
        }
    }

    public static void generateReport() {
        if (reportDirectory.get() == null) {
            System.err.println("Report directory not initialized");
            return;
        }

        String testName = currentTestName.get() != null ? currentTestName.get() : "UnnamedTest";
        String reportFile = reportDirectory.get() + "/" + testName + "-Report.html";

        try (FileWriter writer = new FileWriter(reportFile)) {
            // Get test duration
            long duration = System.currentTimeMillis() - testStartTime.get().getTime();

            // Calculate test result
            boolean testPassed = true;
            for (StepResult step : getDetailedStepLog()) {
                if (step.getStatus() == Status.FAILED) {
                    testPassed = false;
                    break;
                }
            }

            if (getJiraIssueKey() != null && getJiraUrl() != null) {
                writer.write("    <p><strong>Jira Issue:</strong> <a href='" +
                        getJiraUrl() + "/browse/" + getJiraIssueKey() +
                        "' target='_blank'>" + getJiraIssueKey() + "</a></p>\n");
            }

            // Create HTML report
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html lang='en'>\n");
            writer.write("<head>\n");
            writer.write("  <meta charset='UTF-8'>\n");
            writer.write("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
            writer.write("  <title>Test Report: " + testName + "</title>\n");
            writer.write("  <style>\n");
            writer.write("    body { font-family: Arial, sans-serif; margin: 20px; }\n");
            writer.write("    h1 { color: #2c3e50; }\n");
            writer.write("    .summary { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n");
            writer.write("    .passed { color: green; }\n");
            writer.write("    .failed { color: red; }\n");
            writer.write("    .info { color: blue; }\n");
            writer.write("    .step { margin-bottom: 10px; border: 1px solid #ddd; padding: 10px; border-radius: 5px; }\n");
            writer.write("    .step-passed { border-left: 5px solid green; }\n");
            writer.write("    .step-failed { border-left: 5px solid red; }\n");
            writer.write("    .step-info { border-left: 5px solid blue; }\n");
            writer.write("    .screenshot { max-width: 800px; border: 1px solid #ddd; margin-top: 10px; }\n");
            writer.write("    .details-toggle { cursor: pointer; color: #007bff; }\n");
            writer.write("    .step-details { display: none; margin-top: 10px; }\n");
            writer.write("  </style>\n");
            writer.write("  <script>\n");
            writer.write("    function toggleDetails(id) {\n");
            writer.write("      var element = document.getElementById(id);\n");
            writer.write("      element.style.display = element.style.display === 'block' ? 'none' : 'block';\n");
            writer.write("    }\n");
            writer.write("  </script>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");

            // Header
            writer.write("  <h1>Test Report: " + testName + "</h1>\n");

            // Summary
            writer.write("  <div class='summary'>\n");
            writer.write("    <p><strong>Date:</strong> " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(testStartTime.get()) + "</p>\n");
            writer.write("    <p><strong>Duration:</strong> " + formatDuration(duration) + "</p>\n");
            writer.write("    <p><strong>Result:</strong> <span class='" + (testPassed ? "passed" : "failed") + "'>" +
                    (testPassed ? "PASSED" : "FAILED") + "</span></p>\n");
            writer.write("  </div>\n");

            // Steps
            writer.write("  <h2>Test Procedure</h2>\n");

            int stepCounter = 1;
            for (StepResult step : getDetailedStepLog()) {
                String statusClass = "";
                String statusText = "";

                switch (step.getStatus()) {
                    case PASSED:
                        statusClass = "step-passed";
                        statusText = "PASSED";
                        break;
                    case FAILED:
                        statusClass = "step-failed";
                        statusText = "FAILED";
                        break;
                    case INFO:
                        statusClass = "step-info";
                        statusText = "INFO";
                        break;
                    default:
                        statusClass = "";
                        statusText = step.getStatus().toString();
                }

                writer.write("  <div class='step " + statusClass + "'>\n");
                writer.write("    <p><strong>" + stepCounter + ":</strong> " + step.getStepName() + "</p>\n");
                writer.write("    <p><strong>Action:</strong> " + step.getAction() + "</p>\n");
                writer.write("    <p><strong>Status:</strong> <span class='" +
                        step.getStatus().toString().toLowerCase() + "'>" + statusText + "</span></p>\n");
                writer.write("    <p><strong>Time:</strong> " +
                        new SimpleDateFormat("HH:mm:ss").format(new Date(step.getTimestamp())) + "</p>\n");

                // Toggle for details
                writer.write("    <p class='details-toggle' onclick=\"toggleDetails('details-" + stepCounter + "')\">" +
                        "Show/Hide Details</p>\n");
                writer.write("    <div id='details-" + stepCounter + "' class='step-details'>\n");
                writer.write("      <p><strong>Details:</strong> " + step.getDetails() + "</p>\n");

                // Add screenshot if available
                if (step.getScreenshotPath() != null) {
                    writer.write("      <p><strong>Screenshot:</strong></p>\n");
                    writer.write("      <img src='" + step.getScreenshotPath() + "' class='screenshot' alt='Screenshot'>\n");
                }

                writer.write("    </div>\n");
                writer.write("  </div>\n");

                stepCounter++;
            }

            writer.write("</body>\n");
            writer.write("</html>\n");

            // Set the report path for viewing
            reportPath.set(reportFile);

            System.out.println("Test report generated: " + reportFile);
        } catch (IOException e) {
            System.err.println("Failed to generate report: " + e.getMessage());
        }
    }

    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        return String.format("%d min, %d sec", minutes, seconds);
    }

    public static void cleanup() {
        jiraIssueKey.remove();
        jiraUrl.remove();
        softAssert.remove();
        stepLog.remove();
        detailedStepLog.remove();
        currentTestName.remove();
        testStartTime.remove();
        currentDriver.remove();
        reportDirectory.remove();
        reportPath.remove();
    }

    public static SoftAssert getSoftAssert() {
        if (softAssert.get() == null) {
            initializeReporter();
        }
        return softAssert.get();
    }

    public static List<String> getStepLog() {
        if (stepLog.get() == null) {
            initializeReporter();
        }
        return stepLog.get();
    }

    public static List<StepResult> getDetailedStepLog() {
        if (detailedStepLog.get() == null) {
            initializeReporter();
        }
        return detailedStepLog.get();
    }

    public static String getReportPath() {
        return reportPath.get();
    }

    public static void fail(String message) {
        getSoftAssert().fail(message);
        Reporter.log("FAILED: " + message, true);
    }
}