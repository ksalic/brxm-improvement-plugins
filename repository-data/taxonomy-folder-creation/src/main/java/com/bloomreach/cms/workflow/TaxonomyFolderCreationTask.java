package com.bloomreach.cms.workflow;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.plugins.standardworkflow.AddDocumentArguments;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.documentworkflow.task.AbstractDocumentTask;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.TaxonomyException;
import org.onehippo.taxonomy.impl.TaxonomyImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("Duplicates")
public class TaxonomyFolderCreationTask extends AbstractDocumentTask {

    public static final String INDEX_DOCTYPE = "myproject:index";
    public static final String TITLE_PROPERTY = "myproject:title";
    public static final String OVERVIEW_PROPERTY = "myproject:overview";
    private static final Logger log = LoggerFactory.getLogger(TaxonomyFolderCreationTask.class);

    @Override
    protected Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        Node published = getDocumentHandle().getDocuments().get("published").getNode();
        if (false /*published.isNodeType("hippotaxonomy:taxonomy")*/) {

            Session session = getWorkflowContext().getInternalWorkflowSession();
            TaxonomyImpl taxonomy = null;
            try {
                taxonomy = new TaxonomyImpl(published);
            } catch (TaxonomyException e) {
                //e.printStackTrace();
            }
            Node cometNode = session.getRootNode().getNode("content/documents/comet");
            for (Locale taxonomyLocale : taxonomy.getLocaleObjects()) {
                log.debug("Starting for taxonomy locale: " + taxonomyLocale.getLanguage());
                if (cometNode.hasNode(taxonomyLocale.getLanguage())) {
                    Node langSpecificNode = cometNode.getNode(taxonomyLocale.getLanguage());
                    checkForTaxonomyCategoryFolder(taxonomy.getCategories(), taxonomyLocale, langSpecificNode);
                }
            }
            session.refresh(false);
        }
        return null;
    }

    private void checkForTaxonomyCategoryFolder(List<? extends Category> categories, Locale taxonomyLocale, Node langSpecificNode) throws RepositoryException, WorkflowException, RemoteException {
        log.debug("Node to operate: " + langSpecificNode.getPath());
        for (Category category : categories) {
            log.debug("Checking category: " + category.getKey());
            if (!langSpecificNode.hasNode(category.getInfo(taxonomyLocale).getName().toLowerCase().replace(" ", "-"))) {
                Node newTaxonomyFolder = addNewTaxonomyFolder(langSpecificNode, category, taxonomyLocale);
                addNewIndexDocument(newTaxonomyFolder);
                log.debug("New node was added " + newTaxonomyFolder.getPath());
                checkForTaxonomyCategoryFolder(category.getChildren(), taxonomyLocale, newTaxonomyFolder);
            } else {
                Node taxonomyCategoryNode = langSpecificNode.getNode(category.getInfo(taxonomyLocale).getName().toLowerCase().replace(" ", "-"));
                log.debug("Taxonomy folder exists. Moving forward with node :" + taxonomyCategoryNode.getPath());
                checkForTaxonomyCategoryFolder(category.getChildren(), taxonomyLocale, taxonomyCategoryNode);
            }
        }
    }

    private Node addNewTaxonomyFolder(Node langSpecificNode, Category category, Locale taxonomyLocale) throws RepositoryException, WorkflowException, RemoteException {
        WorkflowManager wflManager = ((HippoWorkspace)getWorkflowContext().getInternalWorkflowSession().getWorkspace()).getWorkflowManager();
        FolderWorkflow workflow = (FolderWorkflow)wflManager.getWorkflow("threepane", langSpecificNode);
        TreeMap<String, String> arguments = new TreeMap<>();
        arguments.put("name", category.getInfo(taxonomyLocale).getName().toLowerCase().replace(" ", "-"));
        arguments.put("localName", category.getInfo(taxonomyLocale).getName());
        if (langSpecificNode.hasProperty(HippoTranslationNodeType.LOCALE) && StringUtils.isNotBlank(langSpecificNode.getProperty(HippoTranslationNodeType.LOCALE).getString())) {
            arguments.put(HippoTranslationNodeType.LOCALE, langSpecificNode.getProperty(HippoTranslationNodeType.LOCALE).getString());
        }
        String newFolderPath = workflow.add("new-translated-folder", "hippostd:folder", arguments);
        Node folderNode = getWorkflowContext().getInternalWorkflowSession().getNode(newFolderPath);
        if (!arguments.get("name").equals(arguments.get("localName"))) {
            DefaultWorkflow defaultWorkflow = (DefaultWorkflow)wflManager.getWorkflow("core", folderNode);
            defaultWorkflow.setDisplayName(arguments.get("localName"));
        }
        String[] folderType = new String[]{};
        folderNode.setProperty("hippostd:foldertype", folderType);
        folderNode.getSession().save();
        return folderNode;
    }

    private void addNewIndexDocument(Node newTaxonomyFolder) throws RepositoryException, WorkflowException, RemoteException {
        WorkflowManager wflManager = ((HippoWorkspace)getWorkflowContext().getInternalWorkflowSession().getWorkspace()).getWorkflowManager();
        FolderWorkflow workflow = (FolderWorkflow)wflManager.getWorkflow("threepane", newTaxonomyFolder);
        AddDocumentArguments addDocumentModel = new AddDocumentArguments();
        addDocumentModel.setTargetName("index");
        addDocumentModel.setUriName("index");
        addDocumentModel.setPrototype(INDEX_DOCTYPE);
        TreeMap<String, String> arguments = new TreeMap<>();
        arguments.put("name", "index");
        arguments.put("localName", "index");
        if (StringUtils.isNotBlank(addDocumentModel.getLanguage())) {
            arguments.put(HippoTranslationNodeType.LOCALE, addDocumentModel.getLanguage());
        }
        String path = workflow.add("new-document", addDocumentModel.getPrototype(), arguments);
        Node indexFileNode = getWorkflowContext().getInternalWorkflowSession().getNode(path);
        indexFileNode.setProperty(TITLE_PROPERTY, "Automatically populated Index Title");
        indexFileNode.setProperty(OVERVIEW_PROPERTY, "<p>Automatically populated Index Overview</p>");
        indexFileNode.getSession().save();
        EditableWorkflow editableWorkflow = (EditableWorkflow)wflManager.getWorkflow("editing", indexFileNode.getParent());
        editableWorkflow.commitEditableInstance();
        DocumentWorkflow documentWorkflow = (DocumentWorkflow)wflManager.getWorkflow("default", new Document(indexFileNode.getParent()));
        documentWorkflow.publish();
        indexFileNode.getSession().save();
        indexFileNode.getSession().refresh(false);
    }


}
