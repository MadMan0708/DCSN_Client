/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.common.main;

import cz.cuni.mff.bc.common.enums.ELoggerMessages;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Storing and loading properties file Properties file can be stored manually or
 * is automatically stored when new property is added Properties file can be
 * loaded manually or is automatically or is automatically loaded only when is
 * firstly needed
 *
 * @author Jakub
 */
public class PropertiesManager {

    private String file;
    private Properties prop = new Properties();
    private boolean propSet = false;
    private Logger logger;

    private void checkPropExistance() {
        if (!propSet) {
            loadProperties();
            propSet = true;
        }
    }

    public PropertiesManager(Logger logger, String file) {
        this.logger = logger;
        this.file = file;
    }

    public String getProperty(String key) {
        checkPropExistance();
        return prop.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        checkPropExistance();
        return prop.getProperty(key, defaultValue);
    }

    public boolean containsKey(String key) {
        checkPropExistance();
        return prop.containsKey(key);
    }

    public void setProperty(String key, String value) {
        checkPropExistance();
        prop.setProperty(key, value);
        storeProperties();
    }

    public void loadProperties() {
        try {
            File p = new File(file);
            if (p.exists()) {
                prop.load(new FileInputStream(file));
                logger.log("Loading properties file");
            } else {
                logger.log("Creating new properties file");
            }
        } catch (IOException e) {
            logger.log("Properties file couldn't be loaded: " + e.getMessage(), ELoggerMessages.ERROR);
            logger.log("Creating new properties file: " + e.getMessage());
            prop = new Properties();
        }
    }

    public void storeProperties() {
        try {
            prop.store(new FileOutputStream(file), null);
        } catch (IOException e) {
            logger.log("Properties file could not be stored " + e.getMessage(), ELoggerMessages.ERROR);
        }
    }
}
