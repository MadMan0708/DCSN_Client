/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.misc.PropertiesManager;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and stores client parameters
 *
 * @author Jakub Hava
 */
public class ClientParams {

    private PropertiesManager propMan;
    private String clientName;
    private String serverAddress;
    private int serverPort;
    private Path downloadDir;
    private Path uploadDir;
    private Path tempDir;
    private int memory;
    private int cores;
    private static final Logger LOG = Logger.getLogger(Client.class.getName());

    /**
     * Constructor
     *
     * @param logHandler logging handler
     */
    public ClientParams(Handler logHandler) {
        propMan = new PropertiesManager("client.config.properties", logHandler);
    }

    private void setDefaultAddress() {
        try {
            setServerAddress("localhost");
        } catch (UnknownHostException e) {
            // tested, can not entry to this part
        }
    }

    private void setDefaultPort() {
        setServerPort(1099);
    }

    private void setDefaulClientName() {
        setClientName(System.getProperty("user.name"));
    }

    private void setDefaultDownloadDir() {
        setDownloadDir(System.getProperty("user.home") + File.separator + "DCSN_downloaded");
    }

    private void setDefaultUploadDir() {
        setUploadDir(System.getProperty("user.home") + File.separator + "DCSN_uploaded");
    }

    private void setDefaultTemporaryDir() {
        setTemporaryDir(System.getProperty("java.io.tmpdir") + File.separator + "DCSN_temporary");
    }

    private void setDefaultCores() {
        setCores(4);
    }

    private void setDefaultMemory() {
        setMemory(125);
    }

    /**
     * Sets the server address and possibly the server port
     *
     * @param connDetails string containing connection details
     * @throws UnknownHostException
     * @throws IllegalArgumentException
     */
    public void setServerIPPort(String connDetails) throws UnknownHostException, IllegalArgumentException {
        String pattern = "^(?<IP>.*):(?<port>\\d+)$";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(connDetails);
        if (m.find()) {
            setServerAddress(m.group("IP"));
            setServerPort(Integer.parseInt(m.group("port")));
        } else {
            setServerAddress(connDetails);
        }
    }

