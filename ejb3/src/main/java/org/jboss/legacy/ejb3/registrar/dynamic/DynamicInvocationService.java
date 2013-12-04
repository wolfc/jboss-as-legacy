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

package org.jboss.legacy.ejb3.registrar.dynamic;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

import java.util.Hashtable;
import java.util.UUID;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.aop.Advisor;
import org.jboss.aop.AspectManager;
import org.jboss.aop.ClassAdvisor;
import org.jboss.aop.Dispatcher;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.ejb3.common.metadata.MetadataUtil;
import org.jboss.ejb3.proxy.impl.jndiregistrar.JndiSessionRegistrarBase;
import org.jboss.ejb3.proxy.spi.container.InvokableContext;
import org.jboss.legacy.common.EJBDataProxy;
import org.jboss.legacy.ejb3.registrar.EJB3Registrar;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;
import org.jboss.metadata.ejb.jboss.jndipolicy.spi.DeploymentSummary;
import org.jboss.metadata.ejb.spec.BusinessRemotesMetaData;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import static org.jboss.legacy.ejb3.registrar.dynamic.DynamicInvokableContext.getJndiSessionRegistrarBase;
import static org.jboss.legacy.ejb3.registrar.dynamic.DynamicInvokableContext.switchLoader;

/**
 * @author baranowb
 */
public abstract class DynamicInvocationService {
    public static final String LEGACY_MD_SECURITY = "security";
    public static final String LEGACY_MD_KEY_PRINCIPIAL = "principal";
    public static final String LEGACY_MD_KEY_CREDENTIAL = "credential";
    public static final String LEGACY_MD_KEY_CONTEXT = "context";

    protected static final ServiceName SERVICE_NAME_BASE = ServiceName.of("jboss", "legacy");
    protected final InjectedValue<ServerSecurityManager> serverSecurityManagerInjectedValue = new InjectedValue<ServerSecurityManager>();
    protected final InjectedValue<EJB3Registrar> ejb3RegistrarInjectedValue = new InjectedValue<EJB3Registrar>();
    protected final InjectedValue<DeploymentRepository> deploymentRepositoryInjectedValue = new InjectedValue<DeploymentRepository>();
    protected final InjectedValue<ComponentView> viewInjectedValue = new InjectedValue<ComponentView>();

    protected final ServiceName serviceName;
    protected final String applicationName;
    protected final String moduleName;
    protected final String distinctName;
    protected final String componentName;
    protected final EJBDataProxy ejb3Data;

    protected Object metadata;

    public DynamicInvocationService(EJBDataProxy ejb3Data, EEModuleDescription moduleDescription,
            EJBComponentDescription ejbComponentDescription) {
        this.ejb3Data = ejb3Data;
        this.serviceName = getServiceName(moduleDescription, ejbComponentDescription);
        this.applicationName = moduleDescription.getEarApplicationName();
        this.moduleName = moduleDescription.getModuleName();
        this.distinctName = moduleDescription.getDistinctName();
        this.componentName = ejbComponentDescription.getComponentName();
    }

    public void start(StartContext context) throws StartException {
        try {
            createLegacyBinding();
        } catch (NamingException e) {
            throw new StartException(e);
        }
    }

