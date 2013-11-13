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
package org.jboss.legacy.jnp.server;

import javax.naming.NamingException;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.msc.inject.Injector;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jnp.interfaces.Naming;
import org.jnp.server.NamingBean;

/**
 * @author baranowb
 */
public class JNPServerService implements Service<JNPServer> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(JNPServerModel.LEGACY).append(
            JNPServerModel.SERVICE_NAME);

    private final InjectedValue<ServiceBasedNamingStore> namingStoreValue = new InjectedValue<ServiceBasedNamingStore>();
    private NamingStoreWrapper singletonNamingServer;

    public JNPServerService() {
        super();
    }

    @Override
    public JNPServer getValue() throws IllegalStateException, IllegalArgumentException {
        return new JNPServer() {
            @Override
            public NamingBean getNamingBean() {
                return new NamingBean() {

                    @Override
                    public Naming getNamingInstance() {
                        return singletonNamingServer;
                    }
                };
            }
        };
    }

    /**
     * Get the naming store injector.
     *
     * @return the injector
     */
    public Injector<ServiceBasedNamingStore> getNamingStoreInjector() {
        return namingStoreValue;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        try {
            this.singletonNamingServer = new NamingStoreWrapper(namingStoreValue.getValue());
        } catch (NamingException e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        this.singletonNamingServer = null;
    }

}
