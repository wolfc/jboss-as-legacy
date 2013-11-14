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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.aop.Advisor;
import org.jboss.aop.AspectManager;
import org.jboss.aop.ClassAdvisor;
import org.jboss.aop.Dispatcher;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.InvocationResponse;
import org.jboss.aop.joinpoint.MethodInvocation;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.EjbDeploymentMarker;
import org.jboss.ejb3.common.lang.SerializableMethod;
import org.jboss.ejb3.common.metadata.MetadataUtil;
import org.jboss.ejb3.proxy.impl.jndiregistrar.JndiSessionRegistrarBase;
import org.jboss.ejb3.proxy.impl.remoting.SessionSpecRemotingMetadata;
import org.jboss.ejb3.proxy.spi.container.InvokableContext;
import org.jboss.legacy.common.EJBDataProxy;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;
import org.jboss.metadata.ejb.spec.BusinessRemotesMetaData;
import org.jboss.msc.service.ServiceController;
/**
 * Processor to hook up EJB with nice JNP/AOP binding.
 * @author baranowb
 */
public class EJB3DeploymentProcessor implements DeploymentUnitProcessor {

    public static final EJB3DeploymentProcessor INSTANCE = new EJB3DeploymentProcessor();
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!EjbDeploymentMarker.isEjbDeployment(deploymentUnit)) {
            return;
        }

        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription != null) {

            for (final ComponentDescription componentDescription : moduleDescription.getComponentDescriptions()) {
                if (componentDescription instanceof EJBComponentDescription) {
                    try {
                        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentDescription;

                        ejbComponentDescription.getConfigurators().add(new ComponentConfigurator() {
                            public void configure(DeploymentPhaseContext context, ComponentDescription description,
                                    ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                                final EJBDataProxy data = deploymentUnit.getAttachment(EJBDataProxy.ATTACHMENT_KEY);
                                if (data != null) {
                                    createLegacyBinding(data, phaseContext);
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        if (!EjbDeploymentMarker.isEjbDeployment(deploymentUnit)) {
            return;
        }
        final EJBDataProxy data = deploymentUnit.getAttachment(EJBDataProxy.ATTACHMENT_KEY);
        if (data != null) {
            removeLegacyBinding(data, deploymentUnit);
        }
    }
    /**
     * @param data
     * @param phaseContext
     */
    private void createLegacyBinding(final EJBDataProxy data, final DeploymentPhaseContext phaseContext) {
        final JBossSessionBeanMetaData metaData = createMetaData(data);
        final String containerName = metaData.getName();
        final ClassLoader beanClassLoader = data.getBeanClassLoader();
        Dispatcher.singleton.registerTarget(containerName, new InvokableContextClassProxyHack(new InvokableContext() {
            @Override
            public Object invoke(Object proxy, SerializableMethod method, Object[] args) throws Throwable {
                throw new RuntimeException("NYI: .invoke");
            }

            @Override
            public InvocationResponse dynamicInvoke(Invocation invocation) throws Throwable {
                final MethodInvocation si = (MethodInvocation) invocation;
                final SerializableMethod invokedMethod = (SerializableMethod) si.getMetaData(
                        SessionSpecRemotingMetadata.TAG_SESSION_INVOCATION, SessionSpecRemotingMetadata.KEY_INVOKED_METHOD);

                ClassLoader invocationCL = switchLoader(beanClassLoader);
                try {
                    // business logic
                    final InitialContext ic = new InitialContext();
                    final Object proxy = ic.lookup(data.getLocalASBinding());
                    final Method method = invokedMethod.toMethod();
                    final Object returnValue = method.invoke(proxy, si.getArguments());
                    final InvocationResponse response = new InvocationResponse(returnValue);
                    return response;
                } finally {
                    switchLoader(invocationCL);
                }
            }
        }));

        final ServiceController<EJB3Registrar> controller = (ServiceController<EJB3Registrar>) phaseContext
                .getServiceRegistry().getService(EJB3RegistrarService.SERVICE_NAME);
        final EJB3Registrar value = controller.getValue();
        final JndiSessionRegistrarBase registrar = getJndiSessionRegistrarBase(data,value);
        final String containerGuid = containerName + ":" + UUID.randomUUID().toString();
        final AspectManager aspectManager = AspectManager.instance(beanClassLoader);
        // TODO: probably ClassAdvisor won't do
        final Advisor advisor = new ClassAdvisor(Object.class, aspectManager);
        final ClassLoader old = switchLoader(getClass().getClassLoader());
        try {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
            env.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
            InitialContext context = new InitialContext(env);
            registrar.bindEjb(context, metaData, beanClassLoader, containerName, containerGuid, advisor);
        } catch (NamingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private void removeLegacyBinding(final EJBDataProxy data, final DeploymentUnit deploymentUnit){
        final ServiceController<EJB3Registrar> controller = (ServiceController<EJB3Registrar>) deploymentUnit
                .getServiceRegistry().getService(EJB3RegistrarService.SERVICE_NAME);
        final EJB3Registrar value = controller.getValue();
        final JBossSessionBeanMetaData metaData = createMetaData(data);
        final JndiSessionRegistrarBase registrar = getJndiSessionRegistrarBase(data,value);
        final ClassLoader old = switchLoader(getClass().getClassLoader());
        try {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
            env.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
            InitialContext context = new InitialContext(env);
            registrar.unbindEjb(context, metaData);
        } catch (NamingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private JBossSessionBeanMetaData createMetaData(final EJBDataProxy data) {
        final JBossMetaData jarMetaData = new JBossMetaData();
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
        return (JBossSessionBeanMetaData) jarMetaData.getEnterpriseBean(smd.getName());
    }

    private ClassLoader switchLoader(final ClassLoader loader){
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        return current;
    }

    private JndiSessionRegistrarBase getJndiSessionRegistrarBase(final EJBDataProxy data, final EJB3Registrar registrarService){
        return data.isStateful() ? registrarService.getJndiStatefulSessionRegistrar() : registrarService.getJndiStatelessSessionRegistrar();
    }
}
