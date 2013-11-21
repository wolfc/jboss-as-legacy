/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.legacy.jnp.infinispan;

import java.io.Serializable;
import org.jboss.as.clustering.StateTransferProvider;
import org.jboss.ha.framework.interfaces.HAPartition;

/**
 *
 * @author ehsavoie
 */
public class StateTransferProviderAdapter implements StateTransferProvider {

    private HAPartition.HAPartitionStateTransfer transfert;

    public StateTransferProviderAdapter(HAPartition.HAPartitionStateTransfer transfert) {
        this.transfert = transfert;
    }

    @Override
    public Serializable getCurrentState() {
        return transfert.getCurrentState();
    }


}
