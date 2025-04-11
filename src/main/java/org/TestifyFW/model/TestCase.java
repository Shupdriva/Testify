package org.TestifyFW.model;
//import org.TestifyFW.model.TestStep;

import java.util.List;

public class TestCase {
    private String name;
    private List<TestStep> steps;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TestStep> getSteps() {
        return steps;
    }

    public void setSteps(List<TestStep> steps) {
        this.steps = steps;
    }
}
