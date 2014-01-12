/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.computation;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

/**
 *
 * @author Jakub
 */
public class CustomCL extends URLClassLoader {

    public CustomCL() {
        super(new URL[]{});
    }

    public CustomCL(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public CustomCL(URL[] urls) {
        super(urls);
    }

    public CustomCL(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    public void addNewUrl(URL url) {
        addURL(url);
    }
}
