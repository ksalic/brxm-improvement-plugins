package com.bloomreach.cms.workflow;

import javax.jcr.Node;

import org.apache.commons.scxml2.ActionExecutionContext;
import org.easymock.EasyMock;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManager;
import org.onehippo.cms7.services.lock.LockResource;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.mock.MockNode;

@Ignore
public class UIDTaskTest {

    @Test
    public void doExecute() throws LockException {

        DocumentHandle handle = EasyMock.createNiceMock(DocumentHandle.class);
        LockManager lockManager = EasyMock.createNiceMock(LockManager.class);
        LockResource lockResource = EasyMock.createNiceMock(LockResource.class);

        Node draft = new MockNode("draft") ;
        Node key = new MockNode("idgenerator") ;

        EasyMock.expect(handle.getDocuments().get("draft").getNode()).andReturn(draft).anyTimes();
        EasyMock.expect(lockManager.lock("/content/uid-generation")).andReturn(lockResource).anyTimes();



        UIDTask task = new UIDTask() {

            @Override
            LockManager getLockManager() {
                return super.getLockManager();
            }

            @Override
            public DocumentHandle getDocumentHandle() {
                return super.getDocumentHandle();
            }
        };

        try {
            task.doExecute();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //check value
    }
}