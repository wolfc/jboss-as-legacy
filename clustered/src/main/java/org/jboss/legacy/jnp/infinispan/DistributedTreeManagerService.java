/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.legacy.jnp.infinispan;

import javax.naming.NamingException;
import org.infinispan.Cache;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.legacy.jnp.server.NamingStoreWrapper;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 *
 * @author ehsavoie
 */
public class DistributedTreeManagerService implements Service<InfinispanDistributedTreeManager> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(DistributedTreeManagerModel.LEGACY).append(
            DistributedTreeManagerModel.SERVICE_NAME);

    private InfinispanDistributedTreeManager treeManager;
    private InjectedValue<Cache> cache = new InjectedValue<Cache>();

    @Override
    public void start(StartContext context) throws StartException {
            this.treeManager = new InfinispanDistributedTreeManager();
            this.treeManager.setClusteredCache(cache.getValue());
    }

    @Override
    public void stop(StopContext context) {
        this.treeManager = null;
    }

    @Override
    public InfinispanDistributedTreeManager getValue() throws IllegalStateException, IllegalArgumentException {
        return this.treeManager;
    }

    public InjectedValue<Cache> getCache() {
        return cache;
    }
}
