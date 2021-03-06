package org.jppf.application.template;

import org.jppf.application.template.JobInformationAPI;

import java.net.InetAddress; 

import org.apache.xmlrpc.common.TypeConverterFactoryImpl;
import org.apache.xmlrpc.server.XmlRpcServer; 
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl; 
import org.apache.xmlrpc.webserver.WebServer; 
import org.apache.xmlrpc.server.*;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import java.net.URL;

public class JPPFXmlRpcServer
{	
	public JPPFXmlRpcServer() throws Exception
	{
                Integer port = 8090;
                Integer errorPort = 8081;

                WebServer webServer = new WebServer(port);

                XmlRpcServer server = webServer.getXmlRpcServer();

                PropertyHandlerMapping mapping = new PropertyHandlerMapping();
                mapping.addHandler("system", JobInformationAPI.class);
                server.setHandlerMapping(mapping);

                XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) server.getConfig();
                serverConfig.setEnabledForExtensions(true);
                serverConfig.setContentLengthOptional(false);
                serverConfig.setKeepAliveEnabled(true);

                webServer.start();

                System.out.println("JPPF XMLRPC Server Started");

                XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
                config.setServerURL(new URL("http://127.0.0.1:8090"));
                XmlRpcClient client = new XmlRpcClient();
                client.setConfig(config);
                //Object[] params = new Object[]{"XMLRPC test text", new Integer(9), new Integer(3)};
                //String result = (String) client.execute("system.submitTestJob", params);

                //System.out.println("result was: " + result);
	}
}
