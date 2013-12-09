/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.legacy.jnp.connector.clustered;

import org.jboss.as.clustering.impl.CoreGroupCommunicationService;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.network.SocketBinding;
import org.jboss.ha.jndi.HANamingService;
import org.jboss.legacy.jnp.connector.JNPServerNamingConnectorService;
import org.jboss.legacy.jnp.infinispan.InfinispanDistributedTreeManager;
import org.jboss.legacy.jnp.infinispan.InfinispanHAPartition;
import org.jboss.legacy.jnp.server.NamingStoreWrapper;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author baranowb
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class HAConnectorService implements JNPServerNamingConnectorService<HANamingService> {

    private final InjectedValue<InfinispanDistributedTreeManager> distributedTreeManager = new InjectedValue<InfinispanDistributedTreeManager>();
    private final InjectedValue<CoreGroupCommunicationService> coreGroupCommunicationService = new InjectedValue<CoreGroupCommunicationService>();
    private final InjectedValue<SocketBinding> binding = new InjectedValue<SocketBinding>();
    private final InjectedValue<SocketBinding> rmiBinding = new InjectedValue<SocketBinding>();
    private final InjectedValue<ServiceBasedNamingStore> namingStoreValue = new InjectedValue<ServiceBasedNamingStore>();

    private NamingStoreWrapper singletonNamingServer;
    private HANamingService haNamingService;

    public HAConnectorService() {
        super();
    }

    public InjectedValue<InfinispanDistributedTreeManager> getDistributedTreeManager() {
        return distributedTreeManager;
    }

    public InjectedValue<CoreGroupCommunicationService> getCoreGroupCommunicationService() {
        return coreGroupCommunicationService;
    }

    @Override
    public InjectedValue<SocketBinding> getBinding() {
        return binding;
    }

    @Override
    public InjectedValue<SocketBinding> getRmiBinding() {
        return rmiBinding;
    }

    public InjectedValue<ServiceBasedNamingStore> getNamingStoreValue() {
        return namingStoreValue;
    }

    @Override
    public HANamingService getValue() throws IllegalStateException, IllegalArgumentException {
        return this.haNamingService;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        try {
            this.haNamingService = new HANamingService();
            this.singletonNamingServer = new NamingStoreWrapper(namingStoreValue.getValue());
            InfinispanHAPartition partition = new InfinispanHAPartition(coreGroupCommunicationService.getValue());
            this.haNamingService.setHAPartition(partition);
            this.haNamingService.setDistributedTreeManager(distributedTreeManager.getValue());
            this.haNamingService.setLocalNamingInstance(singletonNamingServer);

            if (this.getRmiBinding().getOptionalValue() != null) {
                haNamingService.setRmiBindAddress(this.getRmiBinding().getValue().getAddress().getHostName());
                haNamingService.setRmiPort(this.getRmiBinding().getValue().getAbsolutePort());
            }
            haNamingService.setBindAddress(this.getBinding().getValue().getAddress().getHostName());
            haNamingService.setPort(this.getBinding().getValue().getAbsolutePort());
            partition.start();
            haNamingService.create();
            haNamingService.start();
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        this.haNamingService.stop();
        this.haNamingService = null;
    }
}
