/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *      Data In Motion - initial API and implementation
 */
package org.gecko.emf.osgi.components;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.emf.ecore.resource.Resource.Factory;
import org.eclipse.emf.ecore.resource.Resource.Factory.Registry;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryRegistryImpl;
import org.gecko.emf.osgi.EMFNamespaces;
import org.gecko.emf.osgi.annotation.EMFResourceFactoryConfigurator;
import org.gecko.emf.osgi.helper.ServicePropertiesHelper;
import org.osgi.annotation.bundle.Capability;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.util.converter.Converters;

/**
 * Component for the {@link ResourceFactoryRegistryImpl}
 * @author Mark Hoffmann
 * @since 25.07.2017
 */
@Component(name="DefaultResourceFactoryRegistry")
@Capability(
		namespace = org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE,
		attribute = ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE + ":List<String>=org.eclipse.emf.ecore.resource.Resource.Factory.Registry"
		)
public class DefaultResourceFactoryRegistryComponent {

	private final Registry registry;
	private ServiceRegistration<Registry> serviceRegistration;
	private final Map<Long, Set<String>> resourceFactoryNameMap = new ConcurrentHashMap<>();
	private long serviceChangeCount = 0;
	
	/**
	 * Creates a new instance.
	 */
	@Activate
	public DefaultResourceFactoryRegistryComponent(BundleContext ctx) {
		registry = new ResourceFactoryRegistryImpl();
		serviceRegistration = ctx.registerService(Registry.class, registry, getDictionary());
	}
	
	@Deactivate
	public void deactivate() {
		serviceRegistration.unregister();
		serviceRegistration = null;
	}
	
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, target = "(|" + "( " + EMFNamespaces.EMF_RESOURCE_CONFIGURATOR_CONTENT_TYPE + "=* ) " + "( " + EMFNamespaces.EMF_RESOURCE_CONFIGURATOR_FILE_EXT + "=* ) " + "( " + EMFNamespaces.EMF_RESOURCE_CONFIGURATOR_PROTOCOL + "=* ) " + ")")
//	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, target = "(|" + "( " + EMFNamespaces.EMF_RESOURCE_CONFIGURATOR_CONTENT_TYPE + "=* ) " + "( " + EMFNamespaces.EMF_RESOURCE_CONFIGURATOR_FILE_EXT + "=* ) " + "( " + EMFNamespaces.EMF_RESOURCE_CONFIGURATOR_PROTOCOL + "=* ) " + ")" )
//	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	public void addFactory(Factory factory, Map<String, Object> props) {
		System.err.println("Adding Factory for " + props);
		EMFResourceFactoryConfigurator configuration = Converters.standardConverter().convert(props).to(EMFResourceFactoryConfigurator.class);
		Arrays.asList(configuration.contentType()).forEach(s -> registry.getContentTypeToFactoryMap().put(s, factory)); 
		Arrays.asList(configuration.fileExtension()).forEach(s -> registry.getExtensionToFactoryMap().put(s, factory)); 
		Arrays.asList(configuration.protocol()).forEach(s -> registry.getProtocolToFactoryMap().put(s, factory)); 
		updateProperties(props, true);
	}
	
	public void removeFactory(Factory factory, Map<String, Object> props) {
		System.err.println("Removing Factory for " + props);
		EMFResourceFactoryConfigurator configuration = Converters.standardConverter().convert(props).to(EMFResourceFactoryConfigurator.class);
		Arrays.asList(configuration.contentType()).forEach(registry.getContentTypeToFactoryMap()::remove); 
		Arrays.asList(configuration.fileExtension()).forEach(registry.getExtensionToFactoryMap()::remove); 
		Arrays.asList(configuration.protocol()).forEach(registry.getProtocolToFactoryMap()::remove);
		updateProperties(props, false);
	}
	
	protected void updateProperties(Map<String, Object> serviceProperties, boolean add) {
		Object name = serviceProperties.get(EMFNamespaces.EMF_RESOURCE_CONFIGURATOR_NAME);
		Long serviceId = (Long) serviceProperties.get(Constants.SERVICE_ID);
		if (name != null && (name instanceof String || name instanceof String[])) {
			Set<String> nameSet;
			if (!add) {
				nameSet = Collections.emptySet();
			} else {
				if (name instanceof String) {
					nameSet = Collections.singleton(name.toString());
				} else {
					nameSet = new HashSet<String>(Arrays.asList((String[])name));
				}
			}
			ServicePropertiesHelper.updateNameMap(resourceFactoryNameMap, nameSet, serviceId);
			updateRegistrationProperties();
		}
	}
	
	/**
	 * Updates the service registration properties
	 */
	protected void updateRegistrationProperties() {
		if (serviceRegistration != null) {
			serviceRegistration.setProperties(getDictionary());
		}
	}
	
	/**
	 * Creates a dictionary for the stored properties
	 * @return a dictionary for the stored properties
	 */
	protected Dictionary<String, Object> getDictionary() {
		Dictionary<String, Object> properties = new Hashtable<>();
		String[] resourceFactoryNames = ServicePropertiesHelper.getNamesArray(resourceFactoryNameMap);
		properties.put(ComponentConstants.COMPONENT_NAME, "DefaultResourceFactoryRegistry");
		properties.put(EMFNamespaces.EMF_RESOURCE_CONFIGURATOR_NAME, resourceFactoryNames);
		properties.put(Constants.SERVICE_CHANGECOUNT, serviceChangeCount++);
		return properties;
	}
}
