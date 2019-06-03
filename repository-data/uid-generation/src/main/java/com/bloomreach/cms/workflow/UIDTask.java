package com.bloomreach.cms.workflow;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.lock.AlreadyLockedException;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManager;
import org.onehippo.cms7.services.lock.LockResource;
import org.onehippo.repository.documentworkflow.task.AbstractDocumentTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UIDTask extends AbstractDocumentTask {

    private static final Logger log = LoggerFactory.getLogger(UIDTask.class);

    private static final String COUNTER = "/content/uid-generation";
    private static final String COUNTER_PROPERTY = "uidgeneration:counter";
    private static final String ID_PROPERY = "uidgeneration:id";
    private static final String FORMAT_PROPERTY = "uidgeneration:format";
    private static final String MIXIN = "uidgeneration:identifable";

    private final LockManager lockManager = HippoServiceRegistry.getService(LockManager.class);

    @Override
    protected Object doExecute() throws RepositoryException {
        Node draft = getDocumentHandle().getDocuments().get("draft").getNode();
        if (!draft.hasProperty(ID_PROPERY)) {
            try (LockResource ignore = lockManager.lock(COUNTER)) {
                Session session = getWorkflowContext().getInternalWorkflowSession();
                session.refresh(true);
                generateAndUpdateUID(draft, session);
            } catch (AlreadyLockedException e) {
                log.warn("'{}' is already locked", COUNTER, e);
//                try {
////                    LockManagerUtils.waitForLock(lockManager, COUNTER, 500, 1000 * 5);
////                    doExecute(); try again?
//                } catch (LockException | TimeoutException | InterruptedException ex) {
//                    log.error("Exception while trying to wait for lock", ex);
//                }
            } catch (LockException e) {
                log.error("Exception while trying to obtain lock", e);
            }
        }
        return null;
    }

    protected void generateAndUpdateUID(final Node draft, final Session session) throws RepositoryException {
        if (!draft.isNodeType(MIXIN)) {
            draft.addMixin(MIXIN);
        }

        Node uidGeneration = session.getNode(COUNTER);
        long id = uidGeneration.getProperty(COUNTER_PROPERTY).getLong();
        String format = uidGeneration.getProperty(FORMAT_PROPERTY).getString();
        long docUid = id++;
        draft.setProperty(ID_PROPERY, String.format(format, docUid));
        draft.getSession().save();

        uidGeneration.setProperty(COUNTER_PROPERTY, id);
        session.save();
    }


}
