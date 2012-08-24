package eu.stratuslab.marketplace.server.store.rdf.sesame;

import static eu.stratuslab.marketplace.server.cfg.Parameter.RDBMS_DBNAME;
import static eu.stratuslab.marketplace.server.cfg.Parameter.RDBMS_DBPASS;
import static eu.stratuslab.marketplace.server.cfg.Parameter.RDBMS_DBUSER;
import static eu.stratuslab.marketplace.server.cfg.Parameter.RDBMS_HOST;
import static eu.stratuslab.marketplace.server.cfg.Parameter.RDBMS_PORT;

import java.util.logging.Logger;

import org.openrdf.sail.helpers.SailBase;
import org.openrdf.sail.rdbms.postgresql.PgSqlStore;

import eu.stratuslab.marketplace.server.cfg.Configuration;

public class SesamePostgresBackend implements SesameBackend {

	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	private static final String PGSQL_URL_MESSAGE = "using postgres datastore: postgresql://%s:xxxxxx@%s:%d/%s";
	
	public SailBase getSailBase() {
		String pgsqlDb = Configuration.getParameterValue(RDBMS_DBNAME);
		String pgsqlHost = Configuration.getParameterValue(RDBMS_HOST);
		int pgsqlPort = Configuration.getParameterValueAsInt(RDBMS_PORT);
		String pgsqlUser = Configuration.getParameterValue(RDBMS_DBUSER);
		String pgsqlPass = Configuration.getParameterValue(RDBMS_DBPASS);

		LOGGER.info(String.format(PGSQL_URL_MESSAGE, pgsqlUser, pgsqlHost,
				pgsqlPort, pgsqlDb));

		PgSqlStore pgsqlStore = new PgSqlStore();
		pgsqlStore.setDatabaseName(pgsqlDb);
		pgsqlStore.setServerName(pgsqlHost);
		pgsqlStore.setPortNumber(pgsqlPort);
		pgsqlStore.setUser(pgsqlUser);
		pgsqlStore.setPassword(pgsqlPass);

		return pgsqlStore;
	}

	public boolean keepAlive() {
		return true;
	}
	
}
