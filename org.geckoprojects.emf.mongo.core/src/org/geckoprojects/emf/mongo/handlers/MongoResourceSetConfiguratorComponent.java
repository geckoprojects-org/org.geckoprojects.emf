package org.geckoprojects.emf.mongo.handlers;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.geckoprojects.emf.core.api.EMFNamespaces;
import org.geckoprojects.emf.core.api.ResourceSetConfigurator;
import org.geckoprojects.emf.mongo.InputStreamFactory;
import org.geckoprojects.emf.mongo.OutputStreamFactory;
import org.geckoprojects.mongo.core.GeckoMongoDatabase;
import org.geckoprojects.mongo.core.MongoConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.mongodb.client.MongoDatabase;

/**
 * This implementation of the ResourceSetConfigurator service will attach all
 * currently bound URI handlers to the ResourceSet. This service is intended to
 * be used with the IResourceSetFactory service.
 * 
 * @author bhunt
 * 
 */
@Component(name = "MongoResourceSetConfiguratorComponent", immediate = true)
public class MongoResourceSetConfiguratorComponent {

	private final Map<MongoDatabase, Map<String,Object>> mongoDatabases = new ConcurrentHashMap<MongoDatabase, Map<String,Object>>();
	private ServiceRegistration<ResourceSetConfigurator> configuratorRegistration;
	private MongoURIHandlerProvider uriHandlerProvider = new MongoURIHandlerProvider();
	private BundleContext ctx;
	private List<String> uids = new LinkedList<String>();
	private Map<String, String> uidIdentifierMap = new HashMap<String, String>();

	/**
	 * Called on component activation
	 * 
	 * @param context the component context
	 */
	@Activate
	public void activate(BundleContext context) {
		ctx = context;
		Dictionary<String, Object> properties = getDictionary();
		configuratorRegistration = ctx.registerService(ResourceSetConfigurator.class,
				new MongoResourceSetConfigurator(uriHandlerProvider), properties);

	}

	/**
	 * Called on component deactivation
	 */
	@Deactivate
	public void deactivate() {
		configuratorRegistration.unregister();
		configuratorRegistration = null;
	}

	/**
	 * Adds a {@link GeckoMongoDatabase} to the provider map.
	 * 
	 * @param mongoDatabase the provider to be added
	 */
	@Reference(name = "MongoDatabase", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.AT_LEAST_ONE)
	public void addMongoDatabase(GeckoMongoDatabase mongoDatabase, Map<String,Object> map) {

		mongoDatabases.put(mongoDatabase, map);
		
		String alias=mongoDatabase.databaseAlias();
		if(alias==null) {
			throw new IllegalArgumentException("Database alias must not be null");
		}
		String internalName=mongoDatabase.internalName();
			
		uriHandlerProvider.addMongoDatabaseProvider(mongoDatabase);
		updateProperties(alias,internalName, true);
	}



	/**
	 * Removes a {@link GeckoMongoDatabase} from the map
	 * 
	 * @param mongoDatabase the provider to be removed
	 */
	public void removeMongoDatabase(GeckoMongoDatabase mongoDatabase, Map<String,Object>map) {

		String alias=mongoDatabase.databaseAlias();
		String internalName=mongoDatabase.internalName();
		uriHandlerProvider.removeMongoDatabaseProvider(mongoDatabase);
		updateProperties(alias,internalName, false);
	}

	/**
	 * Sets an {@link InputStreamFactory} to handle input streams
	 * 
	 * @param inputStreamFactory the factory to set
	 */
	@Reference(name = "InputStreamFactory", cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
	public void setInputStreamFactory(InputStreamFactory inputStreamFactory) {
		uriHandlerProvider.setInputStreamFactory(inputStreamFactory);
	}

	/**
	 * Sets an {@link OutputStreamFactory} to handle output streams
	 * 
	 * @param outputStreamFactory the factory to set
	 */
	@Reference(name = "OutputStreamFactory", cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
	public void setOutputStreamFactory(OutputStreamFactory outputStreamFactory) {
		uriHandlerProvider.setOutputStreamFactory(outputStreamFactory);
	}

	/**
	 * Updates the properties of the service, depending on changes on injected
	 * services
	 * 
	 * @param config the service properties from the injected service
	 * @param add    <code>true</code>, if the service was add, <code>false</code>
	 *               in case of an remove
	 */
	private void updateProperties(String uid,String internal_name, boolean add) {


		if (add) {
			uids.add(uid);
			if (internal_name != null) {
				uidIdentifierMap.put(uid, internal_name);
			}
		} else {
			uids.remove(uid);
			uidIdentifierMap.remove(uid);
		}

		updateRegistrationProperties();

	}

	/**
	 * Updates the service registration properties
	 */
	private void updateRegistrationProperties() {
		if (configuratorRegistration != null) {
			configuratorRegistration.setProperties(getDictionary());
		}
	}

	/**
	 * Creates a dictionary for the stored properties
	 * 
	 * @return a dictionary for the stored properties
	 */
	private Dictionary<String, Object> getDictionary() {
		Dictionary<String, Object> properties = new Hashtable<>();
		List<String> uidsList = new LinkedList<String>(uids);
		String[] uidsArr = uidsList.toArray(new String[uids.size()]);
		if (uidsArr.length > 0) {
			properties.put(MongoConstants.DB_PROP_DATABASE_DATABASE_ALIAS, uidsArr);
		}
		String[] ids = uidsList.stream().map(this::replaceWithIdentifier).collect(Collectors.toList())
				.toArray(new String[0]);
		String[] configNames = Arrays.copyOf(ids, ids.length + 1);
		configNames[ids.length] = "mongo";
		properties.put(EMFNamespaces.EMF_CONFIGURATOR_NAME, configNames);
		return properties;
	}

	/**
	 * Replaces the given uid with an identifier
	 * {@link MongoDatabaseProvider#PROP_DATABASE_IDENTIFIER} value if it exists
	 * 
	 * @param alias the alias
	 * @return the identifier, if it exists, otherwise the alias
	 */
	private String replaceWithIdentifier(String uid) {
		String ident = uidIdentifierMap.get(uid);
		if (ident != null && !ident.isEmpty()) {
			return ident;
		} else {
			return uid;
		}
	}

}
