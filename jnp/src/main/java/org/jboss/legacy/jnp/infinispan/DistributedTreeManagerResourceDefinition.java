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
package org.jboss.legacy.jnp.infinispan;

import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
import org.jboss.legacy.jnp.JNPExtension;
import static org.jboss.legacy.jnp.infinispan.DistributedTreeManagerModel.DISTRIBUTED_TREE_PATH;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class DistributedTreeManagerResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition CACHE_CONTAINER
            = new SimpleAttributeDefinitionBuilder(DistributedTreeManagerModel.CACHE_CONTAINER, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition CACHE_REF
            = new SimpleAttributeDefinitionBuilder(DistributedTreeManagerModel.CACHE_REF, ModelType.STRING, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1, true))
            .build();

    public static final DistributedTreeManagerResourceDefinition INSTANCE = new DistributedTreeManagerResourceDefinition();

    private DistributedTreeManagerResourceDefinition() {
        super(DISTRIBUTED_TREE_PATH, JNPExtension.getResourceDescriptionResolver(DistributedTreeManagerModel.SERVICE_NAME), 
                DistributedTreeManagerServiceAddStepHandler.INSTANCE, DistributedTreeManagerServiceRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        final ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(CACHE_REF, CACHE_CONTAINER);
        resourceRegistration.registerReadWriteAttribute(CACHE_REF, null, handler);
        resourceRegistration.registerReadWriteAttribute(CACHE_CONTAINER, null, handler);
    }
}
