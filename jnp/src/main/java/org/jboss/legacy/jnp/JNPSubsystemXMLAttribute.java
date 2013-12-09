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

import java.util.HashMap;
import java.util.Map;

import org.jboss.legacy.jnp.connector.JNPServerConnectorModel;
import org.jboss.legacy.jnp.infinispan.DistributedTreeManagerModel;

/**
 * @author baranowb
 *
 */
public enum JNPSubsystemXMLAttribute {
    UNKNOWN(null),
    SOCKET_BINDING(JNPServerConnectorModel.SOCKET_BINDING),
    RMI_SOCKET_BINDING(JNPServerConnectorModel.RMI_SOCKET_BINDING),
    CACHE_REF(DistributedTreeManagerModel.CACHE_REF),
    CACHE_CONTAINER(DistributedTreeManagerModel.CACHE_CONTAINER);

    private final String name;

    JNPSubsystemXMLAttribute(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this attribute.
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, JNPSubsystemXMLAttribute> MAP;

    static {
        final Map<String, JNPSubsystemXMLAttribute> map = new HashMap<String, JNPSubsystemXMLAttribute>();
        for (JNPSubsystemXMLAttribute element : values()) {
            final String name = element.getLocalName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    public static JNPSubsystemXMLAttribute forName(String localName) {
        final JNPSubsystemXMLAttribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    @Override
    public String toString() {
        return getLocalName();
    }
}
