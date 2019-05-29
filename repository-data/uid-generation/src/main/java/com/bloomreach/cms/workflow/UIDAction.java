package com.bloomreach.cms.workflow;

import org.onehippo.repository.documentworkflow.action.AbstractDocumentTaskAction;

public class UIDAction extends AbstractDocumentTaskAction<UIDTask> {

    @Override
    protected UIDTask createWorkflowTask() {
        return new UIDTask();
    }

}
