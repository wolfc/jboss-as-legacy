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
package org.jboss.legacy.tx;

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
import org.jboss.legacy.tx.remoting.RemotingConnectorModel;
import org.jboss.legacy.tx.remoting.RemotingResourceDefinition;
import org.jboss.legacy.tx.txsession.UserSessionTransactionModel;
import org.jboss.legacy.tx.usertx.ClientUserTransactionModel;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class UserTransactionSubsystem10Parser implements XMLElementReader<List<ModelNode>> {

    public static final UserTransactionSubsystem10Parser INSTANCE = new UserTransactionSubsystem10Parser();

    protected UserTransactionSubsystem10Parser() {
    }

    @Override
    public void readElement(final XMLExtendedStreamReader xmlExtendedStreamReader, final List<ModelNode> result)
            throws XMLStreamException {
        requireNoAttributes(xmlExtendedStreamReader);
        final ModelNode txSubsystemAddOperation = Util.createAddOperation();
        txSubsystemAddOperation.get(OP_ADDR).add(SUBSYSTEM, UserTransactionExtension.SUBSYSTEM_NAME);
        result.add(txSubsystemAddOperation);


        final ModelNode remotingConnectorServiceAddOperation = Util.createAddOperation();
        remotingConnectorServiceAddOperation.get(OP_ADDR).add(SUBSYSTEM, UserTransactionExtension.SUBSYSTEM_NAME)
                .add(RemotingConnectorModel.SERVICE, RemotingConnectorModel.SERVICE_NAME);
        result.add(remotingConnectorServiceAddOperation);

        final ModelNode clientUserTransactionServiceAddOperation = Util.createAddOperation();
        clientUserTransactionServiceAddOperation.get(OP_ADDR).add(SUBSYSTEM, UserTransactionExtension.SUBSYSTEM_NAME)
                .add(ClientUserTransactionModel.SERVICE, ClientUserTransactionModel.SERVICE_NAME);
        result.add(clientUserTransactionServiceAddOperation);

        final ModelNode userSessionTransactionServiceAddOperation = Util.createAddOperation();
        userSessionTransactionServiceAddOperation.get(OP_ADDR).add(SUBSYSTEM, UserTransactionExtension.SUBSYSTEM_NAME)
                .add(UserSessionTransactionModel.SERVICE, UserSessionTransactionModel.SERVICE_NAME);
        result.add(userSessionTransactionServiceAddOperation);
        final EnumSet<UserTransactionSubsystemXMLElement> encountered = EnumSet.noneOf(UserTransactionSubsystemXMLElement.class);
        while (xmlExtendedStreamReader.hasNext() && xmlExtendedStreamReader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (UserTransactionSubsystemNamespace.forUri(xmlExtendedStreamReader.getNamespaceURI()) != UserTransactionSubsystemNamespace.LEGACY_TX_1_0) {
                throw unexpectedElement(xmlExtendedStreamReader);
            }
            final UserTransactionSubsystemXMLElement element = UserTransactionSubsystemXMLElement.forName(xmlExtendedStreamReader
                    .getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(xmlExtendedStreamReader);
            }
            switch (element) {
                case CONNECTOR:
                    parseInvokerConnector(xmlExtendedStreamReader, remotingConnectorServiceAddOperation);
                    break;
                case UNKNOWN:
                default:
                    throw unexpectedElement(xmlExtendedStreamReader);
            }
        }

    }

    private boolean parseInvokerConnector(final XMLExtendedStreamReader xmlExtendedStreamReader,
            final ModelNode remotingConnectorServiceAddOperation) throws XMLStreamException {
        boolean isHa = false;
        for (int i = 0; i < xmlExtendedStreamReader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(xmlExtendedStreamReader, i);
            final String value = xmlExtendedStreamReader.getAttributeValue(i);
            switch (UserTransactionSubsystemXMLAttribute.forName(xmlExtendedStreamReader.getAttributeLocalName(i))) {
                case SOCKET_BINDING:
                    RemotingResourceDefinition.SOCKET_BINDING.parseAndSetParameter(value, remotingConnectorServiceAddOperation,
                            xmlExtendedStreamReader);
                    break;
                case UNKNOWN:
                default: {
                    throw unexpectedAttribute(xmlExtendedStreamReader, i);
                }
            }
        }
        requireNoContent(xmlExtendedStreamReader);
        return isHa;
    }
}
