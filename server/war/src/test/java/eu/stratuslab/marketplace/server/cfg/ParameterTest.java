package eu.stratuslab.marketplace.server.cfg;

import static eu.stratuslab.marketplace.server.cfg.Parameter.ADMIN_EMAIL;
import static eu.stratuslab.marketplace.server.cfg.Parameter.DATA_DIR;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MAIL_PORT;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MYSQL_PORT;
import static eu.stratuslab.marketplace.server.cfg.Parameter.STORE_TYPE;
import static eu.stratuslab.marketplace.server.cfg.Parameter.SUPPORT_EMAIL;
//import static eu.stratuslab.marketplace.server.cfg.Parameter.TIME_RANGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ParameterTest {

    @Test
    public void checkDefaultValues() {
        for (Parameter p : Parameter.values()) {
            String defaultValue = p.getDefaultValue();
            if (defaultValue != null) {
                p.validate(defaultValue);
            }
        }
    }

    @Test
    public void checkKeyAndNameConsistency() {
        for (Parameter p : Parameter.values()) {

            String name = p.name();
            String key = p.getKey();

            Parameter p2 = Parameter.parameterFromKey(key);
            assertEquals(p, p2);

            String key2 = Parameter.nameToKey(name);
            assertEquals(key, key2);
        }
    }

    @Test
    public void checkValidStoreTypeValues() {
        STORE_TYPE.validate("memory");
        STORE_TYPE.validate("mysql");
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkInvalidStoreTypeValues() {
        STORE_TYPE.validate("invalid");
    }

    @Test
    public void checkValidDataDirValues() {
        DATA_DIR.validate("/tmp");
        DATA_DIR.validate("/var/lib/stratuslab");
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkInvalidDataDirValues() {
        DATA_DIR.validate("some/relative/path");
    }

    /*@Test
    public void checkValidTimeRangeValues() {
        String[] values = { "10", "07" };
        for (String value : values) {
            TIME_RANGE.validate(value);
        }
    }

    @Test
    public void checkInvalidTimeRangeValues() {
        String[] values = { "0", "-1", "0xG" };
        for (String value : values) {
            try {
                TIME_RANGE.validate(value);
                fail("no exception thrown for value: " + value);
            } catch (IllegalArgumentException consumed) {
                // OK
            }
        }
    }*/

    @Test
    public void checkValidMysqlPortValues() {
        String[] values = { "1", "10", "07", "65535" };
        for (String value : values) {
            MYSQL_PORT.validate(value);
        }
    }

    @Test
    public void checkInvalidMysqlPortValues() {
        String[] values = { "0", "-1", "0xG", "65536" };
        for (String value : values) {
            try {
                MYSQL_PORT.validate(value);
                fail("no exception thrown for value: " + value);
            } catch (IllegalArgumentException consumed) {
                // OK
            }
        }
    }

    @Test
    public void checkValidMailPortValues() {
        String[] values = { "1", "10", "07", "65535" };
        for (String value : values) {
            MAIL_PORT.validate(value);
        }
    }

    @Test
    public void checkInvalidMailPortValues() {
        String[] values = { "0", "-1", "0xG", "65536" };
        for (String value : values) {
            try {
                MAIL_PORT.validate(value);
                fail("no exception thrown for value: " + value);
            } catch (IllegalArgumentException consumed) {
                // OK
            }
        }
    }

    @Test
    public void checkValidAdminEmailValues() {
        String[] values = { "admin@example.org", "admin.list@example.com" };
        for (String value : values) {
            ADMIN_EMAIL.validate(value);
        }
    }

    @Test
    public void checkInvalidAdminEmailValues() {
        String[] values = { "admin", "admin@list@example.com" };
        for (String value : values) {
            try {
                ADMIN_EMAIL.validate(value);
                fail("no exception thrown for value: " + value);
            } catch (IllegalArgumentException consumed) {
                // OK
            }
        }
    }

    @Test
    public void checkValidSupportEmailValues() {
        String[] values = { "support@example.org", "support.list@example.com" };
        for (String value : values) {
            SUPPORT_EMAIL.validate(value);
        }
    }

    @Test
    public void checkInvalidSupportEmailValues() {
        String[] values = { "support", "support@list@example.com" };
        for (String value : values) {
            try {
                SUPPORT_EMAIL.validate(value);
                fail("no exception thrown for value: " + value);
            } catch (IllegalArgumentException consumed) {
                // OK
            }
        }
    }

}
