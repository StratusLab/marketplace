package eu.stratuslab.marketplace.server.store.rdf.sesame;

import static eu.stratuslab.marketplace.server.cfg.Parameter.RDBMS_DBNAME;
import static eu.stratuslab.marketplace.server.cfg.Parameter.RDBMS_DBPASS;
import static eu.stratuslab.marketplace.server.cfg.Parameter.RDBMS_DBUSER;
import static eu.stratuslab.marketplace.server.cfg.Parameter.RDBMS_HOST;
import static eu.stratuslab.marketplace.server.cfg.Parameter.RDBMS_PORT;

import java.util.logging.Logger;

import org.openrdf.sail.helpers.SailBase;
import org.openrdf.sail.rdbms.mysql.MySqlStore;

import eu.stratuslab.marketplace.server.cfg.Configuration;

public class SesameMySQLBackend implements SesameBackend {
	
	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	private static final String MYSQL_URL_MESSAGE = "using mysql datastore: mysql://%s:xxxxxx@%s:%d/%s";
	
	public SailBase getSailBase() {
		String mysqlDb = Configuration.getParameterValue(RDBMS_DBNAME);
		String mysqlHost = Configuration.getParameterValue(RDBMS_HOST);
		int mysqlPort = Configuration.getParameterValueAsInt(RDBMS_PORT);
		String mysqlUser = Configuration.getParameterValue(RDBMS_DBUSER);
		String mysqlPass = Configuration.getParameterValue(RDBMS_DBPASS);

		LOGGER.info(String.format(MYSQL_URL_MESSAGE, mysqlUser, mysqlHost,
				mysqlPort, mysqlDb));

		MySqlStore mysqlStore = new MySqlStore();
		mysqlStore.setDatabaseName(mysqlDb);
		mysqlStore.setServerName(mysqlHost);
		mysqlStore.setPortNumber(mysqlPort);
		mysqlStore.setUser(mysqlUser);
		mysqlStore.setPassword(mysqlPass);

		return mysqlStore;
	}

	public boolean keepAlive() {
		return true;
	}
	
}
