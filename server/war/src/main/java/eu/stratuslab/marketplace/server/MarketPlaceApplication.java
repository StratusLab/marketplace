/**
 * Created as part of the StratusLab project (http://stratuslab.eu),
 * co-funded by the European Commission under the Grant Agreement
 * INSFO-RI-261552.
 *
 * Copyright (c) 2011
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.stratuslab.marketplace.server;

import static eu.stratuslab.marketplace.server.cfg.Parameter.DATA_DIR;
import static eu.stratuslab.marketplace.server.cfg.Parameter.ENDORSER_REMINDER;
import static eu.stratuslab.marketplace.server.cfg.Parameter.PENDING_DIR;
import static eu.stratuslab.marketplace.server.cfg.Parameter.STORE_TYPE;
import static eu.stratuslab.marketplace.server.cfg.Parameter.FILESTORE_TYPE;
import static eu.stratuslab.marketplace.server.cfg.Parameter.REPLICATION_ENABLED;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.json.simple.JSONValue;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.freemarker.ContextTemplateLoader;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Directory;
import org.restlet.routing.Filter;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.routing.TemplateRoute;
import org.restlet.service.StatusService;

import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.cfg.Parameter;
import eu.stratuslab.marketplace.server.query.QueryBuilder;
import eu.stratuslab.marketplace.server.query.SolrBuilder;
import eu.stratuslab.marketplace.server.query.SparqlBuilder;
import eu.stratuslab.marketplace.server.resources.EndorserResource;
import eu.stratuslab.marketplace.server.resources.EndorsersResource;
import eu.stratuslab.marketplace.server.resources.ErrorResource;
import eu.stratuslab.marketplace.server.resources.HomeResource;
import eu.stratuslab.marketplace.server.resources.MDataResource;
import eu.stratuslab.marketplace.server.resources.MDatumResource;
import eu.stratuslab.marketplace.server.resources.QueryResource;
import eu.stratuslab.marketplace.server.resources.TagsResource;
import eu.stratuslab.marketplace.server.resources.UploadResource;
import eu.stratuslab.marketplace.server.resources.SyncResource;
import eu.stratuslab.marketplace.server.routers.ActionRouter;
import eu.stratuslab.marketplace.server.store.file.CouchbaseStore;
import eu.stratuslab.marketplace.server.store.file.FileStore;
import eu.stratuslab.marketplace.server.store.file.FlatFileStore;
import eu.stratuslab.marketplace.server.store.file.GitStore;
import eu.stratuslab.marketplace.server.store.rdf.RdfStore;
import eu.stratuslab.marketplace.server.store.rdf.RdfStoreFactory;
import eu.stratuslab.marketplace.server.store.rdf.RdfStoreFactoryImpl;
import eu.stratuslab.marketplace.server.store.rdf.Processor;
import eu.stratuslab.marketplace.server.store.rdf.RdfStoreUpdater;
import eu.stratuslab.marketplace.server.utils.EndorserWhitelist;
import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;
import eu.stratuslab.marketplace.server.utils.Reminder;

public class MarketPlaceApplication extends Application {

    private static final Logger LOGGER = Logger.getLogger("org.restlet");
    
    private final ScheduledExecutorService scheduler = Executors
            .newScheduledThreadPool(2);
    private final ScheduledExecutorService indexUpdater = Executors.newScheduledThreadPool(1);
    
    private ScheduledFuture<?> reminderHandle;
    private ScheduledFuture<?> indexUpdateHandle;

    private static final int REMINDER_INTERVAL = 30;
    private static final int EXPIRY_INTERVAL = 1;
    
    private Reminder reminder;
    private Reminder expiry;
    
    private RdfStore store = null;
    private FileStore fileStore = null;
    private QueryBuilder queryBuilder = null;
    
    private String dataDir = null;

    private freemarker.template.Configuration freeMarkerConfiguration = null;

    private EndorserWhitelist whitelist;
    
    private RdfStoreUpdater rdfUpdater;
   
	private boolean invalidConfig = false;

	public MarketPlaceApplication() {
		try {		
			String storeType = Configuration.getParameterValue(STORE_TYPE);
			String fileStoreType = Configuration.getParameterValue(FILESTORE_TYPE);
			String dataDir = Configuration.getParameterValue(DATA_DIR);
			init(dataDir, storeType, fileStoreType);
		} catch(ExceptionInInitializerError e){
			LOGGER.severe("incorrect configuration: " + e.getCause().getMessage());
			invalidConfig = true;
		}
	}

    public MarketPlaceApplication(String dataDir, String storeType, String fileStoreType) {
        init(dataDir, storeType, fileStoreType);
    }

    private void init(String dataDir, String storeType, String fileStoreType) {
        setName("StratusLab Marketplace");
        setDescription("Marketplace for StratusLab images");
        setOwner("StratusLab");
        setAuthor("Stuart Kenny");

        getMetadataService().addExtension("multipart",
                MediaType.MULTIPART_FORM_DATA, false);
        getMetadataService().addExtension("www_form",
                MediaType.APPLICATION_WWW_FORM, false);
        getMetadataService().addExtension("application_rdf",
                MediaType.APPLICATION_RDF_XML, true);
        getMetadataService().addExtension("application_xml",
                MediaType.APPLICATION_XML, false);
        
        getMetadataService().setDefaultMediaType(MediaType.APPLICATION_XML);

        setStatusService(new MarketPlaceStatusService());

        getTunnelService().setUserAgentTunnel(true);
        
        this.dataDir = dataDir;
        
        boolean success = MetadataFileUtils.createIfNotExists(dataDir);
        if(!success){
        	LOGGER.severe("Unable to create directory: " + dataDir);
        }
        
        success = MetadataFileUtils.createIfNotExists(
        		Configuration.getParameterValue(PENDING_DIR));
        if(!success){
        	LOGGER.severe("Unable to create directory: " 
        			+ Configuration.getParameterValue(PENDING_DIR));
        }
        
        whitelist = new EndorserWhitelist();

        RdfStoreFactory factory = new RdfStoreFactoryImpl();
        if (storeType.equals("native") || storeType.equals("memory")) {
        	store = factory.createRdfStore(RdfStoreFactory.SESAME_PROVIDER,
        			storeType);
        	queryBuilder = new SparqlBuilder();
        } else if ( storeType.equals("solr")){
        	store = factory.createRdfStore(RdfStoreFactory.SOLR_PROVIDER,
    				storeType);
        	queryBuilder = new SolrBuilder();
        }
        store.initialize();
        
        FileStore internalStore = null;
        
        if(fileStoreType.equals("file")){
        	internalStore = new FlatFileStore(dataDir);
        } else if(fileStoreType.equals("couchbase")){
        	internalStore = new CouchbaseStore(dataDir);
        }
        
        if(Configuration.getParameterValueAsBoolean(REPLICATION_ENABLED)){
        	 fileStore = new GitStore(dataDir, internalStore);


        	final Runnable update = new Runnable() {
        		public void run() {
        			rdfIndexUpdate();
        		}
        	};

        	rdfUpdater = new RdfStoreUpdater(fileStore, new Processor(this));
        	indexUpdateHandle = indexUpdater.scheduleWithFixedDelay(update, 
        			1, 5, TimeUnit.MINUTES);
        } else {
        	fileStore = internalStore;
        }
        
        final Runnable remind = new Runnable() {
            public void run() {
                remind();
            }
        };

        final Runnable expires = new Runnable() {
            public void run() {
                expiry();
            }
        };
        
        reminder = new Reminder(this);
        expiry = new Reminder(this);
        
        if (Configuration.getParameterValueAsBoolean(ENDORSER_REMINDER)) {
        	reminderHandle = scheduler.scheduleWithFixedDelay(remind, REMINDER_INTERVAL, 
        			REMINDER_INTERVAL, TimeUnit.DAYS);
            reminderHandle = scheduler.scheduleWithFixedDelay(expires, EXPIRY_INTERVAL, 
            		EXPIRY_INTERVAL, TimeUnit.DAYS);
        }
        
    }

    /**
     * Creates a root Restlet that will receive all incoming calls.
     */
    @Override
    public Restlet createInboundRoot() {
        Context context = getContext();

        // Create the FreeMarker configuration.
        freeMarkerConfiguration = MarketPlaceApplication
                .createFreeMarkerConfig(context);

        // Create a router Restlet that defines routes.
        Router router = new Router(context);
        
        if(invalidConfig){
        	router.attach("/", ErrorResource.class);
        } else {
        	TemplateRoute route = router.attach("/metadata/{email}?tag={tag}", MDatumResource.class);       
        	route.setMatchingQuery(true);

        	// Defines a route for the resource "list of metadata entries"
        	router.attach("/metadata", MDataResource.class);
        	router.attach("/metadata/", MDataResource.class);
        	router.attach("/metadata/{arg1}", MDataResource.class);
        	router.attach("/metadata/{arg1}/", MDataResource.class);
        	router.attach("/metadata/{arg1}/{arg2}", MDataResource.class);
        	router.attach("/metadata/{arg1}/{arg2}/", MDataResource.class);
        	router.attach("/metadata?query={query}", MDataResource.class);

        	// Defines a route for the resource "metadatum"
        	router.attach("/metadata/{identifier}/{email}/{date}",
        			MDatumResource.class);
        	router.attach("/metadata/{identifier}/{email}/{date}/",
        			MDatumResource.class);

        	// Defines a route for the resource "endorsers"
        	router.attach("/endorsers", EndorsersResource.class);
        	router.attach("/endorsers/", EndorsersResource.class);

        	// Defines a route for the resource "endorser"
        	router.attach("/endorsers/{email}", EndorserResource.class);
        	router.attach("/endorsers/{email}/", EndorserResource.class);

        	router.attach("/endorsers/{email}/tags", TagsResource.class);
        	router.attach("/endorsers/{email}/tags/", TagsResource.class);

        	// Defines a route for queries
        	router.attach("/query", QueryResource.class);
        	router.attach("/query/", QueryResource.class);

        	// Defines a route for the upload form
        	router.attach("/upload", UploadResource.class);
        	router.attach("/upload/", UploadResource.class);

        	// Defines a route for the sync resource
        	router.attach("/sync", SyncResource.class);
        	router.attach("/sync/", SyncResource.class);

        	// Defines a router for actions
        	route = router.attach("/action/", new ActionRouter());
        	route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);

        	attachDirectory(router, getContext(), "/css/", 
        			Configuration.getParameterValue(Parameter.STYLE_PATH)
        			);

        	attachDirectory(router, getContext(), "/js/", 
        			Configuration.getParameterValue(Parameter.JS_PATH)
        			);

                attachDirectory(router, getContext(), "/img/",
                                Configuration.getParameterValue(Parameter.IMG_PATH)
                                );

        	// Unknown root pages get the home page.
        	router.attachDefault(HomeResource.class);
        }
        
        Filter filter = new DoubleSlashFilter();
        filter.setNext(router);
        
        return filter;
    }

    private static void attachDirectory(Router router, Context context,
            String attachmentPoint, String path) {

        Reference styleRef = LocalReference.createClapReference(
                LocalReference.CLAP_THREAD, path);

        Directory dir = new Directory(context, styleRef);
        dir.setNegotiatingContent(false);
        dir.setIndexName("index.html");
        router.attach(attachmentPoint, dir);
    }

    private void remind() {
        reminder.remind();
    }

    private void expiry() {
        expiry.expiry();
    }
    
    private void rdfIndexUpdate() {
    	rdfUpdater.update();
    }
    
    @Override
    public void stop() {
    	if(store != null){
    		store.shutdown();
    	}

    	if(fileStore != null){
    		fileStore.shutdown();
    	}
    	
        if (reminderHandle != null) {
            reminderHandle.cancel(true);
        }
        
        if(indexUpdateHandle != null){
        	indexUpdateHandle.cancel(true);
        }
    }

    public RdfStore getMetadataRdfStore() {
        return store;
    }

    public FileStore getMetadataFileStore(){
    	return fileStore;
    }
    
    public QueryBuilder getQueryBuilder() {
		return queryBuilder;
	}
    
    public String getDataDir() {
        return dataDir;
    }

    public EndorserWhitelist getWhitelist() {
        return whitelist;
    }

    public freemarker.template.Configuration getFreeMarkerConfiguration() {
        return freeMarkerConfiguration;
    }

    private static freemarker.template.Configuration createFreeMarkerConfig(
            Context context) {

        freemarker.template.Configuration cfg = new freemarker.template.Configuration();
        cfg.setLocalizedLookup(false);

        LocalReference fmBaseRef = LocalReference
                .createClapReference("/freemarker/");

        cfg.setTemplateLoader(new ContextTemplateLoader(context, fmBaseRef));

        return cfg;
    }

    static class DoubleSlashFilter extends Filter {
    	@Override
    	protected int beforeHandle(Request request, Response response) {
    		Reference ref = request.getResourceRef();
    		String originalPath = ref.getPath();
    		if (originalPath.contains("//"))
    		{
    			String newPath = originalPath.replaceAll("//", "/");
    			ref.setPath(newPath);
    		}
    		return Filter.CONTINUE;
    	}
    }
    
    class MarketPlaceStatusService extends StatusService {

        public Representation getRepresentation(Status status, Request request,
                Response response) {

        	if (request.getClientInfo().getAcceptedMediaTypes().get(0)
                    .getMetadata().equals(MediaType.TEXT_XML)
                    || request.getClientInfo().getAcceptedMediaTypes().get(0)
                            .getMetadata().equals(MediaType.APPLICATION_XML)
                            || request.getClientInfo().getAcceptedMediaTypes().get(0)
                            .getMetadata().equals(MediaType.APPLICATION_RDF_XML)) {

                Representation r = generateXmlErrorRepresentation(response
                        .getStatus().getDescription(),
                        Integer.toString(response.getStatus().getCode()));
                return r;
            } else if (request.getClientInfo().getAcceptedMediaTypes().get(0)
                    .getMetadata().equals(MediaType.APPLICATION_JSON)) {
                Representation r = generateJsonErrorRepresentation(response
                        .getStatus().getDescription(),
                        Integer.toString(response.getStatus().getCode()));
                return r;
            } else {
                // Create the data model
                Map<String, String> dataModel = new TreeMap<String, String>();
                dataModel.put("baseurl", request.getRootRef().toString());
                dataModel.put("statusName", response.getStatus().getReasonPhrase());
                dataModel.put("statusDescription", response.getStatus()
                        .getDescription());
                dataModel.put("title", response.getStatus().getReasonPhrase());

                freemarker.template.Configuration freeMarkerConfig = freeMarkerConfiguration;

                return new TemplateRepresentation("status.ftl",
                        freeMarkerConfig, dataModel, MediaType.TEXT_HTML);
            }
        }

        protected Representation generateXmlErrorRepresentation(
                String errorMessage, String errorCode) {
            StringRepresentation result = new StringRepresentation(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
                            + "<error>" + errorMessage + "</error>",
                    MediaType.APPLICATION_XML);

            return result;
        }

        protected Representation generateJsonErrorRepresentation(
                String errorMessage, String errorCode) {
            StringRepresentation result = new StringRepresentation("{\""
                    + errorCode + "\" : "
                    + JSONValue.toJSONString(errorMessage) + "}",
                    MediaType.APPLICATION_JSON);

            return result;
        }

    }
	
}
