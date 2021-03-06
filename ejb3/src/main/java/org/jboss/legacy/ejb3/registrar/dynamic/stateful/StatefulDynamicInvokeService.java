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

package org.jboss.legacy.ejb3.registrar.dynamic.stateful;

import java.io.Serializable;
import javax.naming.NamingException;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.ejb3.proxy.impl.jndiregistrar.JndiSessionRegistrarBase;
import org.jboss.ejb3.proxy.spi.container.InvokableContext;
import org.jboss.ejb3.proxy.spi.container.StatefulSessionFactory;
import org.jboss.legacy.common.EJBDataProxy;
import org.jboss.legacy.ejb3.registrar.EJB3Registrar;
import org.jboss.legacy.ejb3.registrar.dynamic.DynamicInvocationService;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;
import org.jboss.msc.service.Service;
import org.jboss.msc.value.InjectedValue;

/**
 * @author baranowb
 */
public class StatefulDynamicInvokeService extends DynamicInvocationService implements Service<DynamicInvocationService> {

    private final InjectedValue<Component> componentCreateServiceInjectedValue = new InjectedValue<Component>();

    public StatefulDynamicInvokeService(final EJBDataProxy ejb3Data, final EEModuleDescription moduleDescription,
            final EJBComponentDescription ejbComponentDescription) {
        super(ejb3Data, moduleDescription, ejbComponentDescription);
    }

    @Override
    public DynamicInvocationService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    protected InvokableContext createInvokableContext() {
        return new StatefulDynamicInvokableContext(super.ejb3Data, this.componentCreateServiceInjectedValue,
                super.serverSecurityManagerInjectedValue, super.ejb3RegistrarInjectedValue,
                super.deploymentRepositoryInjectedValue, super.viewInjectedValue, super.applicationName, super.moduleName,
                super.distinctName, super.componentName);
    }

    @Override
    protected void createLegacyBinding() throws NamingException {
        super.createLegacyBinding();
        final EJB3Registrar value = this.ejb3RegistrarInjectedValue.getValue();
        final JBossSessionBeanMetaData metaData = createMetaData(ejb3Data);
        final ClassLoader old = switchLoader(getClass().getClassLoader());
        try {

            // this is wrong
            value.getRegistrar().bind(metaData.getJndiName().replace("/remote", ""), new StatefulSessionFactory() {
                @Override
                public Serializable createSession() {
                    try {
                        StatefulSessionComponent component = (StatefulSessionComponent) componentCreateServiceInjectedValue
                                .getValue();
                        return component.createSession().getEncodedForm();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                Thread.currentThread().setContextClassLoader(old);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void removeLegacyBinding() throws NamingException {
        super.removeLegacyBinding();
        final EJB3Registrar value = this.ejb3RegistrarInjectedValue.getValue();
        final JBossSessionBeanMetaData metaData = createMetaData(ejb3Data);
        final ClassLoader old = switchLoader(getClass().getClassLoader());
        try {
            value.getRegistrar().unbind(metaData.getJndiName().replace("/remote", ""));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                Thread.currentThread().setContextClassLoader(old);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ClassLoader switchLoader(final ClassLoader loader) {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        return current;
    }

    private JndiSessionRegistrarBase getJndiSessionRegistrarBase(final EJBDataProxy data, final EJB3Registrar registrarService) {
        return data.isStateful() ? registrarService.getJndiStatefulSessionRegistrar() : registrarService
                .getJndiStatelessSessionRegistrar();
    }

    public InjectedValue<Component> getComponentCreateInjectedValue() {
        return componentCreateServiceInjectedValue;
    }
}