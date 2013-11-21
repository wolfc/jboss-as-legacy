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
package org.jboss.legacy.jnp.connector;

import org.jboss.as.clustering.impl.CoreGroupCommunicationService;
import org.jboss.as.network.SocketBinding;
import org.jboss.ha.jndi.HANamingService;
import org.jboss.legacy.jnp.infinispan.InfinispanDistributedTreeManager;
import org.jboss.legacy.jnp.infinispan.InfinispanHAPartition;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author baranowb
 */
public class JNPServerConnectorService implements Service<HANamingService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(JNPServerConnectorModel.LEGACY).append(
            JNPServerConnectorModel.SERVICE_NAME);

    private InjectedValue<InfinispanDistributedTreeManager> distributedTreeManager = new InjectedValue<InfinispanDistributedTreeManager>();
    private final InjectedValue<CoreGroupCommunicationService> coreGroupCommunicationService = new InjectedValue<CoreGroupCommunicationService>();
    private final InjectedValue<SocketBinding> binding = new InjectedValue<SocketBinding>();
    private final InjectedValue<SocketBinding> rmiBinding = new InjectedValue<SocketBinding>();

    private HANamingService haNamingService;

    public JNPServerConnectorService() {
        super();

    }

    public InjectedValue<InfinispanDistributedTreeManager> getDistributedTreeManager() {
        return distributedTreeManager;
    }

    public InjectedValue<CoreGroupCommunicationService> getCoreGroupCommunicationService() {
        return coreGroupCommunicationService;
    }

    public InjectedValue<SocketBinding> getBinding() {
        return binding;
    }

    public InjectedValue<SocketBinding> getRmiBinding() {
        return rmiBinding;
    }

    @Override
    public HANamingService getValue() throws IllegalStateException, IllegalArgumentException {
        return this.haNamingService;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        this.haNamingService = new HANamingService();
        this.haNamingService.setHAPartition(new InfinispanHAPartition(coreGroupCommunicationService.getValue()));
        this.haNamingService.setDistributedTreeManager(distributedTreeManager.getValue());
        this.haNamingService.setLocalNamingInstance(getDistributedTreeManager().getValue().getHAStub());
        try {
            if (this.getRmiBinding().getOptionalValue() != null) {
                haNamingService.setRmiBindAddress(this.getRmiBinding().getValue().getAddress().getHostName());
                haNamingService.setRmiPort(this.getRmiBinding().getValue().getAbsolutePort());
            }
            haNamingService.setBindAddress(this.getBinding().getValue().getAddress().getHostName());
            haNamingService.setPort(this.getBinding().getValue().getAbsolutePort());
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
