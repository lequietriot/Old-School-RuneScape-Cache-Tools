/*
 * (C) Copyright IBM Corp. 2005, 2008
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package synth.engine;


/**
 * A class to maintain a pool of threads which continously render a set of
 * Renderables. For that to work, the method dispatch() must be called for every
 * block to be rendered.
 * 
 * @author florian
 */

public class AsynchronousRenderer {

	public static boolean DEBUG_ASYNC_RENDERER = false;

	/**
	 * Priority of the render thread -- on a scale from 0 to 10.
	 */
	private static final int RENDER_THREAD_PRIORITY = AudioPullThread.PULLTHREAD_PRIORITY;

	/**
	 * Flag: if <code>true</code>, each render thread is assigned a pre-set
	 * range of Renderables that it alone will render. If <code>false</code>,
	 * each thread will try to render all Renderables from the beginning, using
	 * synchronization to prevent concurrent access to the renderables array.
	 * <p>
	 * Partitioning works in a way that the first thread will render the 0th,
	 * Nth, (2*N)th, etc. element, the second thread elements 1, (N+1), (2N+1),
	 * etc. where N is the number of threads. This is to insure that rendering
	 * goes approximately in order of the Renderables array. This enables the
	 * mixer to start mixing together the first Renderables while the rendering
	 * threads are still busy with rendering the last Renderables.
	 */
	private static final boolean PARTITION_RENDERABLES = true;

	/**
	 * Flag: if <code>true</code>, the rendering threads will call
	 * <code>wait()</code> to wait for the synchronized start of a new slice.
	 * Otherwise the threads will just call <code>yield()</code> until a new
	 * set of Renderables is available.
	 */
	private static final boolean USE_WAIT = true;

	private volatile boolean started = false;

	private static int defaultThreadCount;
	
	static {
		defaultThreadCount = Runtime.getRuntime().availableProcessors();
		if (defaultThreadCount < 2) {
			// do not use separate render threads if only one processor 
			// core available
			defaultThreadCount = 0;
		}
	}
		

	/**
	 * The array of rendering threads. Must always be non-null!
	 */
	private RenderingThread[] threads = new RenderingThread[0];

	/**
	 * The target number of threads that is used when start() is called
	 */
	private int threadCount;

	/**
	 * The time of the current or next block to be rendered.
	 */
	private volatile AudioTime renderTime;

	/**
	 * The array of Renderable that contain the most recent list of objects to
	 * be rendered. Must never be null!
	 */
	private volatile Renderable[] renderableArray = new Renderable[0];

	/**
	 * Create an AsynchronousRenderer with the specified number of threads.
	 */
	public AsynchronousRenderer(int threadCount) {
		this.threadCount = threadCount;
	}

	/**
	 * @return the defaultThreadCount, which is the number of processors on the
	 *         system
	 */
	public static int getDefaultThreadCount() {
		return defaultThreadCount;
	}

	/**
	 * @return if the render thread is started
	 */
	public synchronized boolean isStarted() {
		return started;
	}

	/**
	 * @return the threadCount
	 */
	public synchronized int getThreadCount() {
		return threadCount;
	}

	/**
	 * Change the number of active threads
	 */
	public synchronized void setThreadCount(int c) {
		if (c < 1) {
			throw new IllegalArgumentException("Illegal number of threads: "
					+ c);
		}
		threadCount = c;
		assertThreadCount();
	}

	/**
	 * Create and start the rendering threads.
	 */
	public synchronized void start() {
		if (!started) {
			started = true;
			assertThreadCount();
		}
	}

	/**
	 * Stop and destroy the rendering threads. This method blocks until all
	 * threads have died.
	 */
	public synchronized void stop() {
		if (started) {
			started = false;
			for (RenderingThread rt : threads) {
				rt.finish();
			}
			try {
				while (getActiveCount() > 0) {
					this.wait();
				}
			} catch (InterruptedException ie) {
				// nothing
			}
			threads = new RenderingThread[0];
		}
	}

	public synchronized int getActiveCount() {
		int result = 0;
		for (RenderingThread rt : threads) {
			if (rt.isRunning()) {
				result++;
			}
		}
		return result;
	}

	private synchronized void assertThreadCount() {
		if (!started) {
			return;
		}
		// first assert that the array has the right size
		if (threads.length != threadCount) {
			// stop excess threads
			for (int i = threadCount; i < threads.length; i++) {
				if (threads[i] != null) {
					threads[i].finish();
					threads[i] = null;
				}
			}
			RenderingThread[] newThreads = new RenderingThread[threadCount];
			System.arraycopy(threads, 0, newThreads, 0,
					Math.min(threadCount, threads.length));
			threads = newThreads;
		}
		// here we can be sure that threads.length == threadCount
		// verify that all threads in the array exist and are running
		for (int i = 0; i < threads.length; i++) {
			RenderingThread rt = threads[i];
			if (rt == null || rt.isStopped()) {
				threads[i] = new RenderingThread(i);
			}
		}
	}

