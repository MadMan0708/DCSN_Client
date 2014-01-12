/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

/**
 * Client custom class loader
 *
 * @author Jakub Hava
 */
public class ClientCustomCL extends URLClassLoader {

    public ClientCustomCL() {
        super(new URL[]{});
    }

    public ClientCustomCL(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public ClientCustomCL(URL[] urls) {
        super(urls);
    }

    public ClientCustomCL(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
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
