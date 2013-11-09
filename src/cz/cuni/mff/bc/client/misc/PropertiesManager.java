/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private void checkPropExistance() {
        if (!propSet) {
            loadProperties();
            propSet = true;
        }
    }
    private static final Logger LOG = Logger.getLogger(PropertiesManager.class.getName());

    public PropertiesManager(String file, Handler logHandler) {
        LOG.addHandler(logHandler);
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
                LOG.log(Level.INFO, "Loading properties file");
            } else {
                LOG.log(Level.INFO, "Creating new properties file");
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Properties file couldn''t be loaded: {0}", e.getMessage());
            LOG.log(Level.INFO, "Creating new properties file: {0}", e.getMessage());
            prop = new Properties();
        }
    }

    public void storeProperties() {
        try {
            prop.store(new FileOutputStream(file), null);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Properties file could not be stored {0}", e.getMessage());
        }
    }
}
