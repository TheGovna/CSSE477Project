package plugins;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Scanner;

public abstract class IPlugin {
	
	HashMap<String, IServlet> servlets = new HashMap<String, IServlet>();

	public IPlugin(File configFile) throws Exception {
		System.out.println(configFile.toPath().toString());
		Scanner sc = new Scanner(configFile);
		
		while (sc.hasNext()) {
			try{
			String[] line = sc.nextLine().split("\\|");
			
			String requestType = line[0];
			String uri = line[1];
			
			String jarName = configFile.toPath().toString().substring(configFile.toPath().toString().lastIndexOf('\\')+1, configFile.toPath().toString().lastIndexOf("."));
			String jarUrl = "file:src\\plugins\\activePlugins\\" + jarName +".jar";
			URL classUrl = new URL(jarUrl);
			URL[] classUrls = { classUrl };
			URLClassLoader urlClassLoader = new URLClassLoader(classUrls);
			Class<?> beanClass = urlClassLoader.loadClass(jarName + "." +  line[2]);
			
			// Create a new instance from the loaded class
			Constructor<?> constructor = beanClass.getConstructor();
			IServlet servlet = (IServlet) constructor.newInstance();
			
			String key = requestType + ":" + uri;
			
			if (!this.servlets.containsKey(key)) {
				this.servlets.put(key, servlet);
			} else {
				throw new Exception("Config file already has this mapping for: " + key);
			}
			}catch(Exception e){
				System.out.println("Something went wrong with creating the plugin.");
				e.printStackTrace();
			}
			
		}
	}
	
	public abstract IServlet generateServlet(String[] servlets);
	
	public IServlet getServlet(String servlet) {
		return this.servlets.get(servlet);
	}

}
