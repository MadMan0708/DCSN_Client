/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.misc;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

/**
 * Custom class loader
 *
 * @author Jakub Hava
 */
public class CustomClassLoader extends URLClassLoader {

    /**
     * Default constructor
     */
    public CustomClassLoader() {
        super(new URL[]{});
    }

    /**
     * Default constructor
     *
     * @param urls array of URLs which will be added to the class path
     * @param parent parent class loader
     */
    public CustomClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * Default constructor
     *
     * @param urls array of URLs which will be added to the class path
     */
    public CustomClassLoader(URL[] urls) {
        super(urls);
    }

    /**
     * Default constructor
     *
     * @param urls array of URLs which will be added to the class path
     * @param parent parent class loader
     * @param factory URL stream handler factory
     */
    public CustomClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    /**
     *
     * @param url adds new URL to the list where class are being searched
     */
    public void addNewUrl(URL url) {
        addURL(url);
    }
}
