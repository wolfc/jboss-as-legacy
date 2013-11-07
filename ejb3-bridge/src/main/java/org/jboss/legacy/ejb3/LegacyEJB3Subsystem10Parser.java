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

package org.jboss.legacy.ejb3;

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
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.legacy.ejb3.registrar.LegacyEJB3RegistrarModel;
import org.jboss.legacy.ejb3.remoting.LegacyRemotingModel;
import org.jboss.legacy.ejb3.remoting.LegacyRemotingResourceDefinition;
import org.jboss.legacy.jnp.LegacyJNPServerModel;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 */
public class LegacyEJB3Subsystem10Parser implements XMLElementReader<List<ModelNode>> {

    public static final LegacyEJB3Subsystem10Parser INSTANCE = new LegacyEJB3Subsystem10Parser();

    protected LegacyEJB3Subsystem10Parser() {
    }

    @Override
    public void readElement(final XMLExtendedStreamReader xmlExtendedStreamReader, final List<ModelNode> result)
            throws XMLStreamException {
        // no attributes
        requireNoAttributes(xmlExtendedStreamReader);
        final ModelNode ejb3SubsystemAddOperation = Util.createAddOperation();
        ejb3SubsystemAddOperation.get(OP_ADDR).add(SUBSYSTEM, LegacyEJB3Extension.SUBSYSTEM_NAME);
        result.add(ejb3SubsystemAddOperation);

        final ModelNode remotingServiceAddOperation = Util.createAddOperation();
        remotingServiceAddOperation.get(OP_ADDR).add(SUBSYSTEM, LegacyEJB3Extension.SUBSYSTEM_NAME)
                .add(LegacyRemotingModel.SERVICE, LegacyRemotingModel.SERVICE_NAME);
        result.add(remotingServiceAddOperation);

        final ModelNode jnpServerServiceAddOperation = Util.createAddOperation();
        jnpServerServiceAddOperation.get(OP_ADDR).add(SUBSYSTEM, LegacyEJB3Extension.SUBSYSTEM_NAME)
                .add(LegacyJNPServerModel.SERVICE, LegacyJNPServerModel.SERVICE_NAME);
        result.add(jnpServerServiceAddOperation);

        final ModelNode ejb3RegistrarServerServiceAddOperation = Util.createAddOperation();
        ejb3RegistrarServerServiceAddOperation.get(OP_ADDR).add(SUBSYSTEM, LegacyEJB3Extension.SUBSYSTEM_NAME)
                .add(LegacyEJB3RegistrarModel.SERVICE, LegacyEJB3RegistrarModel.SERVICE_NAME);
        result.add(ejb3RegistrarServerServiceAddOperation);

        // elements
        final EnumSet<LegacyEJB3SybsystemXMLElement> encountered = EnumSet.noneOf(LegacyEJB3SybsystemXMLElement.class);
        while (xmlExtendedStreamReader.hasNext() && xmlExtendedStreamReader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (LegacyEJB3SubsystemNamespace.forUri(xmlExtendedStreamReader.getNamespaceURI()) != LegacyEJB3SubsystemNamespace.LEGACY_EJB3_1_0) {
                throw unexpectedElement(xmlExtendedStreamReader);
            }
            final LegacyEJB3SybsystemXMLElement element = LegacyEJB3SybsystemXMLElement.forName(xmlExtendedStreamReader
                    .getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(xmlExtendedStreamReader);
            }
            switch (element) {
                case REMOTING:
                    this.parseRemoting(xmlExtendedStreamReader, remotingServiceAddOperation);
                    break;
                case EJB3_REGISTRAR:
                    this.parseRegistrar(xmlExtendedStreamReader, ejb3RegistrarServerServiceAddOperation);
                    break;
                case JNP_SERVER:
                    this.parseJNPServer(xmlExtendedStreamReader, jnpServerServiceAddOperation);
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

    private void parseRegistrar(XMLExtendedStreamReader xmlExtendedStreamReader,
            ModelNode ejb3RegistrarServerServiceAddOperation) throws XMLStreamException {
        requireNoAttributes(xmlExtendedStreamReader);
        requireNoContent(xmlExtendedStreamReader);
    }

    private void parseRemoting(final XMLExtendedStreamReader xmlExtendedStreamReader,
            final ModelNode ejb3RemotingServiceAddOperation) throws XMLStreamException {
        for (int i = 0; i < xmlExtendedStreamReader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(xmlExtendedStreamReader, i);
            final String value = xmlExtendedStreamReader.getAttributeValue(i);
            switch (LegacyEJB3XMLSubsystemAttribute.forName(xmlExtendedStreamReader.getAttributeLocalName(i))) {
                case HOST:
                    LegacyRemotingResourceDefinition.HOST.parseAndSetParameter(value, ejb3RemotingServiceAddOperation,
                            xmlExtendedStreamReader);
                    break;
                case PORT:
                    LegacyRemotingResourceDefinition.PORT.parseAndSetParameter(value, ejb3RemotingServiceAddOperation,
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
}
