/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.legacy.jnp.infinispan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.infinispan.Cache;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * 
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a>  (c) 2013 Red Hat, inc.
 */
public class DistributedTreeManagerServiceAddStepHandler extends AbstractBoottimeAddStepHandler {

    public static final DistributedTreeManagerServiceAddStepHandler INSTANCE = new DistributedTreeManagerServiceAddStepHandler();

    public DistributedTreeManagerServiceAddStepHandler() {
        super(DistributedTreeManagerResourceDefinition.CACHE_CONTAINER, DistributedTreeManagerResourceDefinition.CACHE_REF);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        newControllers.addAll(this.installRuntimeServices(context, operation, model, verificationHandler));
    }

    Collection<ServiceController<?>> installRuntimeServices(final OperationContext context, final ModelNode operation,
            final ModelNode model, final ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        final String cacheRef = DistributedTreeManagerResourceDefinition.CACHE_REF.resolveModelAttribute(context, operation).asString();
        final String containerRef = DistributedTreeManagerResourceDefinition.CACHE_CONTAINER.resolveModelAttribute(context, operation).asString();
        final DistributedTreeManagerService service = new DistributedTreeManagerService();
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ServiceBuilder<InfinispanDistributedTreeManager> serviceBuilder = serviceTarget.addService(DistributedTreeManagerService.SERVICE_NAME, service);
        serviceBuilder.addDependency(CacheService.getServiceName(containerRef, cacheRef), Cache.class, service.getCache());
        final ServiceController<InfinispanDistributedTreeManager> distributedTreeManagerController = serviceBuilder.install();
        final List<ServiceController<?>> installedServices = new ArrayList<ServiceController<?>>();
        installedServices.add(distributedTreeManagerController);
        return installedServices;
    }
}
