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

import javax.xml.stream.XMLStreamException;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import static org.jboss.legacy.jnp.JNPSubsystemModel.SERVICE;
import org.jboss.legacy.jnp.connector.JNPServerConnectorModel;
import org.jboss.legacy.jnp.connector.JNPServerConnectorResourceDefinition;
import org.jboss.legacy.jnp.infinispan.DistributedTreeManagerModel;
import org.jboss.legacy.jnp.infinispan.DistributedTreeManagerResourceDefinition;
import org.jboss.legacy.jnp.remoting.RemotingModel;
import org.jboss.legacy.jnp.remoting.RemotingResourceDefinition;
import org.jboss.legacy.jnp.server.JNPServerModel;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author baranowb
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class JNPSubsystemXMLPersister implements XMLElementWriter<SubsystemMarshallingContext> {

    public static final JNPSubsystemXMLPersister INSTANCE = new JNPSubsystemXMLPersister();

    @Override
    public void writeContent(XMLExtendedStreamWriter xmlExtendedStreamWriter,
            SubsystemMarshallingContext subsystemMarshallingContext) throws XMLStreamException {
        subsystemMarshallingContext.startSubsystemElement(JNPSubsystemNamespace.LEGACY_JNP_1_0.getUriString(), false);
        writeElements(xmlExtendedStreamWriter, subsystemMarshallingContext);
        xmlExtendedStreamWriter.writeEndElement();
    }

    private void writeElements(XMLExtendedStreamWriter xmlExtendedStreamWriter,
            SubsystemMarshallingContext subsystemMarshallingContext) throws XMLStreamException {
        if (subsystemMarshallingContext.getModelNode().hasDefined(SERVICE))  {
            final ModelNode model = subsystemMarshallingContext.getModelNode().get(SERVICE);

            if (model.hasDefined(JNPServerModel.SERVICE_NAME)) {
                writeJNPServer(xmlExtendedStreamWriter);
            }

            if (model.hasDefined(JNPServerConnectorModel.SERVICE_NAME)) {
                writeConnector(xmlExtendedStreamWriter, subsystemMarshallingContext);
            }

            if (model.hasDefined(RemotingModel.SERVICE_NAME)) {
                writeRemoting(xmlExtendedStreamWriter, subsystemMarshallingContext);
            }

            if (model.hasDefined(DistributedTreeManagerModel.SERVICE_NAME)) {
                final ModelNode treeModel = model.get(DistributedTreeManagerModel.SERVICE_NAME);
                if (model.hasDefined(DistributedTreeManagerModel.CACHE_CONTAINER) && treeModel.hasDefined(DistributedTreeManagerModel.CACHE_REF)) {
                    writeDistributedCache(xmlExtendedStreamWriter, treeModel);
                }
            }
        }

    }

    /**
     * @param xmlExtendedStreamWriter
     * @param subsystemMarshallingContext
     */
    private void writeConnector(XMLExtendedStreamWriter xmlExtendedStreamWriter,
            SubsystemMarshallingContext subsystemMarshallingContext) throws XMLStreamException {
        final ModelNode model = subsystemMarshallingContext.getModelNode().get(SERVICE).get(JNPServerConnectorModel.SERVICE_NAME);

        xmlExtendedStreamWriter.writeStartElement(JNPSubsystemXMLElement.JNP_CONNECTOR.getLocalName());
        if (model.hasDefined(JNPServerConnectorModel.SOCKET_BINDING)) {
            JNPServerConnectorResourceDefinition.SOCKET_BINDING.marshallAsAttribute(model, true, xmlExtendedStreamWriter);
        }

        if (model.hasDefined(JNPServerConnectorModel.RMI_SOCKET_BINDING)) {
            JNPServerConnectorResourceDefinition.RMI_SOCKET_BINDING.marshallAsAttribute(model, true, xmlExtendedStreamWriter);
        }

        xmlExtendedStreamWriter.writeEndElement();
    }

    private void writeJNPServer(XMLExtendedStreamWriter xmlExtendedStreamWriter) throws XMLStreamException {
        xmlExtendedStreamWriter.writeStartElement(JNPSubsystemXMLElement.JNP_SERVER.getLocalName());
        xmlExtendedStreamWriter.writeEndElement();
    }

    private void writeRemoting(XMLExtendedStreamWriter xmlExtendedStreamWriter,
            SubsystemMarshallingContext subsystemMarshallingContext) throws XMLStreamException {
        final ModelNode model = subsystemMarshallingContext.getModelNode().get(SERVICE).get(RemotingModel.SERVICE_NAME);

        xmlExtendedStreamWriter.writeStartElement(JNPSubsystemXMLElement.REMOTING.getLocalName());
        if (model.hasDefined(RemotingModel.SOCKET_BINDING)) {
            RemotingResourceDefinition.SOCKET_BINDING.marshallAsAttribute(model, true, xmlExtendedStreamWriter);
        }
        xmlExtendedStreamWriter.writeEndElement();
    }

    private void writeDistributedCache(XMLExtendedStreamWriter xmlExtendedStreamWriter, ModelNode treeModel) throws XMLStreamException {
        xmlExtendedStreamWriter.writeStartElement(JNPSubsystemXMLElement.DISTRIBUTED_CACHE.getLocalName());
        DistributedTreeManagerResourceDefinition.CACHE_CONTAINER.marshallAsAttribute(treeModel, true, xmlExtendedStreamWriter);
        DistributedTreeManagerResourceDefinition.CACHE_REF.marshallAsAttribute(treeModel, true, xmlExtendedStreamWriter);
        xmlExtendedStreamWriter.writeEndElement();
    }
}
