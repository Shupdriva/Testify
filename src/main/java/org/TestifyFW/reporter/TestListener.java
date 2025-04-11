package org.TestifyFW.reporter;

import java.util.List;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.TestifyFW.jira.JiraIssueReporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestListener implements ITestListener {
    private JiraIssueReporter jiraReporter;

    public TestListener() {
        jiraReporter = new JiraIssueReporter();
    }

    @Override
    public void onTestStart(ITestResult result) {
        System.out.println("============================================");
        System.out.println("TEST STARTED: " + result.getName());
        System.out.println("============================================");
        TestReporter.initializeReporter();
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.println("============================================");
        System.out.println("TEST PASSED: " + result.getName());
        System.out.println("============================================");

        // Print step log for successful tests
        List<String> steps = TestReporter.getStepLog();
        for (String step : steps) {
            System.out.println(step);
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        System.out.println("============================================");
        System.out.println("TEST FAILED: " + result.getName());
        System.out.println("Failure Details: " + result.getThrowable().getMessage());
        System.out.println("============================================");

        // Print step log for failed tests
        List<String> steps = TestReporter.getStepLog();
        for (String step : steps) {
            System.out.println(step);
        }

        // Create Jira issue for the failed test
        if (jiraReporter.isEnabled()) {
            createJiraIssue(result);
        }

        // Take screenshot on failure
        // takeScreenshot(result.getName());
    }

    private void createJiraIssue(ITestResult result) {
        try {
            // Get test method name
            String testName = result.getName();

            // Create a summary for the issue
            String summary = "Test Failure: " + testName;

            // Build a detailed description including the stack trace
            StringBuilder description = new StringBuilder();
            description.append("*Test Name:* ").append(testName).append("\n\n");
            description.append("*Test Class:* ").append(result.getTestClass().getName()).append("\n\n");
            description.append("*Failure Message:* ").append(result.getThrowable().getMessage()).append("\n\n");

            // Add test steps
            description.append("*Test Steps:*\n");
            List<String> steps = TestReporter.getStepLog();
            for (String step : steps) {
                description.append("# ").append(step).append("\n");
            }

            // Add stack trace
            description.append("\n*Stack Trace:*\n{noformat}\n");
            for (StackTraceElement element : result.getThrowable().getStackTrace()) {
                description.append(element.toString()).append("\n");
            }
            description.append("{noformat}\n");

            // Add test execution date
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            description.append("\n*Execution Date:* ").append(dateFormat.format(new Date()));

            // Create the issue in Jira
            String issueKey = jiraReporter.createIssue(summary, description.toString(), testName);

            if (issueKey != null) {
                System.out.println("Jira issue created: " + issueKey);
            }
        } catch (Exception e) {
            System.err.println("Failed to create Jira issue: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.println("============================================");
        System.out.println("TEST SKIPPED: " + result.getName());
        System.out.println("Reason: " + (result.getThrowable() != null ? result.getThrowable().getMessage() : "Dependency failed"));
        System.out.println("============================================");
    }

    @Override
    public void onFinish(ITestContext context) {
        // Generate summary report
        try {
            generateSummaryReport(context);
        } catch (Exception e) {
            System.err.println("Failed to generate summary report: " + e.getMessage());
        }
    }

    private void generateSummaryReport(ITestContext context) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File reportDir = new File("test-output/html-reports");
        reportDir.mkdirs();

        File reportFile = new File(reportDir, "test-summary-" + timestamp + ".html");
        FileWriter writer = new FileWriter(reportFile);

        writer.write("<html><head><title>Test Execution Summary</title>");
        writer.write("<style>table {border-collapse: collapse; width: 100%;} ");
        writer.write("th, td {text-align: left; padding: 8px; border: 1px solid #ddd;} ");
        writer.write("th {background-color: #4CAF50; color: white;} ");
        writer.write("tr:nth-child(even) {background-color: #f2f2f2;} ");
        writer.write(".passed {color: green;} .failed {color: red;} .skipped {color: orange;}</style>");
        writer.write("</head><body>");

        writer.write("<h1>Test Execution Summary</h1>");
        writer.write("<p>Execution Date: " + new Date() + "</p>");

        writer.write("<h2>Results Summary</h2>");
        writer.write("<table>");
        writer.write("<tr><th>Total Tests</th><th>Passed</th><th>Failed</th><th>Skipped</th></tr>");
        writer.write("<tr>");
        writer.write("<td>" + (context.getPassedTests().size() + context.getFailedTests().size() + context.getSkippedTests().size()) + "</td>");
        writer.write("<td class='passed'>" + context.getPassedTests().size() + "</td>");
        writer.write("<td class='failed'>" + context.getFailedTests().size() + "</td>");
        writer.write("<td class='skipped'>" + context.getSkippedTests().size() + "</td>");
        writer.write("</tr>");
        writer.write("</table>");

        // Test details
        writer.write("<h2>Test Details</h2>");
        writer.write("<table>");
        writer.write("<tr><th>Test Name</th><th>Status</th><th>Duration (ms)</th></tr>");

        // Passed tests
        for (ITestResult result : context.getPassedTests().getAllResults()) {
            writer.write("<tr>");
            writer.write("<td>" + result.getName() + "</td>");
            writer.write("<td class='passed'>PASSED</td>");
            writer.write("<td>" + (result.getEndMillis() - result.getStartMillis()) + "</td>");
            writer.write("</tr>");
        }

        // Failed tests
        for (ITestResult result : context.getFailedTests().getAllResults()) {
            writer.write("<tr>");
            writer.write("<td>" + result.getName() + "</td>");
            writer.write("<td class='failed'>FAILED</td>");
            writer.write("<td>" + (result.getEndMillis() - result.getStartMillis()) + "</td>");
            writer.write("</tr>");
        }

        // Skipped tests
        for (ITestResult result : context.getSkippedTests().getAllResults()) {
            writer.write("<tr>");
            writer.write("<td>" + result.getName() + "</td>");
            writer.write("<td class='skipped'>SKIPPED</td>");
            writer.write("<td>N/A</td>");
            writer.write("</tr>");
        }

        writer.write("</table>");
        writer.write("</body></html>");

        writer.close();
        System.out.println("Summary report generated: " + reportFile.getAbsolutePath());
    }
}