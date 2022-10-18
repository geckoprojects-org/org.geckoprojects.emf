/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.emf.osgi.codegen.templates.model.helper;

import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;

/**
 * A helper for some special OSGi specific dependency deduction
 * 
 * @author Juergen Albert
 * @since 29 Aug 2022
 */
public class Dependencies {
	
	public static boolean isPureOSGi(GenPackage genPackage) {
		return !genPackage.isLiteralsInterface() && genPackage.getGenModel().isOSGiCompatible();
	}

}