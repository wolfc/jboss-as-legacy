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

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
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
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.EjbDeploymentMarker;
import org.jboss.ejb3.common.lang.SerializableMethod;
import org.jboss.ejb3.common.metadata.MetadataUtil;
import org.jboss.ejb3.proxy.impl.jndiregistrar.JndiStatelessSessionRegistrar;
import org.jboss.ejb3.proxy.impl.remoting.SessionSpecRemotingMetadata;
import org.jboss.ejb3.proxy.spi.container.InvokableContext;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;
import org.jboss.metadata.ejb.spec.BusinessRemotesMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeansMetaData;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Values;

/**
 * Processor to hook up EJB with nice JNP/AOP binding.
 * @author baranowb
 */
public class EJB3DeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        System.err.println("DOING: " + deploymentUnit.getName() + " > " + EjbDeploymentMarker.isEjbDeployment(deploymentUnit));
        final ClassLoader moduleClassLoader = this.getClass().getClassLoader();
        if (!EjbDeploymentMarker.isEjbDeployment(deploymentUnit)) {
            return;
        }
        // for each remote interface we rock
        final EjbJarMetaData ejbMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbMetaData != null) {
            EnterpriseBeansMetaData data = ejbMetaData.getEnterpriseBeans();
            if (data != null) {
                Iterator<EnterpriseBeanMetaData> it = data.iterator();
                while (it.hasNext()) {
                    EnterpriseBeanMetaData ebmd = it.next();
                    System.err.println(">> " + ebmd);
                }
            } else {
                System.err.println("METADATA2 IS NULL!");
            }
        } else {
            System.err.println("METADATA IS NULL!");
        }
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription != null) {

            for (final ComponentDescription componentDescription : moduleDescription.getComponentDescriptions()) {
                if (componentDescription instanceof EJBComponentDescription) {
                    try {
                        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentDescription;
                        final InjectedValue<ClassLoader> viewClassLoader = new InjectedValue<ClassLoader>();

                        ejbComponentDescription.getConfigurators().add(new ComponentConfigurator() {
                            public void configure(DeploymentPhaseContext context, ComponentDescription description,
                                    ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                                viewClassLoader.setValue(Values.immediateValue(configuration.getModuleClassLoader()));

                                EJBViewDescription remoteView = ejbComponentDescription.getEjbRemoteView();
                                ejbComponentDescription.getViews();
                                if (remoteView == null) {
                                    System.err.println("No EJB 2.x Remote View: " + ejbComponentDescription.getEJBClassName());
                                } else {

                                    System.err.println("Removet view '" + ejbComponentDescription.getEJBClassName() + "' > '"
                                            + remoteView.getViewClassName() + "'");
                                }

                                Set<ViewDescription> views = ejbComponentDescription.getViews();
                                for (ViewDescription vd : views) {
                                    final MethodIntf viewType = ((EJBViewDescription) vd).getMethodIntf();
                                    if (viewType == MethodIntf.REMOTE || viewType == MethodIntf.HOME) {
                                        System.err.println("Remote 3.x VIEW '" + ejbComponentDescription.getEJBClassName()
                                                + "' > '" + vd.getViewClassName() + "'");
                                        doTestBind(vd.getViewClassName(), phaseContext, viewClassLoader, moduleClassLoader);
                                    } else {
                                        System.err.println("NON Remote 3.x VIEW '" + ejbComponentDescription.getEJBClassName()
                                                + "' > '" + vd.getViewClassName() + "'");
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            System.err.println("NO EEModuleDescription");
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

    private static JBossSessionBeanMetaData createMetaData(String remoteClass) {
        final JBossMetaData jarMetaData = new JBossMetaData();
        jarMetaData.setEjbVersion("3.0");
        final JBossEnterpriseBeansMetaData enterpriseBeansMetaData = new JBossEnterpriseBeansMetaData();
        jarMetaData.setEnterpriseBeans(enterpriseBeansMetaData);
        enterpriseBeansMetaData.setEjbJarMetaData(jarMetaData);
        final JBossSessionBeanMetaData smd = new JBossSessionBeanMetaData();
        smd.setEnterpriseBeansMetaData(enterpriseBeansMetaData);
        smd.setEjbName("CalculatorBean");
        enterpriseBeansMetaData.add(smd);
        final BusinessRemotesMetaData businessRemotes = new BusinessRemotesMetaData();
        businessRemotes.add(remoteClass);
        smd.setBusinessRemotes(businessRemotes);
        // // TODO: normally this is resolved through the JNDI decorator
        // smd.setJndiName("GreeterBean");
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        MetadataUtil.decorateEjbsWithJndiPolicy(jarMetaData, cl);
        return (JBossSessionBeanMetaData) jarMetaData.getEnterpriseBean(smd.getName());
    }

    private void doTestBind(String remoteClassName, DeploymentPhaseContext phaseContext,
            InjectedValue<ClassLoader> viewClassLoader, final ClassLoader moduleClassLoader) {
        try {
            Thread.currentThread().sleep(500);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        JBossSessionBeanMetaData smd = createMetaData(remoteClassName);
        final String containerName = smd.getName();
        // Register with AOP
        // phaseContext.getServiceTarget().get
        final ClassLoader cl = viewClassLoader.getValue();
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
                final Class<?> invokedBusinessInterface = Class.forName(invokedMethod.getActualClassName(), false, cl);
                System.err.println("Invoked business interface = " + invokedBusinessInterface);
                System.err.println("Invoked method = " + si.getActualMethod());
                System.err.println("Arguments = " + Arrays.toString(si.getArguments()));

                System.err.println("Method: "+si.getMethod());
                System.err.println("Method: "+si.getActualMethod());
                // business logic
                for(Object arg: si.getArguments())
                    System.err.println("ARG["+arg+"]");

                final InvocationResponse response = new InvocationResponse(new Integer(300));
                // response.setContextInfo();
                return response;
            }
        }));
        ServiceController<EJB3Registrar> controller = (ServiceController<EJB3Registrar>) phaseContext
                .getServiceRegistry().getService(EJB3RegistrarService.SERVICE_NAME);
        EJB3Registrar value = controller.getValue();
        //TODO: this still fails!
        final JndiStatelessSessionRegistrar registrar = value.getJndiStatelessSessionRegistrar();
        final String containerGuid = containerName + ":" + UUID.randomUUID().toString();
        final AspectManager aspectManager = AspectManager.instance(cl);
        // TODO: probably ClassAdvisor won't do
        final Advisor advisor = new ClassAdvisor(Object.class, aspectManager);
        final ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(moduleClassLoader);
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
            env.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
            InitialContext context = new InitialContext(env);
            registrar.bindEjb(context, smd, cl, containerName, containerGuid, advisor);
        } catch (NamingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }

    }
}
