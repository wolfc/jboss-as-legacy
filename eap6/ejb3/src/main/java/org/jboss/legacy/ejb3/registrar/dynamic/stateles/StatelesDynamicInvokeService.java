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

package org.jboss.legacy.ejb3.registrar.dynamic.stateles;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.ejb3.proxy.spi.container.InvokableContext;
import org.jboss.legacy.common.EJBDataProxy;
import org.jboss.legacy.ejb3.registrar.dynamic.DynamicInvocationService;
import org.jboss.legacy.ejb3.registrar.dynamic.DynamicInvokableContext;
import org.jboss.msc.service.Service;

/**
 * @author baranowb
 */
public class StatelesDynamicInvokeService extends DynamicInvocationService implements Service<DynamicInvocationService> {

    public StatelesDynamicInvokeService(final EJBDataProxy ejb3Data, final EEModuleDescription moduleDescription,
            final EJBComponentDescription ejbComponentDescription) {
        super(ejb3Data, moduleDescription, ejbComponentDescription);
    }

    @Override
    public DynamicInvocationService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    protected InvokableContext createInvokableContext() {
        return new DynamicInvokableContext(super.ejb3Data, super.serverSecurityManagerInjectedValue, super.ejb3RegistrarInjectedValue,
                super.deploymentRepositoryInjectedValue, super.viewInjectedValue, super.applicationName, super.moduleName,
                super.distinctName, super.componentName);
    }
}