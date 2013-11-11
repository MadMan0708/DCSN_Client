/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

/**
 *
 * @author Jakub
 */
public class ClientCustomCL extends URLClassLoader{
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

    public void addNewUrl(URL url) {
        addURL(url);
    }
}
