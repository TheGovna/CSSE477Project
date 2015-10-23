package plugins;

import java.io.IOException;

public class WDTest {
	
	public static void main(String[] args) {
		try {
			WatchDir wd = new WatchDir();
			Thread t = new Thread(wd);
			t.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
