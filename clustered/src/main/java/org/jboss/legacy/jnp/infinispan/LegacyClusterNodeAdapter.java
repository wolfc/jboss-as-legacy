/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.legacy.jnp.infinispan;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jgroups.stack.IpAddress;

/**
 *
 * @author ehsavoie
 */
public class LegacyClusterNodeAdapter implements ClusterNode {

    private org.jboss.as.clustering.ClusterNode node;

    public LegacyClusterNodeAdapter(org.jboss.as.clustering.ClusterNode node) {
        this.node = node;
    }

    @Override
    public String getName() {
        return node.getName();
    }

    @Override
    public InetAddress getIpAddress() {
        return node.getIpAddress();
    }

    @Override
    public int getPort() {
        return node.getPort();
    }

    private String getId(ClusterNode node) {
        IpAddress address = new IpAddress(node.getIpAddress(), node.getPort());
        if (address.getAdditionalData() == null) {
            return address.getIpAddress().getHostAddress() + ":" + address.getPort();
        }
        return new String(address.getAdditionalData());
    }

    @Override
    public int compareTo(Object o) {
        if ((o == null) || !(o instanceof ClusterNode)) {
            throw new ClassCastException("ClusterNode.compareTo(): comparison between different classes");
        }
        return getId(this).compareTo(getId((ClusterNode) o));
    }

    public static Vector convertToVector(List<org.jboss.as.clustering.ClusterNode> nodes) {
        Vector result = new Vector(nodes.size());
        for (org.jboss.as.clustering.ClusterNode node : nodes) {
            result.add(new LegacyClusterNodeAdapter(node));
        }
        return result;
    }

    public static ClusterNode[] convertToArray(List<org.jboss.as.clustering.ClusterNode> nodes) {
        List<ClusterNode> result = new ArrayList<ClusterNode>(nodes.size());
        for (org.jboss.as.clustering.ClusterNode node : nodes) {
            result.add(new LegacyClusterNodeAdapter(node));
        }
        return result.toArray(new ClusterNode[nodes.size()]);
    }
}
