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

package org.jboss.legacy.ejb3.remoting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.legacy.ejb3.registrar.EJB3RegistrarService;
import org.jboss.legacy.ejb3.registrar.EJB3RegistrarServiceAddStepHandler;
import org.jboss.legacy.jnp.server.JNPServerService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting.transport.Connector;

/**
 * @author baranowb
 *
 */
public class RemotingServiceAddStepHandler extends AbstractBoottimeAddStepHandler {

    public static final RemotingServiceAddStepHandler INSTANCE = new RemotingServiceAddStepHandler();
    public RemotingServiceAddStepHandler() {
        super(RemotingResourceDefinition.HOST,RemotingResourceDefinition.PORT);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        newControllers.addAll(this.installRuntimeServices(context, operation, model, verificationHandler));
    }

    Collection<ServiceController<?>> installRuntimeServices(final OperationContext context,final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        final String host = RemotingResourceDefinition.HOST.resolveModelAttribute(context, operation).asString();
        final int port = RemotingResourceDefinition.PORT.resolveModelAttribute(context, operation).asInt();
        final RemotingConnectorService service = new RemotingConnectorService(host, port);

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ServiceBuilder<Connector> serviceBuilder = serviceTarget.addService(service.SERVICE_NAME, service);
        serviceBuilder.addDependency(JNPServerService.SERVICE_NAME);
        if (verificationHandler != null) {
            serviceBuilder.addListener(verificationHandler);
        }
        final ServiceController<Connector> remotingServiceController = serviceBuilder.install();
        final List<ServiceController<?>> installedServices = new ArrayList<ServiceController<?>>();
        installedServices.add(remotingServiceController);
        return installedServices;
    }
}
