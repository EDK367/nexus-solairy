package org.nexus.nexussolairy.model.view;

import java.util.List;

public class StackStepViewModel {
    public static class StackFrame {
        private final String label;
        private final String type;

        public StackFrame(String label, String type) {
            this.label = label;
            this.type = type;
        }

        public String getLabel() {
            return label;
        }

        public String getType() {
            return type;
        }
    }

    private final int stepNumber;
    private final String actionName;
    private final String actionType;
    private final String currentToken;
    private final String currentRule;
    private final List<StackFrame> stackFrames;
    private final String logMessage;

    public StackStepViewModel(int stepNumber, String actionName, String actionType, String currentToken, String currentRule, List<StackFrame> stackFrames, String logMessage) {
        this.stepNumber = stepNumber;
        this.actionName = actionName;
        this.actionType = actionType;
        this.currentToken = currentToken;
        this.currentRule = currentRule;
        this.stackFrames = stackFrames;
        this.logMessage = logMessage;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public String getActionName() {
        return actionName;
    }

    public String getActionType() {
        return actionType;
    }

    public String getCurrentToken() {
        return currentToken;
    }

    public String getCurrentRule() {
        return currentRule;
    }

    public List<StackFrame> getStackFrames() {
        return stackFrames;
    }

    public String getLogMessage() {
        return logMessage;
    }
}
