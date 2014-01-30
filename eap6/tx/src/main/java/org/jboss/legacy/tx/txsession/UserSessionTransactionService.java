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
package org.jboss.legacy.tx.txsession;

import com.arjuna.ats.internal.jbossatx.jta.PropagationContextManager;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.transaction.TransactionManager;
import org.jboss.aop.advice.Interceptor;
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
import org.jboss.tm.usertx.interfaces.UserTransactionSession;
import org.jnp.interfaces.Naming;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class UserSessionTransactionService implements Service<RemotingProxyFactory> {

    private static final String JNDI_IMPORTER = "java:/TransactionPropagationContextImporter";
    private static final String JNDI_EXPORTER = "java:/TransactionPropagationContextExporter";

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(LEGACY).append(UserSessionTransactionModel.SERVICE_NAME);

    private RemotingProxyFactory service;
    private final InjectedValue<Connector> injectedConnector = new InjectedValue<Connector>();
    private final InjectedValue<JNPServer> injectedJNPServer = new InjectedValue<JNPServer>();
    private final InjectedValue<TransactionManager> injectedTransactionManager = new InjectedValue<TransactionManager>();

    public InjectedValue<Connector> getInjectedConnector() {
        return injectedConnector;
    }

    public InjectedValue<JNPServer> getInjectedJNPServer() {
        return injectedJNPServer;
    }

    public InjectedValue<TransactionManager> getInjectedTransactionManager() {
        return injectedTransactionManager;
    }

    @Override
    public void start(StartContext context) throws StartException {
        service = new RemotingProxyFactory();
        try {
            bindRef(JNDI_IMPORTER, PropagationContextManager.class.getName());
            bindRef(JNDI_EXPORTER, PropagationContextManager.class.getName());
            service.setConnector(injectedConnector.getValue());
            service.setInvokerLocator(injectedConnector.getValue().getInvokerLocator());
            ArrayList<Interceptor> proxyInterceptors = new ArrayList<Interceptor>(2);
            proxyInterceptors.add(MergeMetaDataInterceptor.singleton);
            proxyInterceptors.add(InvokeRemoteInterceptor.singleton);
            service.setInterceptors(proxyInterceptors);
            service.setTarget(new LegacyUserSessionTransaction());
            service.setInterfaces(new Class<?>[]{UserTransactionSession.class});
            service.setDispatchName("UserTransactionSession");
            service.start();
        } catch (Exception ex) {
            throw new StartException(ex);
        }
    }

    @Override
    public void stop(StopContext context) {
        try {
            service.stop();
        } catch (Exception ex) {
            Logger.getLogger(UserSessionTransactionService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public RemotingProxyFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return service;
    }

    private void bindRef(String jndiName, String className) throws Exception {
        Naming namingServer = this.injectedJNPServer.getValue().getNamingBean().getNamingInstance();
        Reference ref = new Reference(className, className, null);
        namingServer.bind(new CompoundName(jndiName, new Properties()), ref, className);
        createJNPLocalContext().bind(jndiName, ref);
    }

    protected InitialContext createJNPLocalContext() throws NamingException {
        final Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.LocalOnlyContextFactory");
        env.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        return new InitialContext(env);
    }
}
