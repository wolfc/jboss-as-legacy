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

import org.infinispan.Cache;
import static org.jboss.legacy.jnp.JNPSubsystemModel.LEGACY;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class DistributedTreeManagerService implements Service<InfinispanDistributedTreeManager> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(LEGACY).append(DistributedTreeManagerModel.SERVICE_NAME);

    private InfinispanDistributedTreeManager treeManager;
    private final InjectedValue<Cache> cache = new InjectedValue<Cache>();

    @Override
    public void start(StartContext context) throws StartException {
            this.treeManager = new InfinispanDistributedTreeManager();
            this.treeManager.setClusteredCache(cache.getValue());
    }

    @Override
    public void stop(StopContext context) {
        this.treeManager = null;
    }

    @Override
    public InfinispanDistributedTreeManager getValue() throws IllegalStateException, IllegalArgumentException {
        return this.treeManager;
    }

    public InjectedValue<Cache> getCache() {
        return cache;
    }
}
