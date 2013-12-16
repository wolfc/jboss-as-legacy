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

package org.jboss.legacy.jnp;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.legacy.jnp.connector.JNPServerConnectorResourceDefinition;
import org.jboss.legacy.jnp.infinispan.DistributedTreeManagerResourceDefinition;
import org.jboss.legacy.jnp.remoting.RemotingResourceDefinition;
import org.jboss.legacy.jnp.server.JNPServerResourceDefinition;

/**
 * @author baranowb
 *
 */
public class JNPSubsystemRootResourceDefinition extends SimpleResourceDefinition {

    public static final JNPSubsystemRootResourceDefinition INSTANCE = new JNPSubsystemRootResourceDefinition();

    JNPSubsystemRootResourceDefinition() {
        super(JNPExtension.SUBSYSTEM_PATH,
                JNPExtension.getResourceDescriptionResolver(JNPExtension.SUBSYSTEM_NAME),
                JNPSubsystemAdd.INSTANCE, JNPSubsystemRemove.INSTANCE, OperationEntry.Flag.RESTART_ALL_SERVICES,
                OperationEntry.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(JNPServerResourceDefinition.INSTANCE);
        resourceRegistration.registerSubModel(JNPServerConnectorResourceDefinition.INSTANCE);
        resourceRegistration.registerSubModel(DistributedTreeManagerResourceDefinition.INSTANCE);
        resourceRegistration.registerSubModel(RemotingResourceDefinition.INSTANCE);
    }
}
