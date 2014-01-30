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

package org.jboss.legacy.ejb3.registrar;

import org.jboss.ejb3.common.registrar.spi.Ejb3Registrar;
import org.jboss.ejb3.proxy.impl.jndiregistrar.JndiStatefulSessionRegistrar;
import org.jboss.ejb3.proxy.impl.jndiregistrar.JndiStatelessSessionRegistrar;
import org.jboss.msc.service.ServiceName;

/**
 * @author baranowb
 */
public interface EJB3Registrar {

    ServiceName SERVICE_NAME = ServiceName.JBOSS.append(EJB3RegistrarModel.LEGACY).append(EJB3RegistrarModel.SERVICE_NAME);

    Ejb3Registrar getRegistrar();

    JndiStatelessSessionRegistrar getJndiStatelessSessionRegistrar();

    JndiStatefulSessionRegistrar getJndiStatefulSessionRegistrar();

}
