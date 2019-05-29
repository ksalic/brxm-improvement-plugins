package org.example.TaxonomyWorkflowListener;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.plugins.standardworkflow.AddDocumentArguments;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.eventbus.HippoEventListenerRegistry;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.repository.modules.DaemonModule;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.TaxonomyException;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.onehippo.taxonomy.impl.TaxonomyImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxonomyWorkflowListener implements DaemonModule {

    private static final Logger log = LoggerFactory.getLogger(TaxonomyWorkflowListener.class);

    public static final String PUBLICATION_INTERACTION = "default:handle:publish";
    //TODO Replace myproject
    public static final String INDEX_DOCTYPE = "myproject:index";
    public static final String TITLE_PROPERTY = "myproject:title";
    public static final String OVERVIEW_PROPERTY = "myproject:overview";

    private Session session;

    @Override
    public void initialize(final Session session) throws RepositoryException {
        this.session = session;
        HippoEventListenerRegistry.get().register(this);
    }

    @Override
    public void shutdown() {
        HippoEventListenerRegistry.get().unregister(this);
    }

    @Subscribe
    public void handleEvent(final HippoWorkflowEvent event) {
        if (event.success() && PUBLICATION_INTERACTION.equals(event.interaction())) {
            postPublish(event);
        }
    }

    private void postPublish(final HippoWorkflowEvent workflowEvent) {
        if(workflowEvent.documentType().equals(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TAXONOMY) && workflowEvent.subjectPath().equals("/content/taxonomies/topics")) {
            try {
                final HippoNode handle = (HippoNode) session.getNodeByIdentifier(workflowEvent.subjectId());
                final Node published = getPublishedVariant(handle);
                final TaxonomyImpl taxonomy = new TaxonomyImpl(published);
                Node cometNode = session.getRootNode().getNode("content/documents/comet");
                for(Locale taxonomyLocale : taxonomy.getLocaleObjects()) {
                    log.debug("Starting for taxonomy locale: "+ taxonomyLocale.getLanguage());
                    if(cometNode.hasNode(taxonomyLocale.getLanguage())) {
                        Node langSpecificNode = cometNode.getNode(taxonomyLocale.getLanguage());
                        checkForTaxonomyCategoryFolder(taxonomy.getCategories(), taxonomyLocale, langSpecificNode);
                    }
                }
                session.refresh(false);
            } catch (RepositoryException | TaxonomyException | RemoteException | WorkflowException e ) {
                log.error("{} was thrown during taxonomy post-publication operation. ", e.getClass().getCanonicalName(), e);
            }
        }
    }

    private void checkForTaxonomyCategoryFolder(List<? extends Category> categories, Locale taxonomyLocale, Node langSpecificNode) throws RepositoryException, WorkflowException, RemoteException {
        log.debug("Node to operate: "+ langSpecificNode.getPath());
        for (Category category : categories) {
            log.debug("Checking category: "+category.getKey());
            if (!langSpecificNode.hasNode(category.getInfo(taxonomyLocale).getName().toLowerCase().replace(" ","-"))) {
                Node newTaxonomyFolder = addNewTaxonomyFolder(langSpecificNode, category, taxonomyLocale);
                addNewIndexDocument(newTaxonomyFolder);
                log.debug("New node was added "+ newTaxonomyFolder.getPath());
                checkForTaxonomyCategoryFolder(category.getChildren(), taxonomyLocale, newTaxonomyFolder);
            } else {
                Node taxonomyCategoryNode = langSpecificNode.getNode(category.getInfo(taxonomyLocale).getName().toLowerCase().replace(" ","-"));
                log.debug("Taxonomy folder exists. Moving forward with node :"+taxonomyCategoryNode.getPath());
                checkForTaxonomyCategoryFolder(category.getChildren(), taxonomyLocale, taxonomyCategoryNode);
            }
        }
    }

    private Node addNewTaxonomyFolder(Node langSpecificNode, Category category, Locale taxonomyLocale) throws RepositoryException, WorkflowException, RemoteException {
        WorkflowManager wflManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        FolderWorkflow workflow = (FolderWorkflow) wflManager.getWorkflow( "threepane", langSpecificNode);
        TreeMap<String, String> arguments = new TreeMap<>();
        arguments.put("name", category.getInfo(taxonomyLocale).getName().toLowerCase().replace(" ","-"));
        arguments.put("localName", category.getInfo(taxonomyLocale).getName());
        if (langSpecificNode.hasProperty(HippoTranslationNodeType.LOCALE) && StringUtils.isNotBlank(langSpecificNode.getProperty(HippoTranslationNodeType.LOCALE).getString())) {
            arguments.put(HippoTranslationNodeType.LOCALE, langSpecificNode.getProperty(HippoTranslationNodeType.LOCALE).getString());
        }
        String newFolderPath = workflow.add("new-translated-folder","hippostd:folder", arguments);
        Node folderNode = session.getNode(newFolderPath);
        if (!arguments.get("name").equals(arguments.get("localName"))) {
            DefaultWorkflow defaultWorkflow = (DefaultWorkflow) wflManager.getWorkflow("core", folderNode);
            defaultWorkflow.setDisplayName(arguments.get("localName"));
        }
        String[] folderType = new String[]{};
        folderNode.setProperty("hippostd:foldertype", folderType);
        session.save();
        return folderNode;
    }

    private void addNewIndexDocument(Node newTaxonomyFolder) throws RepositoryException, WorkflowException, RemoteException {
        WorkflowManager wflManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        FolderWorkflow workflow = (FolderWorkflow) wflManager.getWorkflow( "threepane", newTaxonomyFolder);
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
        Node indexFileNode = session.getNode(path);
        indexFileNode.setProperty(TITLE_PROPERTY,"Automatically populated Index Title");
        indexFileNode.setProperty(OVERVIEW_PROPERTY,"<p>Automatically populated Index Overview</p>");
        session.save();
        EditableWorkflow editableWorkflow = (EditableWorkflow) wflManager.getWorkflow("editing", indexFileNode.getParent());
        editableWorkflow.commitEditableInstance();
        DocumentWorkflow documentWorkflow = (DocumentWorkflow) wflManager.getWorkflow("default", new Document(indexFileNode.getParent()));
        documentWorkflow.publish();
        session.save();
        session.refresh(false);
    }

    private static Node getPublishedVariant(Node handle) throws RepositoryException {
        for (Node variant : new NodeIterable(handle.getNodes(handle.getName()))) {
            final String state = JcrUtils.getStringProperty(variant, HippoStdNodeType.HIPPOSTD_STATE, null);
            if (HippoStdNodeType.PUBLISHED.equals(state)) {
                return variant;
            }
        }
        return null;
    }

}