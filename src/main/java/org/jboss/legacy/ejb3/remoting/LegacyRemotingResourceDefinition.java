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

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.legacy.ejb3.LegacyEJB3Extension;

/**
 * @author baranowb
 */
public class LegacyRemotingResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition HOST = new SimpleAttributeDefinitionBuilder(LegacyRemotingModel.HOST,
            ModelType.STRING)
    .setDefaultValue(new ModelNode().set("0.0.0.0"))
    //.setXmlName(XXX.HOST.getLocalName())
    .setAllowExpression(true)
    .setAllowNull(true)
    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
    .build();

    public static final SimpleAttributeDefinition PORT = new SimpleAttributeDefinitionBuilder(LegacyRemotingModel.PORT,
            ModelType.INT)
    .setDefaultValue(new ModelNode().set(4873))
    //.setXmlName(XXX.PORT.getLocalName())
    .setAllowExpression(true)
    .setAllowNull(true)
    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
    .build();

    public static final LegacyRemotingResourceDefinition INSTANCE = new LegacyRemotingResourceDefinition();

    private LegacyRemotingResourceDefinition() {
        super(PathElement.pathElement(LegacyRemotingModel.SERVICE, LegacyRemotingModel.SERVICE_NAME), LegacyEJB3Extension
                .getResourceDescriptionResolver(LegacyRemotingModel.SERVICE_NAME), LegacyRemotingServiceAddStepHandler.INSTANCE,
                LegacyRemotingServiceRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        final ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(HOST,PORT);
        resourceRegistration.registerReadWriteAttribute(HOST, null, handler);
        resourceRegistration.registerReadWriteAttribute(PORT, null, handler);
    }
}