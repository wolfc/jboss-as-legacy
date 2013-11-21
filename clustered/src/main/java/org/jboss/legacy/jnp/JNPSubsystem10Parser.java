/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.legacy.jnp.connector.JNPServerConnectorModel;
import org.jboss.legacy.jnp.connector.JNPServerConnectorResourceDefinition;
import org.jboss.legacy.jnp.infinispan.DistributedTreeManagerModel;
import org.jboss.legacy.jnp.infinispan.DistributedTreeManagerResourceDefinition;
import org.jboss.legacy.jnp.server.JNPServerModel;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 */
public class JNPSubsystem10Parser implements XMLElementReader<List<ModelNode>> {

    public static final JNPSubsystem10Parser INSTANCE = new JNPSubsystem10Parser();

    protected JNPSubsystem10Parser() {
    }

    @Override
    public void readElement(final XMLExtendedStreamReader xmlExtendedStreamReader, final List<ModelNode> result)
            throws XMLStreamException {
        // no attributes
        requireNoAttributes(xmlExtendedStreamReader);
        final ModelNode jnpSubsystemAddOperation = Util.createAddOperation();
        jnpSubsystemAddOperation.get(OP_ADDR).add(SUBSYSTEM, JNPExtension.SUBSYSTEM_NAME);
        result.add(jnpSubsystemAddOperation);

        final ModelNode jnpServerServiceAddOperation = Util.createAddOperation();
        jnpServerServiceAddOperation.get(OP_ADDR).add(SUBSYSTEM, JNPExtension.SUBSYSTEM_NAME)
                .add(JNPServerModel.SERVICE, JNPServerModel.SERVICE_NAME);
        result.add(jnpServerServiceAddOperation);

        final ModelNode jnpServerConnectorServiceAddOperation = Util.createAddOperation();
        jnpServerConnectorServiceAddOperation.get(OP_ADDR).add(SUBSYSTEM, JNPExtension.SUBSYSTEM_NAME)
                .add(JNPServerConnectorModel.SERVICE, JNPServerConnectorModel.SERVICE_NAME);
        result.add(jnpServerConnectorServiceAddOperation);

        final ModelNode distributedTreManagerServiceAddOperation = Util.createAddOperation();
        distributedTreManagerServiceAddOperation.get(OP_ADDR).add(SUBSYSTEM, JNPExtension.SUBSYSTEM_NAME)
                .add(DistributedTreeManagerModel.SERVICE, DistributedTreeManagerModel.SERVICE_NAME);
        result.add(distributedTreManagerServiceAddOperation);

        // elements
        final EnumSet<JNPSubsystemXMLElement> encountered = EnumSet.noneOf(JNPSubsystemXMLElement.class);
        while (xmlExtendedStreamReader.hasNext() && xmlExtendedStreamReader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (JNPSubsystemNamespace.forUri(xmlExtendedStreamReader.getNamespaceURI()) != JNPSubsystemNamespace.LEGACY_JNP_1_0) {
                throw unexpectedElement(xmlExtendedStreamReader);
            }
            final JNPSubsystemXMLElement element = JNPSubsystemXMLElement.forName(xmlExtendedStreamReader
                    .getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(xmlExtendedStreamReader);
            }
            switch (element) {
                case JNP_CONNECTOR:
                    this.parseJNPConnector(xmlExtendedStreamReader, jnpServerConnectorServiceAddOperation/*, distributedTreManagerServiceAddOperation*/);
                    break;
                case JNP_SERVER:
                    this.parseJNPServer(xmlExtendedStreamReader, jnpServerServiceAddOperation);
                    break;
                case DISTRIBUTED_TREE:
                    this.parseDistributedTree(xmlExtendedStreamReader, jnpServerConnectorServiceAddOperation, distributedTreManagerServiceAddOperation);
                    break;
                case UNKNOWN:
                default:
                    throw unexpectedElement(xmlExtendedStreamReader);

            }

        }

    }

    private void parseJNPServer(XMLExtendedStreamReader xmlExtendedStreamReader, ModelNode jnpServerServiceAddOperation) throws XMLStreamException {
        requireNoAttributes(xmlExtendedStreamReader);
        requireNoContent(xmlExtendedStreamReader);
    }

    private void parseJNPConnector(final XMLExtendedStreamReader xmlExtendedStreamReader,
            final ModelNode jnpServerConnectorServiceAddOperation/*, final ModelNode distributedTreManagerServiceAddOperation*/) throws XMLStreamException {
        for (int i = 0; i < xmlExtendedStreamReader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(xmlExtendedStreamReader, i);
            final String value = xmlExtendedStreamReader.getAttributeValue(i);
            switch (JNPSubsystemXMLAttribute.forName(xmlExtendedStreamReader.getAttributeLocalName(i))) {
                case SOCKET_BINDING:
                    JNPServerConnectorResourceDefinition.SOCKET_BINDING.parseAndSetParameter(value, jnpServerConnectorServiceAddOperation, xmlExtendedStreamReader);
                    break;
                case RMI_SOCKET_BINDING:
                    JNPServerConnectorResourceDefinition.RMI_SOCKET_BINDING.parseAndSetParameter(value, jnpServerConnectorServiceAddOperation, xmlExtendedStreamReader);
                    break;
                case CACHE_CONTAINER:
                    JNPServerConnectorResourceDefinition.CACHE_CONTAINER.parseAndSetParameter(value, jnpServerConnectorServiceAddOperation, xmlExtendedStreamReader);
                    /*DistributedTreeManagerResourceDefinition.CACHE_CONTAINER.parseAndSetParameter(value, distributedTreManagerServiceAddOperation, xmlExtendedStreamReader);
                     break;
                case CACHE_REF:
                    DistributedTreeManagerResourceDefinition.CACHE_REF.parseAndSetParameter(value, distributedTreManagerServiceAddOperation, xmlExtendedStreamReader);
                   */ break;
                case UNKNOWN:
                default: {
                    throw unexpectedAttribute(xmlExtendedStreamReader, i);
                }
            }
        }
        requireNoContent(xmlExtendedStreamReader);
    }

    private void parseDistributedTree(final XMLExtendedStreamReader xmlExtendedStreamReader, final ModelNode jnpServerConnectorServiceAddOperation, final ModelNode distributedTreManagerServiceAddOperation) throws XMLStreamException {
        for (int i = 0; i < xmlExtendedStreamReader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(xmlExtendedStreamReader, i);
            final String value = xmlExtendedStreamReader.getAttributeValue(i);
            switch (JNPSubsystemXMLAttribute.forName(xmlExtendedStreamReader.getAttributeLocalName(i))) {
                case CACHE_CONTAINER:
                    // JNPServerConnectorResourceDefinition.CACHE_CONTAINER.parseAndSetParameter(value, jnpServerConnectorServiceAddOperation, xmlExtendedStreamReader);
                    DistributedTreeManagerResourceDefinition.CACHE_CONTAINER.parseAndSetParameter(value, distributedTreManagerServiceAddOperation, xmlExtendedStreamReader);
                    break;
                case CACHE_REF:
                    DistributedTreeManagerResourceDefinition.CACHE_REF.parseAndSetParameter(value, distributedTreManagerServiceAddOperation, xmlExtendedStreamReader);
                    break;
                case UNKNOWN:
                default: {
                    throw unexpectedAttribute(xmlExtendedStreamReader, i);
                }
            }
        }
        requireNoContent(xmlExtendedStreamReader);
    }
}
