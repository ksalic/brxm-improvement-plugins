package org.example.beans;

import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;
import org.hippoecm.hst.content.beans.Node;

@HippoEssentialsGenerated(internalName = "myproject:index")
@Node(jcrType = "myproject:index")
public class Index extends BaseDocument {
    @HippoEssentialsGenerated(internalName = "myproject:title")
    public String getTitle() {
        return getProperty("myproject:title");
    }

    @HippoEssentialsGenerated(internalName = "myproject:overview")
    public String getOverview() {
        return getProperty("myproject:overview");
    }
}
