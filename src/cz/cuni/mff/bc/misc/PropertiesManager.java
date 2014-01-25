/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Storing and loading properties file. Properties can be stored manually or are
 * automatically stored when new property is added. Properties can be loaded
 * manually or are automatically loaded only when the property is firstly needed
 *
 * @author Jakub Hava
 */
public class PropertiesManager {

    private String file;
    private Properties prop = new Properties();
    private boolean propSet = false;
    private static final Logger LOG = Logger.getLogger(PropertiesManager.class.getName());

    /**
     * Constructor
     *
     * @param file file where to store properties
     * @param logHandler logging handler
     */
    public PropertiesManager(String file, Handler logHandler) {
        LOG.addHandler(logHandler);
        this.file = file;
    }

    /**
     * Gets the property
     *
     * @param key key of the property
     * @return value of the property
     */
    public String getProperty(String key) {
        checkPropExistance();
        return prop.getProperty(key);
    }

    /**
     * Gets the property
     *
     * @param key key of the property
     * @param defaultValue default value of the property
     * @return value of the property
     */
    public String getProperty(String key, String defaultValue) {
        checkPropExistance();
        return prop.getProperty(key, defaultValue);
    }

    /**
     * Checks if the properties contains the given key
     *
     * @param key key to check
     * @return true if properties contains the key, false otherwise
     */
    public boolean containsKey(String key) {
        checkPropExistance();
        return prop.containsKey(key);
    }

    /**
     * Sets the property and stores the properties file
     *
     * @param key key of the property
     * @param value value of the property
     */
    public void setProperty(String key, String value) {
        checkPropExistance();
        prop.setProperty(key, value);
        storeProperties();
    }

    /**
     * Loads the properties
     */
    public void loadProperties() {
        try {
            File p = new File(file);
            if (p.exists()) {
                prop.load(new FileInputStream(file));
                LOG.log(Level.FINE, "Loading properties file");
            } else {
                LOG.log(Level.FINE, "Creating new properties file");
            }
        } catch (IOException e) {
            LOG.log(Level.FINE, "Properties file couldn''t be loaded: {0}", e.getMessage());
            LOG.log(Level.FINE, "Creating new properties file: {0}", e.getMessage());
            prop = new Properties();
        }
    }

    /**
     * Stores the properties
     */
    public void storeProperties() {
        try {
            prop.store(new FileOutputStream(file), null);
        } catch (IOException e) {
            LOG.log(Level.FINE, "Properties file could not be stored {0}", e.getMessage());
        }
    }

    // loads properties if they aren't loaded so far
    private void checkPropExistance() {
        if (!propSet) {
            loadProperties();
            propSet = true;
        }
    }
}
