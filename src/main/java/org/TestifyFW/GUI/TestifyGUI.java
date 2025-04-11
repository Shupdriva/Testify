package org.TestifyFW.GUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.TestifyFW.framework.ActionHandler;
import org.TestifyFW.framework.BaseTest;
import org.TestifyFW.model.TestCase;
import org.TestifyFW.model.TestStep;
import org.TestifyFW.yaml.YamlTestLoader;
import org.TestifyFW.yaml.YamlTestWriter;
import org.openqa.selenium.WebDriver;

public class TestifyGUI extends JFrame {
    private JPanel contentPane;
    private JTabbedPane tabbedPane;
    private JTable stepTable;
    private DefaultTableModel tableModel;
    private JTextField testCaseNameField;
    private TestCase currentTestCase;
    private WebDriver driver;

    private static final String[] ACTIONS = {
            "open url", "click", "input text", "wait", "verify_text", "end_the_test"
    };

    private static final String[] SELECTOR_TYPES = {
            "id", "css", "xpath", "class"
    };

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    TestifyGUI frame = new TestifyGUI();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public TestifyGUI() {
        setTitle("TestifyFW - Test Automation Framework");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 600, 400);

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        contentPane.add(tabbedPane, BorderLayout.CENTER);

        // Add tabs
        tabbedPane.addTab("Manual Test Builder", createManualTestPanel());
        tabbedPane.addTab("YAML Test Runner", createYamlTestPanel());

