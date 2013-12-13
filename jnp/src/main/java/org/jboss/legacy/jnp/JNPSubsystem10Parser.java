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

import java.util.EnumSet;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import org.jboss.as.controller.operations.common.Util;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import org.jboss.dmr.ModelNode;
import static org.jboss.legacy.jnp.JNPSubsystemModel.SERVICE;
import org.jboss.legacy.jnp.connector.JNPServerConnectorModel;
import org.jboss.legacy.jnp.connector.JNPServerConnectorResourceDefinition;
import org.jboss.legacy.jnp.infinispan.DistributedTreeManagerModel;
import org.jboss.legacy.jnp.infinispan.DistributedTreeManagerResourceDefinition;
import org.jboss.legacy.jnp.remoting.RemotingModel;
import org.jboss.legacy.jnp.remoting.RemotingResourceDefinition;
import org.jboss.legacy.jnp.server.JNPServerModel;
import org.jboss.legacy.jnp.server.JNPServerResourceDefinition;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class JNPSubsystem10Parser implements XMLElementReader<List<ModelNode>> {

    public static final JNPSubsystem10Parser INSTANCE = new JNPSubsystem10Parser();

    protected JNPSubsystem10Parser() {
    }

    @Override
    public void readElement(final XMLExtendedStreamReader xmlExtendedStreamReader, final List<ModelNode> result)
            throws XMLStreamException {
        requireNoAttributes(xmlExtendedStreamReader);
        final ModelNode jnpSubsystemAddOperation = Util.createAddOperation();
        jnpSubsystemAddOperation.get(OP_ADDR).add(SUBSYSTEM, JNPExtension.SUBSYSTEM_NAME);
        result.add(jnpSubsystemAddOperation);

        final ModelNode jnpServerServiceAddOperation = Util.createAddOperation();
        jnpServerServiceAddOperation.get(OP_ADDR).add(SUBSYSTEM, JNPExtension.SUBSYSTEM_NAME).add(SERVICE, JNPServerModel.SERVICE_NAME);
        result.add(jnpServerServiceAddOperation);

        final ModelNode jnpServerConnectorServiceAddOperation = Util.createAddOperation();
        jnpServerConnectorServiceAddOperation.get(OP_ADDR).add(SUBSYSTEM, JNPExtension.SUBSYSTEM_NAME).add(SERVICE, JNPServerConnectorModel.SERVICE_NAME);
        result.add(jnpServerConnectorServiceAddOperation);

        final ModelNode distributedTreManagerServiceAddOperation = Util.createAddOperation();
        distributedTreManagerServiceAddOperation.get(OP_ADDR).add(SUBSYSTEM, JNPExtension.SUBSYSTEM_NAME).add(SERVICE, DistributedTreeManagerModel.SERVICE_NAME);

        final ModelNode remotingServiceAddOperation = Util.createAddOperation();
        remotingServiceAddOperation.get(OP_ADDR).add(SUBSYSTEM, JNPExtension.SUBSYSTEM_NAME).add(SERVICE, RemotingModel.SERVICE_NAME);
        result.add(remotingServiceAddOperation);

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
                    this.parseJNPConnector(xmlExtendedStreamReader, jnpServerConnectorServiceAddOperation);
                    break;
                case JNP_SERVER:
                    this.parseJNPServer(xmlExtendedStreamReader, jnpServerServiceAddOperation);
                    break;
                case REMOTING:
                    this.parseRemoting(xmlExtendedStreamReader, remotingServiceAddOperation);
                    break;
                case DISTRIBUTED_CACHE:
                    this.parseDistributedCache(xmlExtendedStreamReader, distributedTreManagerServiceAddOperation, jnpServerConnectorServiceAddOperation);
                    JNPServerResourceDefinition.HA.parseAndSetParameter("true", jnpServerServiceAddOperation, xmlExtendedStreamReader);
                    result.add(distributedTreManagerServiceAddOperation);
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

    private void parseJNPConnector(final XMLExtendedStreamReader xmlExtendedStreamReader, final ModelNode jnpServerConnectorServiceAddOperation) throws XMLStreamException {
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
                case UNKNOWN:
                default: {
                    throw unexpectedAttribute(xmlExtendedStreamReader, i);
                }
            }
        }
        requireNoContent(xmlExtendedStreamReader);

    }

    private void parseRemoting(XMLExtendedStreamReader xmlExtendedStreamReader, ModelNode remotingServiceAddOperation) throws XMLStreamException {
        for (int i = 0; i < xmlExtendedStreamReader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(xmlExtendedStreamReader, i);
            final String value = xmlExtendedStreamReader.getAttributeValue(i);
            switch (JNPSubsystemXMLAttribute.forName(xmlExtendedStreamReader.getAttributeLocalName(i))) {
                case SOCKET_BINDING:
                    RemotingResourceDefinition.SOCKET_BINDING.parseAndSetParameter(value, remotingServiceAddOperation,
                            xmlExtendedStreamReader);
                    break;
                case UNKNOWN:
                default: {
                    throw unexpectedAttribute(xmlExtendedStreamReader, i);
                }
            }
        }
        requireNoContent(xmlExtendedStreamReader);
    }

    private boolean parseDistributedCache(XMLExtendedStreamReader xmlExtendedStreamReader, ModelNode distributedTreManagerServiceAddOperation,
            final ModelNode jnpServerConnectorServiceAddOperation) throws XMLStreamException {
        for (int i = 0; i < xmlExtendedStreamReader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(xmlExtendedStreamReader, i);
            final String value = xmlExtendedStreamReader.getAttributeValue(i);
            switch (JNPSubsystemXMLAttribute.forName(xmlExtendedStreamReader.getAttributeLocalName(i))) {
                case CACHE_CONTAINER:
                    DistributedTreeManagerResourceDefinition.CACHE_CONTAINER.parseAndSetParameter(value, distributedTreManagerServiceAddOperation, xmlExtendedStreamReader);
                    JNPServerConnectorResourceDefinition.CACHE_CONTAINER.parseAndSetParameter(value, jnpServerConnectorServiceAddOperation, xmlExtendedStreamReader);
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
        return true;
    }
}
