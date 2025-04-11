package org.TestifyFW.yaml;

import org.TestifyFW.model.TestCase;
import org.TestifyFW.model.TestStep;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YamlTestWriter {

    public boolean saveTestCase(TestCase testCase, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            Yaml yaml = new Yaml(options);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("testCaseName", testCase.getName());

            List<Map<String, Object>> steps = new ArrayList<>();

            for (TestStep step : testCase.getSteps()) {
                Map<String, Object> stepMap = new LinkedHashMap<>();

                // Add all step properties
                stepMap.put("action", step.getAction());

                // Only include selector details if they exist
                if (step.getSelectorType() != null && !step.getSelectorType().isEmpty()) {
                    stepMap.put("selectorType", step.getSelectorType());
                    stepMap.put("selector", step.getSelector());
                }

                // Add other properties, only if they have values
                if (step.getValue() != null && !step.getValue().isEmpty()) {
                    stepMap.put("value", step.getValue());
                }

                if (step.getExpected() != null && !step.getExpected().isEmpty()) {
                    stepMap.put("expected", step.getExpected());
                }

                if (step.getUrl() != null && !step.getUrl().isEmpty()) {
                    stepMap.put("url", step.getUrl());
                }

                // Include boolean properties
                if (step.isCritical()) {
                    stepMap.put("critical", true);
                }

                if (step.isEndOfTest()) {
                    stepMap.put("endOfTest", true);
                }

                steps.add(stepMap);
            }

            data.put("steps", steps);

            yaml.dump(data, writer);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}