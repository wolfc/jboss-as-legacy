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

package org.jboss.legacy.ejb3.registrar;

import java.net.URL;

import org.jboss.aop.AspectXmlLoader;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.ee.component.Component;
import org.jboss.ejb3.common.registrar.spi.Ejb3Registrar;
import org.jboss.ejb3.common.registrar.spi.Ejb3RegistrarLocator;
import org.jboss.ejb3.proxy.impl.jndiregistrar.JndiStatefulSessionRegistrar;
import org.jboss.ejb3.proxy.impl.jndiregistrar.JndiStatelessSessionRegistrar;
import org.jboss.ejb3.proxy.impl.objectfactory.session.stateful.StatefulSessionProxyObjectFactory;
import org.jboss.ejb3.proxy.impl.objectfactory.session.stateless.StatelessSessionProxyObjectFactory;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting.transport.Connector;


/**
 * @author baranowb
 *
 */
public class EJB3RegistrarService implements Service<EJB3Registrar>{

    private static final String AOP_FILE = "ejb3-interceptors-aop.xml";
    private static final String CONNECTOR_BIND_NAME = "org.jboss.ejb3.RemotingConnector";
    //main thing
    private Ejb3Registrar registrar;
    //hacks
    private JndiStatelessSessionRegistrar jndiStatelessSessionRegistrar;
    private JndiStatefulSessionRegistrar jndiStatefulSessionRegistrar;
    private URL aopURL;

    private EJB3Registrar value = new LegacyEJB3RegistrarProxy(this);

    private InjectedValue<Connector> connector = new InjectedValue<Connector>();
    private final InjectedValue<ServerSecurityManager> serverSecurityManagerInjectedValue = new InjectedValue<ServerSecurityManager>();
    public EJB3RegistrarService() {
        super();

    }

    @Override
    public EJB3Registrar getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        try {
            this.registrar = new InMemoryEJB3Registrar();
            Ejb3RegistrarLocator.bindRegistrar(this.registrar);
            Ejb3RegistrarLocator.locateRegistrar().bind(CONNECTOR_BIND_NAME, this.connector.getValue());
            this.aopURL = Thread.currentThread().getContextClassLoader().getResource(AOP_FILE);
            AspectXmlLoader.deployXML(this.aopURL);
            this.jndiStatelessSessionRegistrar = new JndiStatelessSessionRegistrar(StatelessSessionProxyObjectFactory.class.getName());
            this.jndiStatefulSessionRegistrar = new JndiStatefulSessionRegistrar(StatefulSessionProxyObjectFactory.class.getName());
          //TODO AOP + ejb3 deployment interceptor to bind proxy.// AOP
        } catch (Exception e) {
            throw new StartException(e);
        }

    }

    @Override
    public void stop(StopContext stopContext) {
        if(Ejb3RegistrarLocator.isRegistrarBound() && Ejb3RegistrarLocator.locateRegistrar() == this.registrar){
            Ejb3RegistrarLocator.locateRegistrar().unbind(CONNECTOR_BIND_NAME);
            Ejb3RegistrarLocator.unbindRegistrar();
        }
        if(aopURL!=null){
            try {
                AspectXmlLoader.undeployXML(aopURL);
            } catch (Exception e) {
                e.printStackTrace();
            }
            aopURL = null;
        }
        this.jndiStatelessSessionRegistrar = null;
        this.jndiStatefulSessionRegistrar = null;
    }

    public InjectedValue<Connector> getInjectedValueConnector(){
        return this.connector;
    }

    public InjectedValue<ServerSecurityManager> getServerSecurityManagerInjectedValue() {
        return serverSecurityManagerInjectedValue;
    }

    private class LegacyEJB3RegistrarProxy implements EJB3Registrar{
        private EJB3RegistrarService wrapped;

        public LegacyEJB3RegistrarProxy(EJB3RegistrarService legacyEJB3RegistrarService) {
            this.wrapped = legacyEJB3RegistrarService;
        }

        @Override
        public Ejb3Registrar getRegistrar() {
            return wrapped.registrar;
        }

        @Override
        public JndiStatelessSessionRegistrar getJndiStatelessSessionRegistrar() {
            return wrapped.jndiStatelessSessionRegistrar;
        }

        @Override
        public JndiStatefulSessionRegistrar getJndiStatefulSessionRegistrar() {
            return wrapped.jndiStatefulSessionRegistrar;
        }

    }
}
