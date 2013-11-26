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

package org.jboss.test;

import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * @author baranowb
 */
public class Test {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            Properties env = new Properties();
            env.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
            env.put("java.naming.factory.url.pkgs", "org.jboss.naming org.jnp.interfaces");
            env.setProperty(Context.PROVIDER_URL, "jnp://localhost:1099");
//            env.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.security.jndi.JndiLoginInitialContextFactory");
//            env.setProperty(Context.PROVIDER_URL, "jnp://localhost:1099/");
//            //password
//            env.setProperty(Context.SECURITY_CREDENTIALS, "test-xx");
//            //user
//            env.setProperty(Context.SECURITY_PRINCIPAL, "test-yy");
            InitialContext context = new InitialContext(env);
            Object o = context.lookup("CalculatorBean/remote");
            System.err.println(">>> " + o);
            RemoteCalculator rc = (RemoteCalculator) o;
            System.err.println(">>> " + rc.subtract(121, 11));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Properties env = new Properties();
            env.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
            env.put("java.naming.factory.url.pkgs", "org.jboss.naming org.jnp.interfaces");
            env.setProperty(Context.PROVIDER_URL, "jnp://localhost:1099");
            // env.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.security.jndi.JndiLoginInitialContextFactory");
            // env.setProperty(Context.PROVIDER_URL, "jnp://localhost:1099/");
            // env.setProperty(Context.SECURITY_CREDENTIALS, "test-xx");
            // env.setProperty(Context.SECURITY_PRINCIPAL, "test-yy ");
            InitialContext context = new InitialContext(env);
            Object o = context.lookup("StoryBean/remote");
            System.err.println(">>> " + o);
            RemoteStoryTeller rc = (RemoteStoryTeller) o;
            System.err.println(">>> " + rc.doTell());
            System.err.println(">>> " + rc.doTell());
            Thread.currentThread().sleep(1000);
            System.err.println(">>> " + rc.doTell());
            o = context.lookup("StoryBean/remote");
            System.err.println(">>> " + o);
            rc = (RemoteStoryTeller) o;
            System.err.println(">>> " + rc.doTell());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
// <security-domain name="ejb-security-domain" cache-type="default">
// <authentication>
// <login-module code="Remoting" flag="optional">
// <module-option name="password-stacking" value="useFirstPass"/>
// </login-module>
// <login-module code="org.jboss.security.auth.spi.UsersRolesLoginModule" flag="required">
// <module-option name="defaultUsersProperties" value="${jboss.server.config.dir}/ejb-users.properties"/>
// <module-option name="defaultRolesProperties" value="${jboss.server.config.dir}/ejb-roles.properties"/>
// <module-option name="usersProperties" value="${jboss.server.config.dir}/ejb-users.properties"/>
// <module-option name="rolesProperties" value="${jboss.server.config.dir}/ejb-roles.properties"/>
// <module-option name="password-stacking" value="useFirstPass"/>
// </login-module>
// </authentication>
// </security-domain>

