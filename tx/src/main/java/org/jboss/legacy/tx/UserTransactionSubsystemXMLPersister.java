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
package org.jboss.legacy.tx;

import javax.xml.stream.XMLStreamException;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.legacy.tx.remoting.RemotingConnectorModel;
import static org.jboss.legacy.tx.remoting.RemotingConnectorModel.SERVICE_NAME;
import static org.jboss.legacy.tx.remoting.RemotingConnectorModel.SOCKET_BINDING;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class UserTransactionSubsystemXMLPersister implements XMLElementWriter<SubsystemMarshallingContext> {

    public static final UserTransactionSubsystemXMLPersister INSTANCE = new UserTransactionSubsystemXMLPersister();

    @Override
    public void writeContent(XMLExtendedStreamWriter xmlExtendedStreamWriter,
            SubsystemMarshallingContext subsystemMarshallingContext) throws XMLStreamException {
        subsystemMarshallingContext.startSubsystemElement(UserTransactionSubsystemNamespace.LEGACY_TX_1_0.getUriString(), false);
        writeElements(xmlExtendedStreamWriter, subsystemMarshallingContext);
        xmlExtendedStreamWriter.writeEndElement();
    }

    private void writeElements(XMLExtendedStreamWriter xmlExtendedStreamWriter,
            SubsystemMarshallingContext subsystemMarshallingContext) throws XMLStreamException {
        final ModelNode model = subsystemMarshallingContext.getModelNode();
        if (model.hasDefined(RemotingConnectorModel.SERVICE_NAME)) {
            writeConnector(xmlExtendedStreamWriter, subsystemMarshallingContext);
        }
    }
/**
     *
     * @param xmlExtendedStreamWriter
     * @param subsystemMarshallingContext
     * @throws XMLStreamException
     */
    private void writeConnector(final XMLExtendedStreamWriter xmlExtendedStreamWriter,
            final SubsystemMarshallingContext subsystemMarshallingContext) throws XMLStreamException {
        final ModelNode model = subsystemMarshallingContext.getModelNode().get(SERVICE_NAME);
        xmlExtendedStreamWriter.writeStartElement(UserTransactionSubsystemXMLElement.CONNECTOR.getLocalName());
        if (model.hasDefined(SOCKET_BINDING)) {
            xmlExtendedStreamWriter.writeAttribute(UserTransactionSubsystemXMLAttribute.SOCKET_BINDING.getLocalName(),
                    model.get(SOCKET_BINDING).asString());
        }
        xmlExtendedStreamWriter.writeEndElement();
    }
}
