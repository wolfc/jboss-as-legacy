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

import java.util.HashMap;
import java.util.Map;

import org.jboss.aspects.remoting.AOPRemotingInvocationHandler;
import org.jboss.ejb3.common.registrar.spi.Ejb3RegistrarLocator;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.remoting.ServerConfiguration;
import org.jboss.remoting.transport.Connector;


/**
 * @author baranowb
 *
 */
public class RemotingConnectorService implements Service<Connector>{

    private static final String INVOCATION_HANDLER_KEY = "AOP";
    private static final String INVOCATION_HANDLER_CLASS = AOPRemotingInvocationHandler.class.getName();
    private static final String URL_SCHEME = "socket://";
    private Connector connector;
    /** The name of the remoting service */

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(RemotingModel.LEGACY).append(RemotingModel.SERVICE_NAME);

    public RemotingConnectorService(final String host, final int port) {
        super();
        final ServerConfiguration serverConfiguration = new ServerConfiguration();
        final Map<String, String> invocationHandlers = new HashMap<String, String>();
        invocationHandlers.put(INVOCATION_HANDLER_KEY, INVOCATION_HANDLER_CLASS);
        serverConfiguration.setInvocationHandlers(invocationHandlers);
        this.connector = new Connector(URL_SCHEME+host+":"+port);
        //this.connector = new Connector("socket://0.0.0.0:4873");
        this.connector.setServerConfiguration(serverConfiguration);
    }


    @Override
    public Connector getValue() throws IllegalStateException, IllegalArgumentException {
        return this.connector;
    }


    @Override
    public void start(StartContext startContext) throws StartException {
        try{
            this.connector.start();
        }catch(Exception e){
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        this.connector.stop();
    }

}
