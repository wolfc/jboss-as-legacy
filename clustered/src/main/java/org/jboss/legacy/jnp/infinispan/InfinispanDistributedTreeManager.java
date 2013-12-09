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
package org.jboss.legacy.jnp.infinispan;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import org.infinispan.Cache;
import org.infinispan.CacheException;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.TreeCache;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCacheFactory;
import org.jboss.ha.jndi.spi.DistributedTreeManager;
import org.jboss.logging.Logger;
import org.jnp.interfaces.Naming;
import org.jnp.interfaces.NamingContext;
import org.jnp.interfaces.NamingParser;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class InfinispanDistributedTreeManager implements org.jnp.interfaces.Naming, DistributedTreeManager {

    static final long serialVersionUID = 6342802270002172451L;

    private static Logger log = Logger.getLogger(InfinispanDistributedTreeManager.class);

    private static final NamingParser parser = new NamingParser();

    public static final String DEFAULT_ROOT = "__HA_JNDI__";

    private TreeCache<String, Binding> cache;
    private Fqn m_root;
    private Naming haStub;
    private boolean treeRootSet;
    protected boolean acquiredCache = false;

    public InfinispanDistributedTreeManager() {
        super();
    }

    public Cache<String, Binding> getClusteredCache() {
        return (Cache<String, Binding>) cache.getCache();
    }

    public void setClusteredCache(Cache<String, Binding> cache) {
        if (treeRootSet) {
            throw new IllegalStateException("Cannot change clusteredCache after call to init()");
        }
        this.cache = new TreeCacheFactory().createTreeCache(cache);
    }

    public void setRootFqn(String rootFqn) {
        if (treeRootSet) {
            throw new IllegalStateException("Cannot change rootFqn after call to init()");
        }

        m_root = (rootFqn == null) ? null : Fqn.fromString(rootFqn);
    }

    public String getRootFqn() {
        return m_root == null ? DEFAULT_ROOT : m_root.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init() {
        if (this.cache == null) {
            throw new IllegalStateException("No clustered cache available");
        }

        log.debug("initializing HAJNDITreeCache root");
        this.putTreeRoot();
    }

    @Override
    public void shutdown() {
        cache.stop();
    }

    @Override
    public Naming getHAStub() {
        return this.haStub;
    }

    @Override
    public void setHAStub(Naming stub) {
        this.haStub = stub;
    }

    @Override
    public void bind(Name name, Object obj, String className) throws NamingException {
        if (log.isTraceEnabled()) {
            log.trace("bind, name=" + name);
        }

        this.internalBind(name, obj, className, false);
    }

    @Override
    public void rebind(Name name, Object obj, String className) throws NamingException {
        if (log.isTraceEnabled()) {
            log.trace("rebind, name=" + name);
        }

        this.internalBind(name, obj, className, true);
    }

    @Override
    public void unbind(Name name) throws NamingException {
        if (log.isTraceEnabled()) {
            log.trace("unbind, name=" + name);
        }
        if (name.isEmpty()) {
            // Empty names are not allowed
            throw new InvalidNameException();
        }

        // is the name a context?
        try {
            Fqn temp = Fqn.fromRelativeFqn(this.m_root, Fqn.fromString(name.toString()));
            // TODO why not jst call remove -- why hasChild first?
            if (this.cache.getRoot().hasChild(temp)) {
                this.cache.removeNode(temp);
                return;
            }
        } catch (CacheException ce) {
            // don't chain CacheException since JBoss Cache may not be on remote client's classpath
            NamingException ne = new NamingException(ce.getClass().getName() + ": " + ce.getMessage());
            ne.setStackTrace(ce.getStackTrace());
            throw ne;
        }

        int size = name.size();

        // get the context and key
        Fqn ctx;
        String key = name.get(size - 1);
        if (size > 1) {
            String prefix = name.getPrefix(size - 1).toString();
            Fqn fqn = Fqn.fromString(prefix);
            ctx = Fqn.fromRelativeFqn(this.m_root, fqn);
        } else {
            ctx = this.m_root;
        }

        try {
            Object removed = this.cache.remove(ctx, key);
            if (removed == null) {
                if (!this.cache.getRoot().hasChild(ctx)) {
                    throw new NotContextException(name.getPrefix(size - 1).toString() + " not a context");
                }

                throw new NameNotFoundException(key + " not bound");
            }
        } catch (CacheException ce) {
            // don't chain CacheException since JBoss Cache may not be on remote client's classpath
            NamingException ne = new NamingException(ce.getClass().getName() + ": " + ce.getMessage());
            ne.setStackTrace(ce.getStackTrace());
            throw ne;
        }
    }

    public Object lookup(Name name) throws NamingException {
        boolean trace = log.isTraceEnabled();
        if (trace) {
            log.trace("lookup, name=" + name);
        }

        if (name.isEmpty()) {
            // Return this
            return new NamingContext(null, parser.parse(""), this.getHAStub());
        }

        // is the name a context?
        try {
            Node<String, Binding> n = this.cache.getRoot().getChild(Fqn.fromRelativeFqn(this.m_root, Fqn.fromString(name.toString())));
            if (n != null) {
                Name fullName = (Name) name.clone();
                return new NamingContext(null, fullName, this.getHAStub());
            }
        } catch (CacheException ce) {
            // don't chain CacheException since JBoss Cache may not be on remote client's classpath
            NamingException ne = new NamingException(ce.getClass().getName() + ": " + ce.getMessage());
            ne.setStackTrace(ce.getStackTrace());
            throw ne;
        }

        int size = name.size();

        // get the context and key
        Fqn ctx;
        String key = name.get(size - 1);
        if (size > 1) {
            String prefix = name.getPrefix(size - 1).toString();
            Fqn fqn = Fqn.fromString(prefix);
            ctx = Fqn.fromRelativeFqn(this.m_root, fqn);
        } else {
            ctx = this.m_root;
        }

        try {
            Binding b = this.cache.get(ctx, key);

            // if key not in cache, return null
            return (b != null) ? b.getObject() : null;
        } catch (CacheException ce) {
            // don't chain CacheException since JBoss Cache may not be on remote client's classpath
            NamingException ne = new NamingException(ce.getClass().getName() + ": " + ce.getMessage());
            ne.setStackTrace(ce.getStackTrace());
            throw ne;
        }
    }

    public Collection<NameClassPair> list(Name name) throws NamingException {
        if (log.isTraceEnabled()) {
            log.trace("list, name=" + name);
        }

        // get the context
        Fqn ctx;
        String ctxName = "";
        int size = name.size();
        if (size >= 1) {
            ctxName = name.toString();
            Fqn fqn = Fqn.fromString(ctxName);
            ctx = Fqn.fromRelativeFqn(this.m_root, fqn);
        } else {
            ctx = this.m_root;
        }

        boolean exists = this.cache.getRoot().hasChild(ctx);
        if (!exists) {
            try {
                return Collections.list(new InitialContext().list(name));
            } catch (NamingException e) {
                throw new NotContextException(ctxName + " not a context");
            }
        }

        try {
            List<NameClassPair> list = new LinkedList<NameClassPair>();

            Node<String, Binding> base = this.cache.getRoot().getChild(ctx);
            if (base != null) {
                for (Binding b : base.getData().values()) {
                    list.add(new NameClassPair(b.getName(), b.getClassName(), true));
                }

                // Why doesn't this return Set<String>?
                Set<Object> children = base.getChildrenNames();
                if (children != null && !children.isEmpty()) {
                    for (Object child : children) {
                        String node = (String) child;
                        Name fullName = (Name) name.clone();
                        fullName.add(node);
                        list.add(new NameClassPair(node, NamingContext.class.getName(), true));
                    }
                }
            }

            return list;
        } catch (CacheException ce) {
            // don't chain CacheException since JBoss Cache may not be on remote client's classpath
            NamingException ne = new NamingException(ce.getClass().getName() + ": " + ce.getMessage());
            ne.setStackTrace(ce.getStackTrace());
            throw ne;
        }
    }

    public Collection<Binding> listBindings(Name name) throws NamingException {
        if (log.isTraceEnabled()) {
            log.trace("listBindings, name=" + name);
        }

        // get the context
        Fqn ctx;
        String ctxName = "";
        int size = name.size();
        if (size >= 1) {
            ctxName = name.toString();
            Fqn fqn = Fqn.fromString(ctxName);
            ctx = Fqn.fromRelativeFqn(this.m_root, fqn);
        } else {
            ctx = this.m_root;
        }

        boolean exists = this.cache.getRoot().hasChild(ctx);
        if (!exists) {
            // not found in global jndi, look in local.
            try {
                return Collections.list(new InitialContext().listBindings(name));
            } catch (NamingException e) {
                throw new NotContextException(ctxName + " not a context");
            }
        }

        try {
            List<Binding> list = new LinkedList<Binding>();

            Node<String, Binding> node = this.cache.getRoot().getChild(ctx);
            if (node != null) {
                Map<String, Binding> data = node.getData();
                if (data != null && !data.isEmpty()) {
                    list.addAll(data.values());
                }

                // Why doesn't this return Set<String>?
                Set<Object> children = node.getChildrenNames();
                if (children != null && !children.isEmpty()) {
                    for (Object obj : children) {
                        String child = (String) obj;
                        Name fullName = (Name) name.clone();
                        fullName.add(child);
                        NamingContext subCtx = new NamingContext(null, fullName, this.getHAStub());
                        list.add(new Binding(child, NamingContext.class.getName(), subCtx, true));
                    }
                }
            }

            return list;
        } catch (CacheException ce) {
            // don't chain CacheException since JBoss Cache may not be on remote client's classpath
            NamingException ne = new NamingException(ce.getClass().getName() + ": " + ce.getMessage());
            ne.setStackTrace(ce.getStackTrace());
            throw ne;
        }
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
        if (log.isTraceEnabled()) {
            log.trace("createSubcontext, name=" + name);
        }

        int size = name.size();

        if (size == 0) {
            throw new InvalidNameException("Cannot pass an empty name to createSubcontext");
        }

        // does the new context already exist?
        String str = name.toString();
        Fqn fqn = Fqn.fromString(str);
        Fqn ctx = Fqn.fromRelativeFqn(this.m_root, fqn);
        if (this.cache.getRoot().hasChild(ctx)) {
            throw new NameAlreadyBoundException();
        }

        // does the prefix context already exist?
        Fqn pctx;
        String newctx = name.get(size - 1);
        if (size > 1) {
            String prefix = name.getPrefix(size - 1).toString();
            Fqn fqn2 = Fqn.fromString(prefix);
            pctx = Fqn.fromRelativeFqn(this.m_root, fqn2);
        } else {
            pctx = this.m_root;
        }

        boolean exists = this.cache.getRoot().hasChild(pctx);
        if (!exists) {
            throw new NotContextException(name.getPrefix(size - 1).toString());
        }

        Fqn newf = Fqn.fromRelativeFqn(pctx, Fqn.fromString(newctx));
        try {
            this.cache.put(newf, new HashMap<String, Binding>());
        } catch (CacheException ce) {
            // don't chain CacheException since JBoss Cache may not be on remote client's classpath
            NamingException ne = new NamingException(ce.getClass().getName() + ": " + ce.getMessage());
            ne.setStackTrace(ce.getStackTrace());
            throw ne;
        }

        Name fullName = parser.parse("");
        fullName.addAll(name);

        return new NamingContext(null, fullName, this.getHAStub());
    }

    private void putTreeRoot() throws CacheException {
        if (this.m_root == null) {
            setRootFqn(DEFAULT_ROOT);
        }

        if (!this.cache.getRoot().hasChild(this.m_root)) {
            this.cache.put(this.m_root, Collections.<String, Binding>emptyMap());
            this.treeRootSet = true;
        }
    }

    private void internalBind(Name name, Object obj, String className, boolean rebind) throws NamingException {
        if (name.isEmpty()) {  // Empty names are not allowed
            throw new InvalidNameException();
        }

        int size = name.size();

        // get the context and key
        Fqn ctx;
        String key = name.get(size - 1);
        if (size > 1) {
            String prefix = name.getPrefix(size - 1).toString();
            Fqn fqn = Fqn.fromString(prefix);
            ctx = Fqn.fromRelativeFqn(this.m_root, fqn);
        } else {
            ctx = this.m_root;
        }

        boolean exists = this.cache.getRoot().hasChild(ctx);
        if (!exists) {
            throw new NotContextException(name.getPrefix(size - 1).toString() + " not a context");
            // note - NamingServer throws a CannotProceedException if the client attempts to bind
            //        to a Reference object having an "nns" address.  This implementation simply
            //        throws the NotContextException that's used when "nns" isn't present.
        }
        if (!rebind) {
            Node<String, Binding> node = this.cache.getRoot().getChild(ctx);
            if ((node != null) && (node.get(key) != null)) {
                throw new NameAlreadyBoundException(key);
            }
        }

        this.cache.put(ctx, key, new Binding(key, className, obj, true));
    }
}
