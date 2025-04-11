package org.TestifyFW.yaml;

import org.TestifyFW.model.TestCase;
import org.TestifyFW.model.TestStep;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YamlTestLoader {

    public TestCase loadTestCase(String yamlFilePath) {
        try (InputStream input = new FileInputStream(yamlFilePath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);

            TestCase testCase = new TestCase();
            testCase.setName((String) data.get("testCaseName"));

            List<TestStep> steps = new ArrayList<>();
            List<LinkedHashMap<String, Object>> yamlSteps = (List<LinkedHashMap<String, Object>>) data.get("steps");

            for (LinkedHashMap<String, Object> yamlStep : yamlSteps) {
                TestStep step = new TestStep();

                step.setAction((String) yamlStep.get("action"));

                // Handle selector
                String selectorType = (String) yamlStep.get("selectorType");
                String selector = (String) yamlStep.get("selector");

                if (selectorType != null && selector != null) {
                    step.setSelectorType(selectorType);
                    step.setSelector(selector);
                    step.setElement(selectorType + "=" + selector);
                }

                // Handle other fields
                step.setValue(yamlStep.get("value") != null ? yamlStep.get("value").toString() : null);
                step.setExpected((String) yamlStep.get("expected"));
                step.setUrl((String) yamlStep.get("url"));

                Boolean critical = (Boolean) yamlStep.get("critical");
                step.setCritical(critical != null ? critical : false);

                Boolean endOfTest = (Boolean) yamlStep.get("endOfTest");
                step.setEndOfTest(endOfTest != null ? endOfTest : false);

                steps.add(step);
            }

            testCase.setSteps(steps);
            return testCase;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}