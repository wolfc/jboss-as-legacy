/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.legacy.jnp.infinispan;

import java.net.InetAddress;
import org.jboss.as.clustering.ClusterNode;
import org.jgroups.stack.IpAddress;

/**
 *
 * @author ehsavoie
 */
public class ClusterNodeAdapter implements ClusterNode {

    private org.jboss.ha.framework.interfaces.ClusterNode node;

    public ClusterNodeAdapter(org.jboss.ha.framework.interfaces.ClusterNode node) {
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

    @Override
    public int compareTo(ClusterNode o) {
        return getId(this).compareTo(getId(o));
    }

    private String getId(ClusterNode node) {
        IpAddress address = new IpAddress(node.getIpAddress(), node.getPort());
        if (address.getAdditionalData() == null) {
            return address.getIpAddress().getHostAddress() + ":" + address.getPort();
        }
        return new String(address.getAdditionalData());
    }
}
