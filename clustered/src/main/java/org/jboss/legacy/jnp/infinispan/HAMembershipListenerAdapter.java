/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 * @author ehsavoie
 */
public class HAMembershipListenerAdapter implements GroupMembershipListener {

    private HAMembershipListener listener;

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
