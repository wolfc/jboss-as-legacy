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
    private final InjectedValue<ServiceBasedNamingStore> namingStoreValue = new InjectedValue<ServiceBasedNamingStore>();

    private NamingStoreWrapper singletonNamingServer;

    @Override
    public void start(StartContext context) throws StartException {
        try {
            this.treeManager = new InfinispanDistributedTreeManager();
            this.treeManager.setClusteredCache(cache.getValue());
            this.singletonNamingServer = new NamingStoreWrapper(namingStoreValue.getValue());
            this.treeManager.setHAStub(singletonNamingServer);
            this.treeManager.init();
        } catch (NamingException e) {
            throw new StartException(e);
        }
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

    public InjectedValue<ServiceBasedNamingStore> getNamingStoreValue() {
        return namingStoreValue;
    }

}
