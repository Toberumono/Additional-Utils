package toberumono.utils.files;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A layer over {@link WatchService} (which extends it for convenience) that wraps a preinitialized {@link WatchService} and
 * starts a listening thread that automatically waits for updates.
 * 
 * @author Toberumono
 */
public class SimpleWatcher implements WatchService {
	private final WatchService core;
	private final Thread watcher;
	private boolean closed;
	
	/**
	 * Creates a {@link SimpleWatcher} that wraps a {@link WatchService} and creates a thread that uses <tt>action</tt> to
	 * handle events.
	 * 
	 * @param action
	 *            a {@link Consumer} that handles {@link WatchKey WatchKeys} that have been signaled. This <i>must</i>
	 *            re-validate the key.
	 * @param service
	 *            the {@link WatchService} to wrap
	 */
	public SimpleWatcher(Consumer<WatchKey> action, WatchService service) {
		core = service;
		closed = false;
		watcher = new SimpleWatcherThread(action);
		watcher.start();
	}
	
	private class SimpleWatcherThread extends Thread {
		private final Consumer<WatchKey> action;
		
		public SimpleWatcherThread(Consumer<WatchKey> action) {
			this.action = action;
		}
		
		@Override
		public void run() {
			while (!closed)
				try {
					action.accept(core.take());
				}
				catch (ClosedWatchServiceException e) {
					if (!closed)
						e.printStackTrace();
					break;
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
	}
	
	@Override
	public void close() throws IOException {
		if (closed)
			return;
		this.closed = true;
		core.close(); //This implicitly closes the thread as well
	}
	
	@Override
	public WatchKey poll() {
		return core.poll();
	}
	
	@Override
	public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException {
		return core.poll(timeout, unit);
	}
	
	@Override
	public WatchKey take() throws InterruptedException {
		return core.take();
	}
	
}
