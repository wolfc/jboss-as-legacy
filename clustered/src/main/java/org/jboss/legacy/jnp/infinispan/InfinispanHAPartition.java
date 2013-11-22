/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.legacy.jnp.infinispan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import org.jboss.as.clustering.impl.CoreGroupCommunicationService;
import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.ha.framework.interfaces.DistributedReplicantManager;
import org.jboss.ha.framework.interfaces.DistributedState;
import org.jboss.ha.framework.interfaces.HAPartition;
import org.jboss.ha.framework.interfaces.ResponseFilter;
import org.jboss.ha.framework.server.DistributedReplicantManagerImpl;

/**
 *
 * @author ehsavoie
 */
public class InfinispanHAPartition implements HAPartition {

    private final CoreGroupCommunicationService service;
    private final Map<HAMembershipListener, HAMembershipListenerAdapter> groupMembershipListeners = new HashMap<HAMembershipListener, HAMembershipListenerAdapter>();
    private final DistributedReplicantManagerImpl distributedReplicantManager;

    public InfinispanHAPartition(CoreGroupCommunicationService service) {
        this.service = service;
        this.distributedReplicantManager = new DistributedReplicantManagerImpl(this);
    }

    @Override
    public String getNodeName() {
        return service.getNodeName();
    }

    @Override
    public String getPartitionName() {
        return service.getGroupName();
    }

    @Override
    public DistributedReplicantManager getDistributedReplicantManager() {
        return this.distributedReplicantManager;
    }

    @Override
    public DistributedState getDistributedStateService() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void registerRPCHandler(String serviceName, Object handler) {
        service.registerRPCHandler(serviceName, handler);
    }

    @Override
    public void registerRPCHandler(String serviceName, Object handler, ClassLoader classloader) {
        service.registerRPCHandler(serviceName, handler);
    }

    @Override
    public void unregisterRPCHandler(String serviceName, Object subscriber) {
        service.unregisterRPCHandler(serviceName, subscriber);
    }

    @Override
    public ArrayList callMethodOnCluster(String serviceName, String methodName, Object[] args, Class[] types, boolean excludeSelf) throws Exception {
        return new ArrayList(service.callMethodOnCluster(serviceName, methodName, args, types, excludeSelf));
    }

    @Override
    public ArrayList callMethodOnCluster(String serviceName, String methodName, Object[] args, Class[] types, boolean excludeSelf, final ResponseFilter filter) throws Exception {
        return new ArrayList(service.callMethodOnCluster(serviceName, methodName, args, types, excludeSelf, new ResponseFilterAdapter(filter)));
    }

    @Override
    public void callAsynchMethodOnCluster(String serviceName, String methodName, Object[] args, Class[] types, boolean excludeSelf) throws Exception {
        service.callAsynchMethodOnCluster(serviceName, methodName, args, types, excludeSelf);
    }

    @Override
    public ArrayList callMethodOnCoordinatorNode(String serviceName, String methodName, Object[] args, Class[] types, boolean excludeSelf) throws Exception {
        return service.callMethodOnCoordinatorNode(serviceName, methodName, args, types, excludeSelf);
    }

    @Override
    public Object callMethodOnNode(String serviceName, String methodName, Object[] args, Class[] types, long methodTimeout, ClusterNode targetNode) throws Throwable {
        return service.callMethodOnNode(serviceName, methodName, args, types, new ClusterNodeAdapter(targetNode));
    }

    @Override
    public void callAsyncMethodOnNode(String serviceName, String methodName, Object[] args, Class[] types, long methodTimeout, ClusterNode targetNode) throws Throwable {
        service.callAsyncMethodOnNode(serviceName, methodName, args, types, new ClusterNodeAdapter(targetNode));
    }

    @Override
    public void subscribeToStateTransferEvents(String serviceName, HAPartitionStateTransfer subscriber) {
        service.registerStateTransferProvider(serviceName, new StateTransferProviderAdapter(subscriber));
    }

    @Override
    public void unsubscribeFromStateTransferEvents(String serviceName, HAPartitionStateTransfer subscriber) {
        service.unregisterStateTransferProvider(serviceName);
    }

    @Override
    public void registerMembershipListener(HAMembershipListener listener) {
        groupMembershipListeners.put(listener, new HAMembershipListenerAdapter(listener));
        service.registerGroupMembershipListener(groupMembershipListeners.get(listener));
    }

    @Override
    public void unregisterMembershipListener(HAMembershipListener listener) {
        service.unregisterGroupMembershipListener(groupMembershipListeners.get(listener));
    }

    @Override
    public boolean getAllowSynchronousMembershipNotifications() {
        return service.getAllowSynchronousMembershipNotifications();
    }

    @Override
    public void setAllowSynchronousMembershipNotifications(boolean allowSync) {
        service.setAllowSynchronousMembershipNotifications(allowSync);
    }

    @Override
    public long getCurrentViewId() {
        return service.getCurrentViewId();
    }

    @Override
    public Vector getCurrentView() {
        return new Vector(service.getCurrentView());
    }

    @Override
    public ClusterNode[] getClusterNodes() {
        return LegacyClusterNodeAdapter.convertToArray(service.getClusterNodes());
    }

    @Override
    public ClusterNode getClusterNode() {
        return new LegacyClusterNodeAdapter(service.getClusterNode());
    }

    public void start() throws Exception {
        this.distributedReplicantManager.createService();
        this.distributedReplicantManager.startService();
    }

}
