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
package org.jboss.legacy.tx.usertx;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.proxy.Proxy;
import org.jboss.aspects.remoting.InvokeRemoteInterceptor;
import org.jboss.aspects.remoting.MergeMetaDataInterceptor;
import org.jboss.aspects.remoting.RemotingProxyFactory;
import static org.jboss.legacy.jnp.JNPSubsystemModel.LEGACY;
import org.jboss.legacy.jnp.server.JNPServer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting.transport.Connector;
import org.jboss.tm.usertx.client.ClientUserTransaction;
import org.jboss.tm.usertx.interfaces.UserTransactionSessionFactory;
import org.jnp.interfaces.Naming;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class ClientUserTransactionService implements Service<RemotingProxyFactory> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(LEGACY).append(ClientUserTransactionModel.SERVICE_NAME);
    private static final String JNDI_UT_SESSION_FACTORY = "UserTransactionSessionFactory";
    private static final String JNDI_UT = "UserTransaction";
    private static final String JNDI_JBOSS_CONTEXT = "java:jboss";
    private static final String JNDI_COMP_CONTEXT = "java:comp";

    private RemotingProxyFactory service;
    private final InjectedValue<Connector> injectedConnector = new InjectedValue<Connector>();
    private final InjectedValue<JNPServer> injectedJNPServer = new InjectedValue<JNPServer>();
    private final InjectedValue<RemotingProxyFactory> injectedUserSessionTransactionProxyFactory = new InjectedValue<RemotingProxyFactory>();

    public InjectedValue<Connector> getInjectedConnector() {
        return injectedConnector;
    }

    public InjectedValue<RemotingProxyFactory> getInjectedUserSessionTransactionProxyFactory() {
        return injectedUserSessionTransactionProxyFactory;
    }

    public InjectedValue<JNPServer> getInjectedJNPServer() {
        return injectedJNPServer;
    }

    @Override
    public void start(StartContext context) throws StartException {
        service = new RemotingProxyFactory();
        try {
            createSubcontext("java:");
            createSubcontext(JNDI_JBOSS_CONTEXT);
            createSubcontext(JNDI_COMP_CONTEXT);
            bindGlobally(JNDI_UT, ClientUserTransaction.getSingleton().getReference(), ClientUserTransaction.class.getName());
            service.setConnector(injectedConnector.getValue());
            service.setInvokerLocator(injectedConnector.getValue().getInvokerLocator());
            ArrayList<Interceptor> proxyInterceptors = new ArrayList<Interceptor>(2);
            proxyInterceptors.add(MergeMetaDataInterceptor.singleton);
            proxyInterceptors.add(InvokeRemoteInterceptor.singleton);
            service.setInterceptors(proxyInterceptors);
            service.setTarget(new LegacyUserTransactionSessionFactory(injectedUserSessionTransactionProxyFactory.getValue()));
            service.setInterfaces(new Class<?>[]{UserTransactionSessionFactory.class});
            service.setDispatchName("UserTransactionSessionFactory");
            service.start();
            Proxy ut = service.getProxy();
            bindGlobally(JNDI_UT_SESSION_FACTORY, ut, UserTransactionSessionFactory.class.getName());
        } catch (Exception ex) {
            throw new StartException(ex);
        }
    }

    private void createSubcontext(String name) throws NamingException, RemoteException {
        Naming namingServer = this.injectedJNPServer.getValue().getNamingBean().getNamingInstance();
        Name compoundName = new CompoundName(name, new Properties());
        try {
            if (namingServer.lookup(compoundName) != null) {
                return;
            }
            namingServer.createSubcontext(compoundName);
        } catch (NameNotFoundException ex) {
            namingServer.createSubcontext(compoundName);
        }
    }

    private void bindGlobally(String name, Object object, String className) throws NamingException, RemoteException {
        bind(JNDI_JBOSS_CONTEXT + '/' + name, object, className);
        bind(JNDI_COMP_CONTEXT + '/' + name, object, className);
        bind(name, object, className);
    }

    private void bind(String name, Object object, String className) throws NamingException, RemoteException {
        Naming namingServer = this.injectedJNPServer.getValue().getNamingBean().getNamingInstance();
        Name compoundName = new CompoundName(name, new Properties());
        try {
            if (namingServer.lookup(compoundName) != null) {
                return;
            }
            namingServer.bind(compoundName, object, className);
        } catch (NameNotFoundException ex) {
            namingServer.bind(compoundName, object, className);
        }
    }

    @Override
    public void stop(StopContext context) {
        try {
            service.stop();
        } catch (Exception ex) {
            Logger.getLogger(ClientUserTransactionService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public RemotingProxyFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return service;
    }
}
