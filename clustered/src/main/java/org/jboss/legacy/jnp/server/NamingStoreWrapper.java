/*
 * Copyright (C) 2013 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.legacy.jnp.server;

import java.rmi.RemoteException;
import java.util.Collection;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jnp.interfaces.Naming;
import org.jnp.interfaces.NamingContext;
import org.jnp.server.EventMgr;
import org.jnp.server.NamingServer;
import org.jnp.server.SingletonNamingServer;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class NamingStoreWrapper implements Naming {

    //private final SingletonNamingServer singletonNamingServer;
    private final WickedNaming singletonNamingServer;
    private final NamingStore namingStore;

    public NamingStoreWrapper(ServiceBasedNamingStore namingStore) throws NamingException {
        this.namingStore = namingStore;
        //this.singletonNamingServer = new SingletonNamingServer();
        this.singletonNamingServer = new WickedNaming();
    }

    @Override
    public void bind(Name name, Object obj, String className) throws NamingException, RemoteException {
        singletonNamingServer.bind(name, obj, className);
    }

    @Override
    public void rebind(Name name, Object obj, String className) throws NamingException, RemoteException {
        singletonNamingServer.rebind(name, obj, className);
    }

    @Override
    public void unbind(Name name) throws NamingException, RemoteException {
        singletonNamingServer.unbind(name);
    }

    @Override
    public Object lookup(Name name) throws NamingException, RemoteException {
        try {
            return singletonNamingServer.lookup(name);
        } catch (Exception t) {
            return namingStore.lookup(name);
        }
    }

    @Override
    public Collection<NameClassPair> list(Name name) throws NamingException, RemoteException {
        try {
            return singletonNamingServer.list(name);
        } catch (Exception t) {
            return namingStore.list(name);
        }
    }

    @Override
    public Collection<Binding> listBindings(Name name) throws NamingException, RemoteException {
        try {
            return singletonNamingServer.listBindings(name);
        } catch (Exception t) {
            return namingStore.listBindings(name);
        }
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException, RemoteException {
        return singletonNamingServer.createSubcontext(name);
    }

    class WickedNaming extends NamingServer {

        public WickedNaming() throws NamingException {
            NamingContext.setLocal(this);
        }

        public void destroy() {
            NamingContext.setLocal(null);
        }

        public WickedNaming(Name prefix, NamingServer parent, EventMgr eventMgr, SecurityManager secMgr) throws NamingException {
            super(prefix, parent, eventMgr, secMgr);
            // TODO Auto-generated constructor stub
        }

        public WickedNaming(Name prefix, NamingServer parent, EventMgr eventMgr) throws NamingException {
            super(prefix, parent, eventMgr);
            // TODO Auto-generated constructor stub
        }

        public WickedNaming(Name prefix, NamingServer parent) throws NamingException {
            super(prefix, parent);
            // TODO Auto-generated constructor stub
        }

        //override this method and sync it. Utils use this directly, rather than through NamingServer.bind>createSubContext
        //bind is synchronized, this one isnt, hence error conditions on bind....
        @Override
        public synchronized Context createSubcontext(Name name) throws NamingException {
            //in case of:
            //createSubCOntext("1/2/3")
            //createSubContext("1/5")
            //second call will fail, since there is binding at 1 - NamingServer does not check "instanceof Context ? return (Context) value;"
            try{
                return super.createSubcontext(name);
            } catch (NamingException e) {
                Object value = super.lookup(name);
                if(value instanceof Context){
                    return (Context) value;
                } else {
                    throw e;
                }
            }
        }

        @Override
        protected NamingServer createNamingServer(Name prefix, NamingServer parent) throws NamingException {
            return new WickedNaming(prefix, parent);
        }
    }
}
