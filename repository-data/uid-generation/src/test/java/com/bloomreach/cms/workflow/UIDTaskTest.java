package com.bloomreach.cms.workflow;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertTrue;

@Ignore
public class UIDTaskTest {

    @Test
    public void doExecute() throws RepositoryException {
        UIDAction action = new UIDAction();
        UIDTask workflowTask = action.createWorkflowTask();

        assertTrue(workflowTask instanceof UIDTask);

        MockNode root = MockNode.root();
        Session session = root.getSession();

        Node draft1 = new MockNode("draft1");
        Node draft2 = new MockNode("draft2");
        Node key = new MockNode("uid-generation", "uidgeneration:container");
        key.setProperty("uidgeneration:counter", 1);
        key.setProperty("uidgeneration:format", "%08d");
        MockNode content = new MockNode("content");

        content.addNode((MockNode)key);

        root.addNode((MockNode)draft1);
        root.addNode((MockNode)draft2);
        root.addNode(content);


        workflowTask.generateAndUpdateUID(draft1, session);
        workflowTask.generateAndUpdateUID(draft2, session);

        assertTrue(draft1.isNodeType("uidgeneration:identifable"));
        assertTrue(draft1.getProperty("uidgeneration:id").getString().equals("00000001"));

        assertTrue(draft2.isNodeType("uidgeneration:identifable"));
        assertTrue(draft2.getProperty("uidgeneration:id").getString().equals("00000002"));
    }
}