    public void stop(StopContext context) {
        try {
            removeLegacyBinding();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public InjectedValue<ServerSecurityManager> getServerSecurityManagerInjectedValue() {
        return serverSecurityManagerInjectedValue;
    }

    public InjectedValue<EJB3Registrar> getEJB3RegistrarInjectedValue() {
        return ejb3RegistrarInjectedValue;
    }

    public InjectedValue<DeploymentRepository> getDeploymentRepositoryInjectedValue() {
        return deploymentRepositoryInjectedValue;
    }

    public InjectedValue<ComponentView> getViewInjectedValue() {
        return this.viewInjectedValue;
    }

    public ServiceName getServiceName() {
        return this.serviceName;
    }

    /**
     * @param data
     * @param phaseContext
     * @throws NamingException
     */
    protected void createLegacyBinding() throws NamingException {
        final JBossSessionBeanMetaData metaData = createMetaData(ejb3Data);
        final String containerName = metaData.getName();
        final ClassLoader beanClassLoader = ejb3Data.getBeanClassLoader();
        Dispatcher.singleton.registerTarget(containerName, new InvokableContextClassProxyHack(createInvokableContext()));
        final EJB3Registrar value = this.ejb3RegistrarInjectedValue.getValue();
        final JndiSessionRegistrarBase registrar = getJndiSessionRegistrarBase(ejb3Data, value);
        final String containerGuid = containerName + ":" + UUID.randomUUID().toString();
        final AspectManager aspectManager = AspectManager.instance(beanClassLoader);
        // TODO: probably ClassAdvisor won't do
        final Advisor advisor = new ClassAdvisor(Object.class, aspectManager);
        final ClassLoader old = switchLoader(getClass().getClassLoader());
        InitialContext context = null;
        try {
            context = createJNPLocalContext();
            registrar.bindEjb(context, metaData, beanClassLoader, containerName, containerGuid, advisor);
        } finally {
            if (context != null)
                try {
                    context.close();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            try {
                Thread.currentThread().setContextClassLoader(old);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    protected void removeLegacyBinding() throws NamingException {
        final EJB3Registrar value = this.ejb3RegistrarInjectedValue.getValue();
        final JBossSessionBeanMetaData metaData = createMetaData(ejb3Data);
        final JndiSessionRegistrarBase registrar = getJndiSessionRegistrarBase(ejb3Data, value);
        final ClassLoader old = switchLoader(getClass().getClassLoader());
        InitialContext context = null;
        try {
            context = createJNPLocalContext();
            registrar.unbindEjb(context, metaData);
        } finally {
            if (context != null)
                try {
                    context.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            try {
                Thread.currentThread().setContextClassLoader(old);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected InitialContext createJNPLocalContext() throws NamingException {
        final Hashtable<String, String> env = new Hashtable<String, String>();
        env.put("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
        env.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
        final InitialContext context = new InitialContext(env);
        return context;
    }

    protected abstract InvokableContext createInvokableContext();

    protected JBossSessionBeanMetaData createMetaData(final EJBDataProxy data) {
        if (this.metadata != null) {
            return (JBossSessionBeanMetaData) metadata;
        }

        final JBossMetaData jarMetaData = new JBossMetaData();
        DeploymentSummary deploymentSumary = new DeploymentSummary();
        deploymentSumary.setDeploymentName(data.getDeploymentName());
        if(data.getDeploymentScopeBaseName() != null)
            deploymentSumary.setDeploymentScopeBaseName(data.getDeploymentScopeBaseName());
        //TODO: deploymentSummary.packagingType && deploymentSummary.loader 
        jarMetaData.setDeploymentSummary(deploymentSumary);
        jarMetaData.setEjbVersion(data.getEJBVersion());
        final JBossEnterpriseBeansMetaData enterpriseBeansMetaData = new JBossEnterpriseBeansMetaData();
        jarMetaData.setEnterpriseBeans(enterpriseBeansMetaData);
        enterpriseBeansMetaData.setEjbJarMetaData(jarMetaData);
        final JBossSessionBeanMetaData smd = new JBossSessionBeanMetaData();
        smd.setEnterpriseBeansMetaData(enterpriseBeansMetaData);
        smd.setEjbName(data.getName());
        enterpriseBeansMetaData.add(smd);
        final BusinessRemotesMetaData businessRemotes = new BusinessRemotesMetaData();
        businessRemotes.add(data.getRemoteInterfaceClass());
        smd.setBusinessRemotes(businessRemotes);
        MetadataUtil.decorateEjbsWithJndiPolicy(jarMetaData, data.getBeanClassLoader());
        this.metadata = (JBossSessionBeanMetaData) jarMetaData.getEnterpriseBean(smd.getName());
        return (JBossSessionBeanMetaData) this.metadata;
    }

    // protected EjbDeploymentInformation findBean() {
    // final ModuleDeployment module = deploymentRepositoryInjectedValue.getValue().getModules().get(new
    // DeploymentModuleIdentifier(this.applicationName, this.moduleName, this.distinctName));
    // if (module == null) {
    // throw MESSAGES.unknownDeployment(this.applicationName, this.moduleName, this.distinctName);
    // }
    // final EjbDeploymentInformation ejbInfo = module.getEjbs().get(this.componentName);
    // if (ejbInfo == null) {
    // throw MESSAGES.ejbNotFoundInDeployment(this.componentName, this.applicationName, this.moduleName, this.distinctName);
    // }
    // return ejbInfo;
    // }
    public static ServiceName getServiceName(final EEModuleDescription moduleDescription,
            final EJBComponentDescription ejbComponentDescription) {
        // TODO: what about ear/war/jar/ejb ?
        return SERVICE_NAME_BASE.of(SERVICE_NAME_BASE, moduleDescription.getEarApplicationName(),
                moduleDescription.getModuleName(), ejbComponentDescription.getComponentName());
    }
}
