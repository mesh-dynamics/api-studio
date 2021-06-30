package com.cube.launch;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.servlet.ServletContainer;

import io.md.utils.CommonUtils;

import com.cube.ws.CubeApplication;

public class Main {

	private static final Logger LOGGER = LogManager.getLogger(Main.class);

	private static final String JERSEY_SERVLET_NAME = "cubews-servlet";

	public static void main(String[] args) throws Exception {
		new Main().start();
	}

	void start() throws Exception {

		String port = CommonUtils.fromEnvOrSystemProperties("PORT").orElse("8080"); //System.getenv("PORT");

		String contextPath = "";
		String appBase = ".";

		Tomcat tomcat = new Tomcat();
		tomcat.setPort(Integer.valueOf(port));
		tomcat.getHost().setAppBase(appBase);

		Context context = tomcat.addContext(contextPath, appBase);
		Tomcat.addServlet(context, JERSEY_SERVLET_NAME,
			new ServletContainer(/*new JerseyConfiguration()*/new CubeApplication())).setAsyncSupported(true);
		context.addServletMappingDecoded("/cubews/*", JERSEY_SERVLET_NAME);
		/*
		CubeApplication ddd = new CubeApplication();
		System.out.println(ddd);
		*/

		try{
			tomcat.start();
			tomcat.getServer().await();
		}catch (Exception e){
			LOGGER.error("Server start failed existing" , e);
			System.exit(ExitCode.SERVER_PORT_UNAVAILABLE.getValue());
		}

	}
}