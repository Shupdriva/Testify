package org.TestifyFW.jira;

import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Class to handle reporting issues to Jira
 */
public class JiraIssueReporter {
    private String jiraUrl;
    private String username;
    private String apiToken;
    private String projectKey;
    private JiraClient jiraClient;
    private boolean enabled = false;

    public JiraIssueReporter() {
        loadConfiguration();
        if (enabled) {
            initJiraClient();
        }
    }
    public String getJiraUrl() {
        return jiraUrl;
    }

    private void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("jira-config.properties")) {
            props.load(input);

            jiraUrl = props.getProperty("jira.url");
            username = props.getProperty("jira.username");
            apiToken = props.getProperty("jira.api_token");
            projectKey = props.getProperty("jira.project_key");
            enabled = Boolean.parseBoolean(props.getProperty("jira.enabled", "false"));

            if (enabled && (jiraUrl == null || username == null || apiToken == null || projectKey == null)) {
                System.err.println("Jira integration is enabled but configuration is incomplete. Disabling integration.");
                enabled = false;
            }
        } catch (IOException e) {
            System.err.println("Failed to load Jira configuration: " + e.getMessage());
            enabled = false;
        }
    }

    private void initJiraClient() {
        BasicCredentials creds = new BasicCredentials(username, apiToken);
        jiraClient = new JiraClient(jiraUrl, creds);
    }

    public String createIssue(String summary, String description, String testName) {
        System.out.println("Attempting to create Jira issue with URL: " + jiraUrl);
        System.out.println("Using project key: " + projectKey);
        if (!enabled) {
            System.out.println("Jira integration is disabled. No issue created.");
            return null;
        }

        try {
            // Create issue
            Issue.FluentCreate issueCreate = jiraClient.createIssue(projectKey, "Bug");

            // Set fields
            issueCreate.field(Field.SUMMARY, "[Automation Failure] " + summary);
            issueCreate.field(Field.DESCRIPTION, description);

            // Add custom fields if needed
            // issueCreate.field("customfield_10001", "Test Automation");

            // Create the issue
            Issue issue = issueCreate.execute();
            String issueKey = issue.getKey();

            System.out.println("Creating JIRA issue: " + issueKey);
            return issueKey;

        } catch (JiraException e) {
            System.err.println("Failed to create Jira issue: " + e.getMessage());
            return null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}