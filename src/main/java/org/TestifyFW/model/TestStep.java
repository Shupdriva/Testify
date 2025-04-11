package org.TestifyFW.model;


public class TestStep {
    private String action;
    private String element;
    private String value;
    private String url;
    private String expected;
    private Boolean endOfTest;
    private String selectorType;
    private String selector;
    private Boolean critical = false;     // subsequent steps won't execute if this step fails
    private String dependsOn;            // ID of step this step depends on

    // Getters and Setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    // Getter that handles null values safely
    public boolean isEndOfTest() {
        return endOfTest != null && endOfTest;
    }

    // Setter
    public void setEndOfTest(Boolean endOfTest) {
        this.endOfTest = endOfTest;
    }

    public String getSelectorType() {
        return selectorType;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelectorType(String selectorType) {
        this.selectorType = selectorType;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public boolean isCritical() {
        return critical != null && critical;
    }

    public void setCritical(Boolean critical) {
        this.critical = critical;
    }

    public String getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(String dependsOn) {
        this.dependsOn = dependsOn;
    }
}