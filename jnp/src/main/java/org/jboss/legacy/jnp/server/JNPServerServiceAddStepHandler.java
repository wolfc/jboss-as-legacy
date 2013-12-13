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
package org.jboss.legacy.jnp.server;

import org.jboss.legacy.jnp.server.clustered.HAServerService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.ha.jndi.HANamingService;
import org.jboss.legacy.jnp.connector.JNPServerNamingConnectorService;
import org.jboss.legacy.jnp.server.simple.SingleServerService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author baranowb
 */
public class JNPServerServiceAddStepHandler extends AbstractBoottimeAddStepHandler {

    public static final JNPServerServiceAddStepHandler INSTANCE = new JNPServerServiceAddStepHandler();

    public JNPServerServiceAddStepHandler() {
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        newControllers.addAll(this.installRuntimeServices(context, operation, model, verificationHandler));
    }

    Collection<ServiceController<?>> installRuntimeServices(final OperationContext context, final ModelNode operation,
            final ModelNode model, final ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        final boolean isHA = JNPServerResourceDefinition.HA.resolveModelAttribute(context, operation).asBoolean(false);
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ServiceBuilder<?> serviceBuilder;
        if (isHA) {
            HAServerService service = new HAServerService();
            serviceBuilder = serviceTarget.addService(JNPServerService.SERVICE_NAME, service);
            serviceBuilder.addDependency(JNPServerNamingConnectorService.SERVICE_NAME, HANamingService.class, ((HAServerService) service).getHaNamingService());
        } else {
            SingleServerService service = new SingleServerService();
            serviceBuilder = serviceTarget.addService(JNPServerService.SERVICE_NAME, service);
            serviceBuilder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, ((SingleServerService) service).getNamingStoreInjector());
        }
        if (verificationHandler != null) {
            serviceBuilder.addListener(verificationHandler);
        }
        final ServiceController<?> remotingServiceController = serviceBuilder.install();
        final List<ServiceController<?>> installedServices = new ArrayList<ServiceController<?>>();
        installedServices.add(remotingServiceController);
        return installedServices;
    }
    
    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
    }
}
