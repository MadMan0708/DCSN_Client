/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.misc;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

/**
 * Client custom class loader
 *
 * @author Jakub Hava
 */
public class CustomClassLoader extends URLClassLoader {

    public CustomClassLoader() {
        super(new URL[]{});
    }

    public CustomClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public CustomClassLoader(URL[] urls) {
        super(urls);
    }

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
