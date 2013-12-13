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

import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
import static org.jboss.dmr.ModelType.STRING;
import org.jboss.legacy.jnp.JNPExtension;
import static org.jboss.legacy.jnp.connector.JNPServerConnectorModel.JNP_CONNECTOR_PATH;
import org.jboss.legacy.jnp.infinispan.DistributedTreeManagerModel;

/**
 * @author baranowb
 */
public class JNPServerConnectorResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition SOCKET_BINDING = create(JNPServerConnectorModel.SOCKET_BINDING, STRING)
            .setAllowNull(false)
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    public static final SimpleAttributeDefinition RMI_SOCKET_BINDING
            = new SimpleAttributeDefinitionBuilder(JNPServerConnectorModel.RMI_SOCKET_BINDING, ModelType.STRING)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    public static final SimpleAttributeDefinition CACHE_CONTAINER
            = new SimpleAttributeDefinitionBuilder(DistributedTreeManagerModel.CACHE_CONTAINER, ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final JNPServerConnectorResourceDefinition INSTANCE = new JNPServerConnectorResourceDefinition();

    private JNPServerConnectorResourceDefinition() {
        super(JNP_CONNECTOR_PATH, JNPExtension.getResourceDescriptionResolver(JNPServerConnectorModel.SERVICE_NAME), 
                JNPServerConnectorServiceAddStepHandler.INSTANCE, JNPServerConnectorServiceRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        final ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(SOCKET_BINDING, RMI_SOCKET_BINDING, CACHE_CONTAINER);
        resourceRegistration.registerReadWriteAttribute(SOCKET_BINDING, null, handler);
        resourceRegistration.registerReadWriteAttribute(RMI_SOCKET_BINDING, null, handler);
        resourceRegistration.registerReadWriteAttribute(CACHE_CONTAINER, null, handler);
    }
}
