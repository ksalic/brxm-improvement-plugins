package org.example.components;

import java.util.LinkedHashMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.onehippo.cms7.essentials.components.EssentialsContentComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CategoryPageComponent extends EssentialsContentComponent {

    private static Logger log = LoggerFactory.getLogger(CategoryPageComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);
        String relativePath = request.getRequestContext().getResolvedSiteMapItem().getRelativeContentPath();
        String siteContentPath = request.getRequestContext().getSiteContentBaseBean().getPath();
        LinkedHashMap<String,String> folderLinks = new LinkedHashMap<>();
        try {
            Node siteContentNode = request.getRequestContext().getSession().getNode(siteContentPath);
            Node relativeNode = siteContentNode.getNode(relativePath);
            NodeIterator nextFoldersIter = null;
            if(relativeNode.isNodeType("hippo:handle")){
                nextFoldersIter  = relativeNode.getParent().getNodes();
            } else if (relativeNode.isNodeType("hippostd:folder")){
                nextFoldersIter = relativeNode.getNodes();
            }

            while(nextFoldersIter.hasNext()){
                Node nextSibling = nextFoldersIter.nextNode();
                if(nextSibling.isNodeType("hippostd:folder")) {
                    HstLink folderLink = request.getRequestContext().getHstLinkCreator().create(nextSibling, request.getRequestContext().getResolvedMount().getMount());
                    String folderUrlString = folderLink.toUrlForm(request.getRequestContext(),true);
                    folderLinks.put(nextSibling.getName(),folderUrlString);
                }
            }

        } catch (RepositoryException e) {
            log.error("{} was thrown while trying to retrieve links to folders for Category Page. ",e.getClass().getCanonicalName(),e);
        }
        if(!folderLinks.isEmpty()){
            request.setAttribute("folderLinks",folderLinks);
            request.setModel("folderLinks",folderLinks);
        }
    }
}
