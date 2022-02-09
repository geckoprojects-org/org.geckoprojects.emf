/**
 * Copyright (c) 2012 - 2018 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.emf.osgi.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.gecko.emf.osgi.api.EMFNamespaces;
import org.gecko.emf.osgi.api.annotation.require.RequireEMF;
import org.osgi.service.component.annotations.ComponentPropertyType;

/**
 * A meta annotation for the {@link EMFNamespaces#EMF_RESOURCE_CONFIGURATOR_FEATURE} property of the {@link ResourceResourceConfigurator} 
 * @author Juergen Albert
 * @since 12 Feb 2018
 */
@ComponentPropertyType
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@RequireEMF
public @interface EMFResourceFactoryConfigurator {
	String PREFIX_ = EMFNamespaces.EMF_RESOURCE_CONFIGURATOR_PREFIX;

	String name();
	String[] feature() default "";
	String[] contentType() default "";
	String[] fileExtension() default "";
	
}
