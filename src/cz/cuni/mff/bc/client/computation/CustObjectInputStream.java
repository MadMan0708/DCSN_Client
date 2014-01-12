/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.computation;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;

/**
 *
 * @author Jakub
 */
public class CustObjectInputStream extends ObjectInputStream {

    CustomCL cl;

    public CustObjectInputStream(InputStream in, CustomCL cl) throws IOException {
        super(in);
        this.cl = cl;
    }

    @Override
    protected Class resolveClass(ObjectStreamClass osc)
            throws IOException, ClassNotFoundException {
        try {
            String name = osc.getName();
            return Class.forName(name, false, cl);
        } catch (ClassNotFoundException e) {
            return super.resolveClass(osc);
        }
    }

    @Override
    protected Class resolveProxyClass(String[] interfaces)
            throws IOException, ClassNotFoundException {

        Class[] interfacesClass = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            interfacesClass[i] = Class.forName(interfaces[i], false, cl);
        }

        return Proxy.getProxyClass(cl, interfacesClass);
    }
}