	/**
	 * Start a new slice in all threads
	 */
	public synchronized void dispatch(AudioTime time, Renderable[] renderables) {
		this.renderTime = time;
		this.renderableArray = renderables;
		if (PARTITION_RENDERABLES) {
			int threadCount = threads.length;
			for (int i = 0; i < threadCount; i++) {
				threads[i].nextSlice(i, threadCount);
			}
		} else {
			for (RenderingThread rt : threads) {
				rt.nextSlice();
			}
		}
	}

	/**
	 * A thread to call the render method of the inputs of a mixer. Note: only
	 * this classes' run() method may wait on this object. Otherwise,
	 * notifyAll() needs to be used.
	 */
	private class RenderingThread implements Runnable {

		private boolean running;
		private volatile boolean doStop;

		/**
		 * For partitioned rendering: the first element in renderables that is
		 * to be rendered.
		 */
		private int partitionStart;

		/**
		 * For partitioned rendering: how many elements to skip.
		 */
		private int partitionInc;
		
		private final String name;

		/**
		 * Create a rendering thread with the thread number threadNum.
		 * 
		 * @param threadNum the number/name of this thread
		 */
		public RenderingThread(int threadNum) {
			running = true;
			doStop = false;
			name = "Rendering thread " + threadNum;
			ThreadFactory.createThread(this, name, RENDER_THREAD_PRIORITY);
		}

		/**
		 * @return true if this thread is (still) running or about to run. This
		 *         may even be true if finish was called, before the thread
		 *         actually died.
		 */
		public boolean isRunning() {
			return running;
		}

		/**
		 * @return If this thread is not running or about to be stopped
		 */
		public synchronized boolean isStopped() {
			return !running || doStop;
		}

		/**
		 * Stop the thread (asynchronously)
		 */
		public synchronized void finish() {
			this.doStop = true;
			if (USE_WAIT) {
				this.notify();
			}
		}

		public synchronized void nextSlice() {
			assert (!PARTITION_RENDERABLES);
			if (USE_WAIT) {
				this.notify();
			}
		}

		public synchronized void nextSlice(int startIndex, int increment) {
			assert (PARTITION_RENDERABLES);
			this.partitionStart = startIndex;
			this.partitionInc = increment;
			if (USE_WAIT) {
				this.notify();
			}
		}

		/**
		 * The method to actually call the render() method of all Renderables.
		 * The method iterates from the first element to the last element. For
		 * each element, it checks if it is non-null. If it is, the element is
		 * stored locally and the entry in the array is set to null to prevent
		 * other threads from trying to render it as well. Then it calls the
		 * render() method of the Renderable.
		 * <p>
		 * It uses a local copy of renderables to be immune against intermittent
		 * changes of the array instance. Access to the renderables array is
		 * synchronized by the array instance.
		 */
		private void render(AudioTime time, Renderable[] rs) {
			int count = rs.length;
			for (int i = 0; i < count; i++) {
				if (time != renderTime) {
					break;
				}
				// get the next Renderable
				Renderable r;
				r = rs[i];
				if (r != null) {
					rs[i] = null;
				} else {
					// this Renderable is already set to null by another
					// rendering thread
					continue;
				}
				// when we reach this point, r must be set to a non-null
				// Renderable
				r.render(time);
			}
		}

		/**
		 * The render method for partitioned rendering.
		 * 
		 * @see #render(AudioTime, Renderable[])
		 */
		private void render(AudioTime time, Renderable[] rs, int start,
							int inc) {
			int count = rs.length;
			for (int i = start; i < count; i += inc) {
				// get the next Renderable. Don't need to synchronize, since
				// the renderables are assigned
				Renderable r = rs[i];
				if (r == null || time != renderTime) {
					// if r is null, we have reached the "empty" remainder
					// of the array
					break;
				}
				r.render(time);

			}
		}

		public void run() {
			AudioTime lastRenderTime = null;
			if (DEBUG_ASYNC_RENDERER) {
				System.out.println("Start " + getName());
			}
			while (!doStop) {
				// double while loop for try..catch block,
				// to prevent generation of try stack frame for every
				// inner loop
				try {
					while (!doStop) {
						if (USE_WAIT) {
							synchronized (this) {
								while (!doStop && lastRenderTime == renderTime) {
									this.wait();
								}
							}
						} else {
							while (!doStop && lastRenderTime == renderTime) {
								Thread.yield();
							}
						}
						while (!doStop && lastRenderTime != renderTime) {
							lastRenderTime = renderTime;
							if (PARTITION_RENDERABLES) {
								render(renderTime, renderableArray, partitionStart, partitionInc);
							} else {
								render(renderTime, renderableArray);
							}
							// end interval
						}
					}
				} catch (Throwable t) {
					System.out.println(String.valueOf(t));
				}
			}
			synchronized (AsynchronousRenderer.this) {
				running = false;
				AsynchronousRenderer.this.notifyAll();
			}
		}
		
		public String getName() {
			return name;
		}
		
	}
	
}
