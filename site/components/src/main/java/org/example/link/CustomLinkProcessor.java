package org.example.link;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.linking.HstLinkProcessorTemplate;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyManager;

public class CustomLinkProcessor extends HstLinkProcessorTemplate {

    @Override
    protected HstLink doPostProcess(final HstLink hstLink) {

        if(StringUtils.isNotEmpty(hstLink.getHstSiteMapItem().getComponentConfigurationId()) && hstLink.getHstSiteMapItem().getComponentConfigurationId().equals("hst:pages/articledetailpage")) {
            TaxonomyManager taxonomyManager = HstServices.getComponentManager().getComponent(TaxonomyManager.class.getName());
            Taxonomy topicsTaxonomy = taxonomyManager.getTaxonomies().getTaxonomy("topics");

            String path = hstLink.getPath();
            int indexOfLastSlash = path.lastIndexOf("/");
            String articleSegment = path.substring(indexOfLastSlash+1);
            path = path.substring(0, indexOfLastSlash);
            String[] pathSegments = path.split("/", 2);
            String languageSegment = pathSegments[0];
            String uncleanedPath = pathSegments[1];
            Locale pathLocale = new Locale(languageSegment);
            List<String> pathSegmentsList = new ArrayList<>(Arrays.asList(uncleanedPath.split("/")));

            findNonTaxonomySegments(pathSegmentsList, topicsTaxonomy.getCategories(), pathLocale);

            if(!pathSegmentsList.isEmpty()){
                String nonTaxonomySegments = StringUtils.join(pathSegmentsList,"/");
                String cleanedTaxonomyPath = uncleanedPath.replace(nonTaxonomySegments, "");
                hstLink.setPath(languageSegment+"/"+cleanedTaxonomyPath+articleSegment);
            }

        }
        return hstLink;
    }

    private void findNonTaxonomySegments(List<String> segments, List<? extends Category> parentTaxonomyCategories, Locale locale){
        List<? extends Category> childrenTaxonomyCategories = null;
        for(Category category: parentTaxonomyCategories){
            if(!segments.isEmpty() && segments.get(0).equals(category.getInfo(locale).getName().toLowerCase().replace(" ","-"))){
                segments.remove(0);
                childrenTaxonomyCategories = category.getChildren();
                break;
            }
        }
        if(childrenTaxonomyCategories != null && !childrenTaxonomyCategories.isEmpty()) {
            findNonTaxonomySegments(segments, childrenTaxonomyCategories, locale);
        }
    }

    @Override
    protected HstLink doPreProcess(final HstLink hstLink) {
        //TODO Here will be required a logic to put back the subfolders after taxonomy. Subfolders will have to follow some pattern to be easy to populate.
        String path = hstLink.getPath();
        if(path.contains("article") && path.contains("2019")){
            int lastSlash = path.lastIndexOf("/");
            String firstPart = path.substring(0,lastSlash);
            String article = path.substring(lastSlash+1);
            String[] articleParts = article.split("-");
            String relativeContentPath = StringUtils.join(new String[]{firstPart,articleParts[1],articleParts[2],article},"/");
            hstLink.setPath(relativeContentPath);
        }
        return hstLink;
    }
}