    private boolean validatePort(int port) {
        if (port >= 1 && port <= 65535) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * @param port server port
     * @throws IllegalArgumentException
     */
    public void setServerPort(int port) throws IllegalArgumentException {
        if (validatePort(port)) {
            this.serverPort = port;
            LOG.log(Level.INFO, "Server port is now set to: {0}", serverPort);
            propMan.setProperty("port", serverPort + "");
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     *
     * @return server port
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     *
     * @param memory memory limit
     * @throws IllegalArgumentException
     */
    public void setMemory(int memory) throws IllegalArgumentException {
        if (memory > 0) {
            this.memory = memory;
            LOG.log(Level.INFO, "Amount of memory allowed is now set to: {0}", memory);
            propMan.setProperty("memory", memory + "");
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     *
     * @return memory limit
     */
    public int getMemory() {
        return memory;
    }

    /**
     *
     * @param cores limit
     * @throws IllegalArgumentException
     */
    public void setCores(int cores) throws IllegalArgumentException {
        if (cores > 0) {
            this.cores = cores;
            LOG.log(Level.INFO, "Number of cores allowed is now set to: {0}", cores);
            propMan.setProperty("cores", cores + "");
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     *
     * @return cores limit
     */
    public int getCores() {
        return cores;
    }

    /**
     * Sets the server address
     *
     * @param serverAddress server address
     * @throws UnknownHostException
     */
    public void setServerAddress(String serverAddress) throws UnknownHostException {
        this.serverAddress = InetAddress.getByName(serverAddress).getHostAddress();
        LOG.log(Level.INFO, "Server address is now set to: {0}", this.serverAddress);
        propMan.setProperty("address", this.serverAddress);
    }

    /**
     *
     * @return server address
     */
    public String getServerAddress() {
        return serverAddress;
    }

    /**
     *
     * @return temporary directory
     */
    public Path getTemporaryDir() {
        return tempDir;
    }

    /**
     * Sets and creates temporary directory
     *
     * @param dir path to the temporary directory
     * @return true if the directory has been successfully created, false
     * otherwise
     */
    public boolean setTemporaryDir(String dir) {
        File f = new File(dir);
        if (f.exists() && f.isDirectory()) {
            tempDir = f.toPath().toAbsolutePath();
            LOG.log(Level.INFO, "Temporary dir is set to: {0}", tempDir);
            propMan.setProperty("tempDir", tempDir.toString());
            return true;
        } else {
            if (f.mkdirs()) {
                tempDir = f.toPath().toAbsolutePath();
                LOG.log(Level.INFO, "Temporary dir is set to: {0}", tempDir);
                propMan.setProperty("tempDir", tempDir.toString());
                return true;
            } else {
                LOG.log(Level.WARNING, "Path {0} is not correct path", dir);
                return false;
            }
        }
    }

    /**
     *
     * @return download directory
     */
    public Path getDownloadDir() {
        return downloadDir;
    }

    /**
     * Sets and creates download directory
     *
     * @param dir path to the download directory
     * @return true if the directory has been successfully created, false
     * otherwise
     */
    public boolean setDownloadDir(String dir) {
        File f = new File(dir);
        if (f.exists() && f.isDirectory()) {
            downloadDir = f.toPath().toAbsolutePath();
            LOG.log(Level.INFO, "Download dir is set to: {0}", downloadDir);
            propMan.setProperty("downloadDir", downloadDir.toString());
            return true;
        } else {
            if (f.mkdirs()) {
                downloadDir = f.toPath().toAbsolutePath();
                LOG.log(Level.INFO, "Download dir is set to: {0}", downloadDir);
                propMan.setProperty("downloadDir", downloadDir.toString());
                return true;
            } else {
                LOG.log(Level.WARNING, "Path {0} is not correct path", dir);
                return false;
            }
        }
    }

    /**
     * Sets and creates upload directory
     *
     * @param dir path to the upload directory
     * @return true if the upload has been successfully created, false otherwise
     */
    public boolean setUploadDir(String dir) {
        File f = new File(dir);
        if (f.exists() && f.isDirectory()) {
            uploadDir = f.toPath().toAbsolutePath();
            LOG.log(Level.INFO, "Upload dir is set to: {0}", uploadDir);
            propMan.setProperty("uploadDir", uploadDir.toString());
            return true;
        } else {
            if (f.mkdirs()) {
                uploadDir = f.toPath().toAbsolutePath();
                LOG.log(Level.INFO, "Upload dir is set to: {0}", uploadDir);
                propMan.setProperty("uploadDir", uploadDir.toString());
                return true;
            } else {
                LOG.log(Level.WARNING, "Path {0} is not correct path", dir);
                return false;
            }
        }
    }

    /**
     *
     * @param clientName client's name
     */
    public void setClientName(String clientName) {
        this.clientName = clientName;
        LOG.log(Level.INFO, "Client name is now set to: {0}", this.clientName);
        propMan.setProperty("name", this.clientName);
    }

    /**
     *
     * @return client name
     */
    public String getClientName() {
        return clientName;
    }

    /**
     *
     * @return upload directory
     */
    public Path getUploadDir() {
        return uploadDir;
    }

    /**
     * Initialises parameters
     */
    public void initialisesParameters() {
        if (propMan.getProperty("cores") == null) {
            setDefaultCores();
        } else {
            try {
                setCores(Integer.parseInt(propMan.getProperty("cores")));
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "INITIALIZING: Number of cores has to be positive integer");
                setDefaultCores();
            }
        }

        if (propMan.getProperty("memory") == null) {
            setDefaultMemory();
        } else {
            try {
                setMemory(Integer.parseInt(propMan.getProperty("memory")));
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "INITIALIZING: Amount of memory has to be positive integer");
                setDefaultMemory();
            }
        }

        if (propMan.getProperty("name") == null) {
            setDefaulClientName();
        } else {
            setClientName(propMan.getProperty("name"));
        }

        if (propMan.getProperty("address") == null) {
            setDefaultAddress();
        } else {
            try {
                setServerAddress(propMan.getProperty("address"));
            } catch (UnknownHostException e) {
                LOG.log(Level.WARNING, "INITIALIZING: Not correct host or IP address: {0}", e.getMessage());
                setDefaultAddress();
            }
        }

        if (propMan.getProperty("port") == null) {
            setDefaultPort();
        } else {
            int tmpPort = Integer.parseInt(propMan.getProperty("port"));
            try {
                setServerPort(tmpPort);
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, "INITIALIZING: Port number has to be between 1 - 65535");
                setDefaultPort();
            }
        }

        if (propMan.getProperty("tempDir") == null) {
            setDefaultTemporaryDir();
        } else {
            if (!setTemporaryDir(propMan.getProperty("tempDir"))) {
                setDefaultTemporaryDir();
            }
        }

        if (propMan.getProperty("downloadDir") == null) {
            setDefaultDownloadDir();
        } else {
            if (!setDownloadDir(propMan.getProperty("downloadDir"))) {
                setDefaultDownloadDir();
            }
        }

        if (propMan.getProperty("uploadDir") == null) {
            setDefaultUploadDir();
        } else {
            if (!setUploadDir(propMan.getProperty("uploadDir"))) {
                setDefaultUploadDir();
            }
        }
    }
}
