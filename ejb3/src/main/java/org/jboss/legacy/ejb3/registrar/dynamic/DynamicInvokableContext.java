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

import com.arjuna.ats.arjuna.common.Uid;
import java.lang.reflect.Method;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.InvocationResponse;
import org.jboss.aop.joinpoint.MethodInvocation;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.utils.DescriptorUtils;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.aspects.tx.ClientTxPropagationInterceptor;
import org.jboss.ejb3.common.lang.SerializableMethod;
import org.jboss.ejb3.proxy.impl.jndiregistrar.JndiSessionRegistrarBase;
import org.jboss.ejb3.proxy.impl.remoting.SessionSpecRemotingMetadata;
import org.jboss.ejb3.proxy.spi.container.InvokableContext;
import org.jboss.invocation.InterceptorContext;
import org.jboss.legacy.common.EJBDataProxy;
import org.jboss.legacy.ejb3.registrar.EJB3Registrar;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.plugins.JBossSecurityContext;

/**
 * Handles magic moombo jumbo invocation.
 *
 * @author baranowb
 */
public class DynamicInvokableContext implements InvokableContext {

    public static final String LEGACY_MD_SECURITY = "security";
    public static final String LEGACY_MD_KEY_PRINCIPIAL = "principal";
    public static final String LEGACY_MD_KEY_CREDENTIAL = "credential";
    public static final String LEGACY_MD_KEY_CONTEXT = "context";
    public static final String LEGACY_TX_KEY_CONTEXT = "TransactionPropagationContext";
    public static final String LEGACY_TX_KEY_ATTRIBUTE = "TransactionPropagationContext";

    private EjbDeploymentInformation info;

    //just a ref copy, but its better to keep classes scoped, inner classes become messy
    //if they have more than few lines
    protected final EJBDataProxy ejb3Data;
    protected final InjectedValue<ServerSecurityManager> serverSecurityManagerInjectedValue;
    protected final InjectedValue<EJB3Registrar> ejb3RegistrarInjectedValue;
    protected final InjectedValue<DeploymentRepository> deploymentRepositoryInjectedValue;
    protected final InjectedValue<ComponentView> viewInjectedValue;
    protected final String applicationName;
    protected final String moduleName;
    protected final String distinctName;
    protected final String componentName;

    /**
     * @param ejb3Data
     * @param serverSecurityManagerInjectedValue
     * @param ejb3RegistrarInjectedValue
     * @param deploymentRepositoryInjectedValue
     * @param viewInjectedValue
     * @param applicationName
     * @param moduleName
     * @param distinctName
     * @param componentName
     */
    public DynamicInvokableContext(EJBDataProxy ejb3Data, InjectedValue<ServerSecurityManager> serverSecurityManagerInjectedValue,
            InjectedValue<EJB3Registrar> ejb3RegistrarInjectedValue,
            InjectedValue<DeploymentRepository> deploymentRepositoryInjectedValue,
            InjectedValue<ComponentView> viewInjectedValue, String applicationName, String moduleName, String distinctName,
            String componentName) {
        super();
        this.ejb3Data = ejb3Data;
        this.serverSecurityManagerInjectedValue = serverSecurityManagerInjectedValue;
        this.ejb3RegistrarInjectedValue = ejb3RegistrarInjectedValue;
        this.deploymentRepositoryInjectedValue = deploymentRepositoryInjectedValue;
        this.viewInjectedValue = viewInjectedValue;
        this.applicationName = applicationName;
        this.moduleName = moduleName;
        this.distinctName = distinctName;
        this.componentName = componentName;
    }

    @Override
    public Object invoke(Object proxy, SerializableMethod method, Object[] args) throws Throwable {
        throw new RuntimeException("NYI: .invoke");
    }

    @Override
    public InvocationResponse dynamicInvoke(Invocation invocation) throws Throwable {
        final MethodInvocation si = (MethodInvocation) invocation;
        final JBossSecurityContext context = (JBossSecurityContext) si.getMetaData(LEGACY_MD_SECURITY, LEGACY_MD_KEY_CONTEXT);
        ClassLoader invocationCL = switchLoader(this.ejb3Data.getBeanClassLoader());
        try {
            //if (context != null)
            setupSecurity(si, context);
            final InterceptorContext customContext = createInterceptorContext(si);
            final Object returnValue = transactionalInvokation(si, customContext);
            return new InvocationResponse(returnValue);
        } finally {
            switchLoader(invocationCL);
        }
    }

