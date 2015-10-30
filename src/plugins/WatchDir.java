package plugins;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class WatchDir implements Runnable {

	private final WatchService watcher;
	private final Map<WatchKey, Path> keys;
	private final boolean recursive;
	private boolean trace = false;
	private HashMap<String, IPlugin> plugins = new HashMap<String, IPlugin>();

	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	/**
	 * Register the given directory with the WatchService
	 */
	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE,
				ENTRY_MODIFY);
		if (trace) {
			Path prev = keys.get(key);
			if (prev != null) {
				if (!dir.equals(prev)) {
					System.out.format("update: %s -> %s\n", prev, dir);
				}
			}
		}
		keys.put(key, dir);
	}

	/**
	 * Register the given directory, and all its sub-directories, with the
	 * WatchService.
	 */
	private void registerAll(final Path start) throws IOException {
		// register directory and sub-directories
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir,
					BasicFileAttributes attrs) throws IOException {
				register(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public WatchDir() throws Exception {
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<WatchKey, Path>();
		this.recursive = false;
		String workDir = System.getProperty("user.dir");
		Path dir = Paths.get(workDir + "\\src\\plugins\\activePlugins");
		String path = Paths.get(workDir).toString()
		+ "\\src\\plugins\\activePlugins";
		File[] files = new File(path).listFiles();
		// If this pathname does not denote a directory, then listFiles()
		// returns null.
		for (File file : files) {
			if (file.isFile()) {
				String plgin = file.getName().substring(0,
						file.getName().toString().lastIndexOf('.'));
				IPlugin p = loadPlugin(plgin); // assume jar name = plugin name
//				Class<?> clazz = Class.forName(plgin);
//				IPlugin p = (IPlugin) clazz.getConstructor().newInstance(file);
				this.plugins.put(plgin, p);
			}
		}
		for(String localKey: this.plugins.keySet()){
			System.out.println("Initilization "+localKey);
		}
		if (recursive) {
			registerAll(dir);
		} else {
			register(dir);
		}

		// enable trace after initial registration
		this.trace = true;
	}

	/**
	 * Process all events for keys queued to the watcher
	 */
	void processEvents() throws Exception {
		for (;;) {

			// wait for key to be signalled
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException x) {
				return;
			}

			Path dir = keys.get(key);
			if (dir == null) {
				System.err.println("WatchKey not recognized!!");
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				Kind<?> kind = event.kind();

				// TBD - provide example of how OVERFLOW event is handled
				if (kind == OVERFLOW) {
					continue;
				}

				// Context for directory entry event is the file name of entry
				WatchEvent<Path> ev = cast(event);
//				System.out.println(ev.toString());
				Path name = ev.context();
//				System.out.println(name.toString());
				Path child = dir.resolve(name);
//				System.out.println(child);

				if (kind == ENTRY_CREATE) {
					String plgin = name.toString().substring(0,
							name.toString().lastIndexOf('.'));
					IPlugin p = loadPlugin(plgin); // assume jar name = plugin name
					this.plugins.put(plgin, p);
				} else if (kind == ENTRY_DELETE) {
					String plgin = name.toString().substring(0,
							name.toString().lastIndexOf('.'));
					this.plugins.remove(plgin);
				}
				for(String localKey: this.plugins.keySet()){
					System.out.println(localKey);
				}
				// if directory is created, and watching recursively, then
				// register it and its sub-directories
				if (recursive && (kind == ENTRY_CREATE)) {
					try {
						if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
							registerAll(child);
						}
					} catch (IOException x) {
						// ignore to keep sample readbale
					}
				}
			}

			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if (!valid) {
				keys.remove(key);

				// all directories are inaccessible
				if (keys.isEmpty()) {
					break;
				}
			}
		}
	}

	public void run() {
		try {
			processEvents();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public IPlugin loadPlugin(String jarName) throws Exception {
		String jarUrl = "file:src\\plugins\\activePlugins\\" + jarName+".jar";
		URL classUrl = new URL(jarUrl);
		URL[] classUrls = { classUrl };
		URLClassLoader urlClassLoader = new URLClassLoader(classUrls);
		Class<?> beanClass = urlClassLoader.loadClass(jarName + ".PluginCreator");
		
		// Create a new instance from the loaded class
		Constructor<?> constructor = beanClass.getConstructor();
		Object beanObj = constructor.newInstance();
		Method method = beanClass.getMethod("createPlugin", File.class);
		
		// NOTE: This URL is wrong because we want the path from the plugin project
		String pluginFileUrl = "src\\" + jarName + ".txt";
		InputStream is = getClass().getResourceAsStream(jarName + ".txt");
		byte[] buffer = new byte[is.available()];
		is.read(buffer);
		File targetFile = new File("src/plugins/" + jarName + ".txt");
		
		System.out.println("pluginFileUrl: " + pluginFileUrl);
		System.out.println("----------");
		File f = new File(pluginFileUrl);
		
		IPlugin ip = (IPlugin) method.invoke(beanObj, f);
		return ip;
	}

	/**
	 * @return map of plugins
	 */
	public HashMap<String, IPlugin> getPlugins() {
		return this.plugins;
	}

}
