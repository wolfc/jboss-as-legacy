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

import java.util.List;
import java.util.Vector;
import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.GroupMembershipListener;
import org.jboss.ha.framework.interfaces.HAPartition.HAMembershipExtendedListener;
import org.jboss.ha.framework.interfaces.HAPartition.HAMembershipListener;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class HAMembershipListenerAdapter implements GroupMembershipListener {

    private final HAMembershipListener listener;

    public HAMembershipListenerAdapter(HAMembershipListener listener) {
        this.listener = listener;
    }

    @Override
    public void membershipChanged(List<ClusterNode> deadMembers, List<ClusterNode> newMembers, List<ClusterNode> allMembers) {
        listener.membershipChanged(LegacyClusterNodeAdapter.convertToVector(deadMembers),
                LegacyClusterNodeAdapter.convertToVector(newMembers),
                LegacyClusterNodeAdapter.convertToVector(allMembers));
    }

    @Override
    public void membershipChangedDuringMerge(List<ClusterNode> deadMembers, List<ClusterNode> newMembers, List<ClusterNode> allMembers, List<List<ClusterNode>> originatingGroups) {
        if (listener instanceof HAMembershipExtendedListener) {
            Vector groups = new Vector(originatingGroups.size());
            for (List<ClusterNode> group : originatingGroups) {
                groups.add(LegacyClusterNodeAdapter.convertToVector(group));
            }
            ((HAMembershipExtendedListener) listener).membershipChangedDuringMerge(LegacyClusterNodeAdapter.convertToVector(deadMembers),
                    LegacyClusterNodeAdapter.convertToVector(newMembers),
                    LegacyClusterNodeAdapter.convertToVector(allMembers), groups);
        }
    }

}