    protected void setupSecurity(final MethodInvocation si, final JBossSecurityContext context) {
        final String securityDomain = context.getSecurityDomain();
        final ServerSecurityManager serverSecurityManager = serverSecurityManagerInjectedValue.getValue();
        final Object principal = si.getMetaData(LEGACY_MD_SECURITY, LEGACY_MD_KEY_PRINCIPIAL);
        final Object credential = si.getMetaData(LEGACY_MD_SECURITY, LEGACY_MD_KEY_CREDENTIAL);
        if (principal != null && credential != null) {
            serverSecurityManager.push(securityDomain, principal.toString(), credential.toString().toCharArray(), context
                    .getSubjectInfo().getAuthenticatedSubject());
        }
    }

    protected InterceptorContext createInterceptorContext(final MethodInvocation si) throws NamingException {
        final InitialContext ic = new InitialContext();
        final SerializableMethod invokedMethod = (SerializableMethod) si.getMetaData(
                SessionSpecRemotingMetadata.TAG_SESSION_INVOCATION, SessionSpecRemotingMetadata.KEY_INVOKED_METHOD);
        final ComponentView view = viewInjectedValue.getValue();
        final InterceptorContext customContext = new InterceptorContext();
        // Just a copy paste: TODO: this is not very efficient
        final Method method = view.getMethod(invokedMethod.toMethod().getName(), DescriptorUtils.methodDescriptor(invokedMethod.toMethod()));
        customContext.setMethod(method);
        customContext.setParameters(si.getArguments());
        customContext.setTarget(ic.lookup(ejb3Data.getLocalASBinding()));
        // setup private data
        final EjbDeploymentInformation ejb = findBean();
        final EJBComponent ejbComponent = ejb.getEjbComponent();
        customContext.putPrivateData(ComponentView.class, view);
        customContext.putPrivateData(Component.class, ejbComponent);
        return customContext;
    }

    public Object transactionalInvokation(final MethodInvocation invocation, final InterceptorContext customContext) throws Throwable {
        TransactionManager tm = ((EJBComponent) viewInjectedValue.getValue().getComponent()).getTransactionManager();
        Object tpc = invocation.getMetaData(ClientTxPropagationInterceptor.TRANSACTION_PROPAGATION_CONTEXT,
                ClientTxPropagationInterceptor.TRANSACTION_PROPAGATION_CONTEXT);
        if (tpc != null) {
            Transaction tx = tm.getTransaction();
            if (tx != null) {
                throw new RuntimeException("cannot import a transaction context when a transaction is already associated with the thread");
            }
            Transaction importedTx = importTPC(tpc);
            tm.resume(importedTx);
            try {
                return doRealInvocation(customContext);
            } finally {
                tm.suspend();
            }
        } else {
            return doRealInvocation(customContext);
        }
    }

    private Object doRealInvocation(final InterceptorContext customContext) throws Throwable {
        final ComponentView view = viewInjectedValue.getValue();
        return view.invoke(customContext);
    }

    protected Transaction importTPC(Object tpc) throws NamingException {
        Uid importedTx = new Uid((String) tpc);
        return com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple.getTransaction(importedTx);
    }

    protected EjbDeploymentInformation findBean() {
        if (this.info != null) {
            return this.info;
        }
        final ModuleDeployment module = deploymentRepositoryInjectedValue.getValue().getModules().get(new DeploymentModuleIdentifier(this.applicationName, this.moduleName, this.distinctName));
        if (module == null) {
            throw MESSAGES.unknownDeployment(this.applicationName, this.moduleName, this.distinctName);
        }
        this.info = module.getEjbs().get(this.componentName);
        if (this.info == null) {
            throw MESSAGES.ejbNotFoundInDeployment(this.componentName, this.applicationName, this.moduleName, this.distinctName);
        }
        return this.info;
    }

    public static ClassLoader switchLoader(final ClassLoader loader) {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        return current;
    }

    public static JndiSessionRegistrarBase getJndiSessionRegistrarBase(final EJBDataProxy data,
            final EJB3Registrar registrarService) {
        return data.isStateful() ? registrarService.getJndiStatefulSessionRegistrar() : registrarService
                .getJndiStatelessSessionRegistrar();
    }

}
