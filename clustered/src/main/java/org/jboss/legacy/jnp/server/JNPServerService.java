/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.legacy.jnp.server;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;

/**
 *
 * @author ehsavoie
 */
public interface JNPServerService extends Service<JNPServer> {

    ServiceName SERVICE_NAME = ServiceName.JBOSS.append(JNPServerModel.LEGACY).append(
            JNPServerModel.SERVICE_NAME);
}
