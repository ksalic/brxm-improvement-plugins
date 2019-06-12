package com.bloomreach.cms.workflow;

import org.onehippo.repository.documentworkflow.action.AbstractDocumentTaskAction;

public class TaxonomyFolderCreationAction extends AbstractDocumentTaskAction<TaxonomyFolderCreationTask> {

    @Override
    protected TaxonomyFolderCreationTask createWorkflowTask() {
        return new TaxonomyFolderCreationTask();
    }

}
