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

import javax.naming.NamingException;
import org.jboss.aop.joinpoint.MethodInvocation;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.InterceptorContext;
import org.jboss.legacy.common.EJBDataProxy;
import org.jboss.legacy.ejb3.registrar.EJB3Registrar;
import org.jboss.legacy.ejb3.registrar.dynamic.DynamicInvokableContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author baranowb
 */
public class StatefulDynamicInvokableContext extends DynamicInvokableContext {
    public static final String LEGACY_MD_SFSB="SFSBInvocation";
    public static final String LEGACY_MD_SESSION_ID="SessionID";
    private final InjectedValue<Component> componentCreateServiceInjectedValue;

    public StatefulDynamicInvokableContext(EJBDataProxy ejb3Data, InjectedValue<Component> componentCreateServiceInjectedValue,
            InjectedValue<ServerSecurityManager> serverSecurityManagerInjectedValue,
            InjectedValue<EJB3Registrar> ejb3RegistrarInjectedValue,
            InjectedValue<DeploymentRepository> deploymentRepositoryInjectedValue,
            InjectedValue<ComponentView> viewInjectedValue, String applicationName, String moduleName, String distinctName,
            String componentName) {
        super(ejb3Data, serverSecurityManagerInjectedValue, ejb3RegistrarInjectedValue,
                deploymentRepositoryInjectedValue, viewInjectedValue, applicationName, moduleName, distinctName, componentName);
        this.componentCreateServiceInjectedValue = componentCreateServiceInjectedValue;
    }

    @Override
    protected InterceptorContext createInterceptorContext(MethodInvocation si) throws NamingException {
        final InterceptorContext context = super.createInterceptorContext(si);
        //SessionID - this is created before we get here.
        final Object sessionID = si.getMetaData(LEGACY_MD_SFSB, LEGACY_MD_SESSION_ID);
        if(sessionID instanceof byte[]){
            context.putPrivateData(SessionID.class, SessionID.createSessionID((byte[])sessionID));
        } else {
            throw new RuntimeException("Wrong session ID: "+sessionID.getClass());
        }
        return context;
    }

}