        // Initialize test case
        currentTestCase = new TestCase();
        currentTestCase.setSteps(new ArrayList<>());
    }

    private JPanel createManualTestPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 5));

        // North panel - Test Case Information
        JPanel northPanel = new JPanel(new BorderLayout());

        JPanel testCaseInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        testCaseInfo.add(new JLabel("Test Case Name:"));
        testCaseNameField = new JTextField(30);
        testCaseInfo.add(testCaseNameField);

        northPanel.add(testCaseInfo, BorderLayout.NORTH);
        panel.add(northPanel, BorderLayout.NORTH);

        // Center panel - Steps Table
        String[] columnNames = {"Action", "Selector Type", "Selector Value", "Input Value", "Expected Value", "URL", "Critical"};
        tableModel = new DefaultTableModel(columnNames, 0);
        stepTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(stepTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // South panel - Actions
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnAddStep = new JButton("Add Step");
        btnAddStep.addActionListener(e -> showAddStepDialog());
        southPanel.add(btnAddStep);

        JButton btnRemoveStep = new JButton("Remove Step");
        btnRemoveStep.addActionListener(e -> {
            int selectedRow = stepTable.getSelectedRow();
            if (selectedRow != -1) {
                tableModel.removeRow(selectedRow);
            }
        });
        southPanel.add(btnRemoveStep);

        JButton btnSaveYaml = new JButton("Save as YAML");
        btnSaveYaml.addActionListener(e -> saveTestCaseAsYaml());
        southPanel.add(btnSaveYaml);

        JButton btnExecute = new JButton("Execute Test");
        btnExecute.addActionListener(e -> executeTest());
        southPanel.add(btnExecute);

        JButton btnClear = new JButton("Clear All");
        btnClear.addActionListener(e -> {
            tableModel.setRowCount(0);
            testCaseNameField.setText("");
        });
        southPanel.add(btnClear);

        panel.add(southPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createYamlTestPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JTextField filePathField = new JTextField(40);
        filePathField.setEditable(false);

        JButton btnBrowse = new JButton("Browse");
        btnBrowse.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File("."));
            int result = fileChooser.showOpenDialog(panel);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                filePathField.setText(selectedFile.getAbsolutePath());
            }
        });

        filePanel.add(new JLabel("YAML File:"));
        filePanel.add(filePathField);
        filePanel.add(btnBrowse);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnExecuteYaml = new JButton("Execute YAML Test");
        btnExecuteYaml.addActionListener(e -> {
            String filePath = filePathField.getText();
            if (!filePath.isEmpty()) {
                executeYamlTest(filePath);
            } else {
                JOptionPane.showMessageDialog(panel, "Please select a YAML file first.", "No File Selected", JOptionPane.WARNING_MESSAGE);
            }
        });
        buttonPanel.add(btnExecuteYaml);

        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(filePanel);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(buttonPanel);
        centerPanel.add(Box.createVerticalGlue());

        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    private void showAddStepDialog() {
        JDialog dialog = new JDialog(this, "Add Test Step", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Action selection
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionPanel.add(new JLabel("Action:"));
        JComboBox<String> actionCombo = new JComboBox<>(ACTIONS);
        actionPanel.add(actionCombo);
        panel.add(actionPanel);
        panel.add(Box.createVerticalStrut(10));

        // Selector type and value
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectorPanel.add(new JLabel("Selector Type:"));
        JComboBox<String> selectorTypeCombo = new JComboBox<>(SELECTOR_TYPES);
        selectorPanel.add(selectorTypeCombo);
        selectorPanel.add(new JLabel("Selector Value:"));
        JTextField selectorValueField = new JTextField(15);
        selectorPanel.add(selectorValueField);
        panel.add(selectorPanel);
        panel.add(Box.createVerticalStrut(10));

        // Input value
        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        valuePanel.add(new JLabel("Input Value:"));
        JTextField valueField = new JTextField(30);
        valuePanel.add(valueField);
        panel.add(valuePanel);
        panel.add(Box.createVerticalStrut(10));

        // Expected value
        JPanel expectedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        expectedPanel.add(new JLabel("Expected Value:"));
        JTextField expectedField = new JTextField(30);
        expectedPanel.add(expectedField);
        panel.add(expectedPanel);
        panel.add(Box.createVerticalStrut(10));

        // URL
        JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        urlPanel.add(new JLabel("URL:"));
        JTextField urlField = new JTextField(30);
        urlPanel.add(urlField);
        panel.add(urlPanel);
        panel.add(Box.createVerticalStrut(10));

        // Critical checkbox
        JPanel criticalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox criticalCheckBox = new JCheckBox("Critical Step");
        criticalPanel.add(criticalCheckBox);
        panel.add(criticalPanel);
        panel.add(Box.createVerticalStrut(20));

        // Update visibility based on action selection
        actionCombo.addActionListener(e -> {
            String selectedAction = (String) actionCombo.getSelectedItem();
            boolean isSelectorNeeded = !"open url".equals(selectedAction) && !"wait".equals(selectedAction) && !"end_the_test".equals(selectedAction);
            boolean isUrlNeeded = "open url".equals(selectedAction);
            boolean isValueNeeded = "input text".equals(selectedAction) || "wait".equals(selectedAction);
            boolean isExpectedNeeded = "verify_text".equals(selectedAction);

            selectorTypeCombo.setEnabled(isSelectorNeeded);
            selectorValueField.setEnabled(isSelectorNeeded);
            valueField.setEnabled(isValueNeeded);
            expectedField.setEnabled(isExpectedNeeded);
            urlField.setEnabled(isUrlNeeded);
        });

        // Initialize visibility
        actionCombo.setSelectedIndex(0);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> {
            Vector<Object> rowData = new Vector<>();
            rowData.add(actionCombo.getSelectedItem());
            rowData.add(selectorTypeCombo.getSelectedItem());
            rowData.add(selectorValueField.getText());
            rowData.add(valueField.getText());
            rowData.add(expectedField.getText());
            rowData.add(urlField.getText());
            rowData.add(criticalCheckBox.isSelected());

            tableModel.addRow(rowData);
            dialog.dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel);

        dialog.getContentPane().add(panel);
        dialog.setVisible(true);
    }

    private void executeTest() {
        if (testCaseNameField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a test case name.", "Missing Test Case Name", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Please add at least one test step.", "No Test Steps", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create test case
        currentTestCase = new TestCase();
        currentTestCase.setName(testCaseNameField.getText());

        List<TestStep> steps = new ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            TestStep step = new TestStep();
            step.setAction((String) tableModel.getValueAt(i, 0));

            // Only set selector if needed
            if (tableModel.getValueAt(i, 1) != null && !tableModel.getValueAt(i, 1).toString().isEmpty()) {
                step.setSelectorType((String) tableModel.getValueAt(i, 1));
                step.setSelector((String) tableModel.getValueAt(i, 2));
                step.setElement(step.getSelectorType() + "=" + step.getSelector());
            }

            step.setValue((String) tableModel.getValueAt(i, 3));
            step.setExpected((String) tableModel.getValueAt(i, 4));
            step.setUrl((String) tableModel.getValueAt(i, 5));
            step.setCritical((Boolean) tableModel.getValueAt(i, 6));

            if ("end_the_test".equals(step.getAction())) {
                step.setEndOfTest(true);
            }

            steps.add(step);
        }

        currentTestCase.setSteps(steps);
       // boolean testFailed = false;
        // Execute in a separate thread to keep UI responsive
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    BaseTest baseTest = new BaseTest();
                    baseTest.setup();
                    driver = baseTest.driver;

                    ActionHandler actionHandler = new ActionHandler(driver);
                    actionHandler.executeTestCase(currentTestCase);

                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                JOptionPane.showMessageDialog(TestifyGUI.this,
                        "Test execution completed. Check the console for results.",
                        "Test Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        };

        worker.execute();
    }

    private void saveTestCaseAsYaml() {
        if (testCaseNameField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a test case name.", "Missing Test Case Name", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Please add at least one test step.", "No Test Steps", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create the TestCase object from the current UI state
        TestCase testCase = new TestCase();
        testCase.setName(testCaseNameField.getText());

        List<TestStep> steps = new ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            TestStep step = new TestStep();
            step.setAction((String) tableModel.getValueAt(i, 0));

            // Only set selector if needed
            if (tableModel.getValueAt(i, 1) != null && !tableModel.getValueAt(i, 1).toString().isEmpty()) {
                step.setSelectorType((String) tableModel.getValueAt(i, 1));
                step.setSelector((String) tableModel.getValueAt(i, 2));
                step.setElement(step.getSelectorType() + "=" + step.getSelector());
            }

            step.setValue((String) tableModel.getValueAt(i, 3));
            step.setExpected((String) tableModel.getValueAt(i, 4));
            step.setUrl((String) tableModel.getValueAt(i, 5));
            step.setCritical((Boolean) tableModel.getValueAt(i, 6));

            if ("end_the_test".equals(step.getAction())) {
                step.setEndOfTest(true);
            }

            steps.add(step);
        }

        testCase.setSteps(steps);

        // Show save dialog
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Test Case as YAML");

        // Set default file name based on test case name
        String defaultFileName = testCase.getName().replaceAll("\\s+", "_") + ".yaml";
        fileChooser.setSelectedFile(new File(defaultFileName));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            // Add .yaml extension if not present
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".yaml") && !filePath.toLowerCase().endsWith(".yml")) {
                filePath += ".yaml";
            }

            // Save the test case
            YamlTestWriter yamlWriter = new YamlTestWriter();
            boolean success = yamlWriter.saveTestCase(testCase, filePath);

            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Test case saved successfully to:\n" + filePath,
                        "Save Successful", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to save test case. Check console for details.",
                        "Save Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void executeYamlTest(String yamlFilePath) {
       // boolean testFailed = false;
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    YamlTestLoader yamlLoader = new YamlTestLoader();
                    TestCase testCase = yamlLoader.loadTestCase(yamlFilePath);

                    if (testCase != null) {
                        BaseTest baseTest = new BaseTest();
                        baseTest.setup();
                        driver = baseTest.driver;

                        ActionHandler actionHandler = new ActionHandler(driver);
                        actionHandler.executeTestCase(testCase);
                    }

                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                JOptionPane.showMessageDialog(TestifyGUI.this,
                        "YAML test execution completed. Check the console for results.",
                        "Test Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        };

        worker.execute();
    }
}