package plugins;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;

public abstract class IPlugin {
	
	HashMap<String, IServlet> servlets = new HashMap<String, IServlet>();

	public IPlugin(File configFile) throws Exception {
		//System.out.println(configFile);
		System.out.println(configFile.toPath().toString());
		Scanner sc = new Scanner(configFile);
		
		while (sc.hasNext()) {
			try{
			String[] line = sc.nextLine().split("\\|");
			
			String requestType = line[0];
			String servletName = line[1];
			String uri = line[2];
			
			
			IServlet servlet = this.generateServlet(line);
			
			if (!this.servlets.containsKey(uri)) {
				this.servlets.put(uri, servlet);
			} else {
				throw new Exception("Config file already has this servlet: " + servletName);
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