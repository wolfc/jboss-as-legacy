/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jboss.legacy.jnp.infinispan;

import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.ResponseFilter;

/**
 *
 * @author ehsavoie
 */
public class ResponseFilterAdapter implements ResponseFilter {

    private final org.jboss.ha.framework.interfaces.ResponseFilter filter;

    public ResponseFilterAdapter(org.jboss.ha.framework.interfaces.ResponseFilter filter) {
        this.filter = filter;
    }

    @Override
    public boolean isAcceptable(Object response, ClusterNode sender) {
        return filter.isAcceptable(response, new LegacyClusterNodeAdapter(sender));
    }

    @Override
    public boolean needMoreResponses() {
        return this.filter.needMoreResponses();
    }

}
