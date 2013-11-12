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

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.legacy.jnp.server.JNPServer;
import org.jboss.legacy.jnp.server.JNPServerService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jnp.server.Main;

/**
 * @author baranowb
 */
public class JNPServerConnectorServiceAddStepHandler extends AbstractBoottimeAddStepHandler {

    public static final JNPServerConnectorServiceAddStepHandler INSTANCE = new JNPServerConnectorServiceAddStepHandler();

    public JNPServerConnectorServiceAddStepHandler() {
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        newControllers.addAll(this.installRuntimeServices(context, operation, model, verificationHandler));
    }

    Collection<ServiceController<?>> installRuntimeServices(final OperationContext context, final ModelNode operation,
            final ModelNode model, final ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        final String host = JNPServerConnectorResourceDefinition.HOST.resolveModelAttribute(context, operation).asString();
        final int port = JNPServerConnectorResourceDefinition.PORT.resolveModelAttribute(context, operation).asInt();
        final JNPServerConnectorService service = new JNPServerConnectorService(host,port);

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ServiceBuilder<Main> serviceBuilder = serviceTarget.addService(service.SERVICE_NAME, service);
        serviceBuilder.addDependency(JNPServerService.SERVICE_NAME,JNPServer.class,service.getJnjectedValueJnpServer());
        if (verificationHandler != null) {
            serviceBuilder.addListener(verificationHandler);
        }
        final ServiceController<Main> remotingServiceController = serviceBuilder.install();
        final List<ServiceController<?>> installedServices = new ArrayList<ServiceController<?>>();
        installedServices.add(remotingServiceController);
        return installedServices;
    }
}
