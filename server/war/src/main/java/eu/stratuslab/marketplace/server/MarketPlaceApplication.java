package eu.stratuslab.marketplace.server;

import static eu.stratuslab.marketplace.server.cfg.Parameter.DATA_DIR;
import static eu.stratuslab.marketplace.server.cfg.Parameter.PENDING_DIR;
import static eu.stratuslab.marketplace.server.cfg.Parameter.STORE_TYPE;
import static eu.stratuslab.marketplace.server.cfg.Parameter.ENDORSER_REMINDER;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.freemarker.ContextTemplateLoader;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.routing.TemplateRoute;
import org.restlet.service.StatusService;

import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.resources.AboutResource;
import eu.stratuslab.marketplace.server.resources.EndorserResource;
import eu.stratuslab.marketplace.server.resources.EndorsersResource;
import eu.stratuslab.marketplace.server.resources.HomeResource;
import eu.stratuslab.marketplace.server.resources.MDataResource;
import eu.stratuslab.marketplace.server.resources.MDatumResource;
import eu.stratuslab.marketplace.server.resources.QueryResource;
import eu.stratuslab.marketplace.server.resources.UploadResource;
import eu.stratuslab.marketplace.server.routers.ActionRouter;
import eu.stratuslab.marketplace.server.utils.Reminder;

import eu.stratuslab.marketplace.server.store.RdfStoreFactory;
import eu.stratuslab.marketplace.server.store.RdfStoreFactoryImpl;
import eu.stratuslab.marketplace.server.store.RdfStore;

public class MarketPlaceApplication extends Application {

    private static final Logger LOGGER = Logger.getLogger("org.restlet");

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private ScheduledFuture<?> reminderHandle;
    
    private Reminder reminder;
    
    private RdfStore store = null;
    private String dataDir = null;
    
    private freemarker.template.Configuration freeMarkerConfiguration = null;

    public MarketPlaceApplication() {
        String storeType = Configuration.getParameterValue(STORE_TYPE);
        init(storeType);
    }

    public MarketPlaceApplication(String storeType) {
    	init(storeType);
    }
    
    private void init(String storeType){
    	setName("StratusLab Marketplace");
        setDescription("Marketplace for StratusLab images");
        setOwner("StratusLab");
        setAuthor("Stuart Kenny");
        
        getMetadataService().addExtension("multipart", MediaType.MULTIPART_FORM_DATA, false);
        getMetadataService().addExtension("www_form", MediaType.APPLICATION_WWW_FORM, false);
        getMetadataService().addExtension("application_rdf", MediaType.APPLICATION_RDF_XML, true);
        getMetadataService().addExtension("application_xml", MediaType.APPLICATION_XML, false);
        
        setStatusService(new MarketPlaceStatusService());
                
        getTunnelService().setUserAgentTunnel(true);
               
        dataDir = Configuration.getParameterValue(DATA_DIR);
        createIfNotExists(dataDir);
        createIfNotExists(Configuration.getParameterValue(PENDING_DIR));
                     
        RdfStoreFactory factory = new RdfStoreFactoryImpl();
        store = factory.createRdfStore(RdfStoreFactory.SESAME_PROVIDER, storeType);
        store.initialize();
       
        final Runnable remind = new Runnable() {
        	  public void run() {
        		  remind();
        	  }
          };
          
          this.reminder = new Reminder(this);
          if(Boolean.parseBoolean(Configuration.getParameterValue(ENDORSER_REMINDER))){
        	  reminderHandle = 
        		  scheduler.scheduleAtFixedRate(remind, 30, 30, TimeUnit.DAYS);
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

        // Defines a route for the resource "list of metadata entries"
        router.attach("/metadata", MDataResource.class);
        router.attach("/metadata/", MDataResource.class);
        router.attach("/metadata/{arg1}", MDataResource.class);
        router.attach("/metadata/{arg1}/", MDataResource.class);
        router.attach("/metadata/{arg1}/{arg2}", MDataResource.class);
        router.attach("/metadata/{arg1}/{arg2}/", MDataResource.class);

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

        // Defines a route for queries
        router.attach("/query", QueryResource.class);
        router.attach("/query/", QueryResource.class);

        // Defines a route for the upload form
        router.attach("/upload", UploadResource.class);
        router.attach("/upload/", UploadResource.class);

        // Define a route for the about page
        router.attach("/about", AboutResource.class);
        router.attach("/about/", AboutResource.class);
        
       // Defines a router for actions
        TemplateRoute route;
        route = router.attach("/action/", new ActionRouter());
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);

		String staticContentLocation = System.getProperty(
				"static.content.location", "war://");
        
        Directory cssDir = new Directory(getContext(), staticContentLocation + "/css");
        cssDir.setNegotiatingContent(false);
        cssDir.setIndexName("index.html");
        router.attach("/css/", cssDir);
                           
        Directory jsDir = new Directory(getContext(), staticContentLocation + "/js");
        jsDir.setNegotiatingContent(false);
        jsDir.setIndexName("index.html");
        router.attach("/js/", jsDir);
        
        // Unknown root pages get the home page.
        router.attachDefault(HomeResource.class);
    
        return router;
    }
      
    private void remind(){
    	this.reminder.remind();
    }
    
    @Override
    public void stop() {
       store.shutdown();
    	
       if(reminderHandle != null){
    	   reminderHandle.cancel(true);
       }
    }

    public RdfStore getMetadataStore() {
    	return this.store;
    }

    public String getDataDir() {
        return this.dataDir;
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
    
    private void createIfNotExists(String path){
    	File dir = new File(path);
        if(!dir.exists()){
        	LOGGER.warning("directory does not exist: " + path);
        	if(!dir.mkdirs()){
        		LOGGER.severe("Unable to create directory: " + path);
        	}
        }
    }
    
    class MarketPlaceStatusService extends StatusService {

    	public Representation getRepresentation(Status status, Request request,
                Response response) {

           if(request.getClientInfo().getAcceptedMediaTypes().get(0).
    				getMetadata().equals(MediaType.TEXT_XML) || 
    				request.getClientInfo().getAcceptedMediaTypes().get(0).
    				getMetadata().equals(MediaType.APPLICATION_XML)) {
        	   
        	   Representation r = generateErrorRepresentation(response.getStatus()
                       .getDescription(), Integer.toString(response.getStatus().getCode()));
        	   return r;
    		} else {
    			// Create the data model
    			Map<String, String> dataModel = new TreeMap<String, String>();
    			dataModel.put("baseurl", request.getRootRef().toString());
    			dataModel.put("statusName", response.getStatus().getName());
    			dataModel.put("statusDescription", response.getStatus()
    					.getDescription());
    			dataModel.put("title", response.getStatus().getName());

    			freemarker.template.Configuration freeMarkerConfig = freeMarkerConfiguration;

    			return new TemplateRepresentation("status.ftl", freeMarkerConfig, dataModel,
    					MediaType.TEXT_HTML);
    		}
        }
    	    	    	
    	/**
         * Generate an XML representation of an error response.
         * 
         * @param errorMessage
         *            the error message.
         * @param errorCode
         *            the error code.
         */
        protected Representation generateErrorRepresentation(String errorMessage,
                String errorCode) {
            StringRepresentation result = new StringRepresentation(
            		"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                		"<error>" + errorMessage + "</error>"
                		, MediaType.APPLICATION_XML);
                
            return result;
        }
    	
    }


}
