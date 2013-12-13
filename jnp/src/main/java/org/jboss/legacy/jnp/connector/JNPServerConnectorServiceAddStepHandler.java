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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jboss.as.clustering.impl.CoreGroupCommunicationService;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.legacy.jnp.connector.clustered.HAConnectorService;
import org.jboss.legacy.jnp.connector.simple.SingleConnectorService;
import org.jboss.legacy.jnp.infinispan.DistributedTreeManagerService;
import org.jboss.legacy.jnp.infinispan.InfinispanDistributedTreeManager;
import org.jboss.legacy.jnp.server.JNPServer;
import org.jboss.legacy.jnp.server.JNPServerService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author baranowb
 */
public class JNPServerConnectorServiceAddStepHandler extends AbstractBoottimeAddStepHandler {

    public static final JNPServerConnectorServiceAddStepHandler INSTANCE = new JNPServerConnectorServiceAddStepHandler();

    public JNPServerConnectorServiceAddStepHandler() {
        super(JNPServerConnectorResourceDefinition.SOCKET_BINDING, JNPServerConnectorResourceDefinition.RMI_SOCKET_BINDING);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        newControllers.addAll(this.installRuntimeServices(context, operation, model, verificationHandler));
    }

    Collection<ServiceController<?>> installRuntimeServices(final OperationContext context, final ModelNode operation,
            final ModelNode model, final ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        final ModelNode bindingRefModel = JNPServerConnectorResourceDefinition.SOCKET_BINDING.resolveModelAttribute(context, operation);
        final ModelNode containerRef = JNPServerConnectorResourceDefinition.CACHE_CONTAINER.resolveModelAttribute(context, operation);
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ServiceBuilder<JNPServerNamingConnectorService<?>> serviceBuilder;
        final JNPServerNamingConnectorService service;
        if (containerRef.isDefined()) {
            service = new HAConnectorService();
            final HAConnectorService haConnectorService = (HAConnectorService) service;
            serviceBuilder = serviceTarget.addService(JNPServerNamingConnectorService.SERVICE_NAME, service);
            serviceBuilder.addDependency(CoreGroupCommunicationService.getServiceName(containerRef.asString()), CoreGroupCommunicationService.class, haConnectorService.getCoreGroupCommunicationService())
                    .addDependency(DistributedTreeManagerService.SERVICE_NAME, InfinispanDistributedTreeManager.class, haConnectorService.getDistributedTreeManager())
                    .addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, haConnectorService.getNamingStoreValue());
        } else {
            service = new SingleConnectorService();
            serviceBuilder = serviceTarget.addService(JNPServerNamingConnectorService.SERVICE_NAME, service);
            serviceBuilder.addDependency(JNPServerService.SERVICE_NAME, JNPServer.class, ((SingleConnectorService) service).getJNPServer());
        }
        serviceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(bindingRefModel.asString()), SocketBinding.class, service.getBinding());
        ModelNode rmiBindingRefModel = JNPServerConnectorResourceDefinition.RMI_SOCKET_BINDING.resolveModelAttribute(context, operation);
        if (rmiBindingRefModel.isDefined()) {
            serviceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(rmiBindingRefModel.asString()), SocketBinding.class, service.getRmiBinding());
        } else {
            service.getRmiBinding().inject(null);
        }
        if (verificationHandler != null) {
            serviceBuilder.addListener(verificationHandler);
        }
        final ServiceController<?> remotingServiceController = serviceBuilder.install();
        final List<ServiceController<?>> installedServices = new ArrayList<ServiceController<?>>();
        installedServices.add(remotingServiceController);
        return installedServices;
    }

}
