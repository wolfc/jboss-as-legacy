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

import org.jboss.legacy.jnp.server.JNPServer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jnp.server.Main;

/**
 * @author baranowb
 */
public class JNPServerConnectorService implements Service<Main> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(JNPServerConnectorModel.LEGACY).append(
            JNPServerConnectorModel.SERVICE_NAME);

    private InjectedValue<JNPServer> jnpServer = new InjectedValue<JNPServer>();
    private Main serverConnector;

    public JNPServerConnectorService() {
        super();

    }

    public InjectedValue<JNPServer> getJnjectedValueJnpServer() {
        return jnpServer;
    }

    @Override
    public Main getValue() throws IllegalStateException, IllegalArgumentException {
        return this.serverConnector;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        this.serverConnector = new Main();
        this.serverConnector.setNamingInfo(jnpServer.getValue().getNamingBean());
        try {
            this.serverConnector.start();
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        this.serverConnector.stop();
        this.serverConnector = null;
    }

}
