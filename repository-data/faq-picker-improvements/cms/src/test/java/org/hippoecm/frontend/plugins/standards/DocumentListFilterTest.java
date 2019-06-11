package org.hippoecm.frontend.plugins.standards;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.junit.Test;
import org.onehippo.repository.util.NodeTypeUtils;

import static org.junit.Assert.assertTrue;

public class DocumentListFilterTest extends org.hippoecm.frontend.PluginTest {

    String[] content = new String[]{
            "/test",
            "nt:unstructured",
            "/test/content",
            "hippostd:folder",
            "/test/content/toBeShowed",
            "hippo:handle",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/content/toBeShowed/toBeShowed",
            "test:faqitem",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/content/toBeHidden",
            "hippo:handle",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/content/toBeHidden/toBeHidden",
            "hippo:document",
            "jcr:mixinTypes", "mix:referenceable",
            "/plugin",
            "nt:unstructured",
            "/plugin/filters",
            "frontend:pluginconfig",
            "/plugin/filters/showFaqItemType",
            "frontend:pluginconfig",
            "child", "hippo:handle",
            "display", "true",
            "subchild", "test:faqitem",
            "/plugin/filters/hideDocuments",
            "frontend:pluginconfig",
            "child", "hippo:handle",
            "display", "false",
            "subchild", "hippo:document",


    };


    private static <T> List<T>
    getListFromIterator(Iterator<T> iterator) {

        // Create an empty list
        List<T> list = new ArrayList<>();

        // Add each element of iterator to the List
        iterator.forEachRemaining(list::add);

        // Return the List
        return list;
    }

    //    @Ignore
    @Test
    public void filter() throws RepositoryException {
        session.getWorkspace().getNamespaceRegistry().registerNamespace("test", "http://www.test.com/test/nt/1.0");
        NodeTypeUtils.initializeNodeTypes(session, getClass().getResourceAsStream("/test.cnd"), "test");
        build(content, session);
        session.save();

        IPluginConfig config = new JcrPluginConfig(new JcrNodeModel(session.getNode("/plugin")));
        DocumentListFilter documentListFilter = new DocumentListFilter(config);

        Node folder = session.getNode("/test/content");

        NodeIterator it = documentListFilter.filter(folder, folder.getNodes());

        List<Node> list = getListFromIterator(it);
        assertTrue(list.size() == 1);
        assertTrue(list.get(0).getName().equals("toBeShowed"));
        removeNode("/plugin");
    }
}