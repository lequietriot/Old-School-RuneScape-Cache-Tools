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
package synth.utils;

import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * A thread to execute the listener asynchronously.
 */

public class AsynchExec<T> implements Runnable {

	public static boolean DEBUG_ASYNCH_EXEC = false;
	
	private Listener<T> listener;
	private String name;
	
	/**
	 * The list of asynchronous events
	 */
	private Queue<T> execs = new PriorityBlockingQueue<T>();

	/**
	 * The event dispatcher thread
	 */
	private Thread thread;

	private volatile boolean stopped = false;

	public AsynchExec(Listener<T> listener, String name) {
		this.name = name;
		this.listener = listener;
	}

	public synchronized void start() {
		stop();
		stopped = false;
		thread = new Thread(this, name);
		thread.setDaemon(true);
		thread.start();
	}

	public synchronized void stop() {
		if (!stopped && thread != null && thread.isAlive()) {
			stopped = true;
			synchronized (execs) {
				execs.notifyAll();
			}
			try {
				thread.join();
			} catch (InterruptedException ie) {
				System.out.println(String.valueOf(ie));
			}
		}
		thread = null;
	}
	
	public synchronized boolean isStarted() {
		return !stopped;
	}

	/**
	 * Call the registered listener asynchronously with the specified parameter.
	 * @param o the object passed on to the listener as parameter
	 */
	public void invokeLater(T o) {
		execs.offer(o);
		synchronized (execs) {
			execs.notifyAll();
		}
	}

	public void run() {
		if (DEBUG_ASYNCH_EXEC) {
			System.out.println(name+": start.");
		}
		try {
			while (!stopped) {
				if (execs.isEmpty()) {
					synchronized (execs) {
						execs.wait();
					}
				}
				while (!stopped && !execs.isEmpty()) {
					T event = execs.poll();
					if (listener != null) {
						listener.onAsynchronousExecution(event);
					}
				}
			}
		} catch (InterruptedException ie) {
			System.out.println(String.valueOf(ie));
		}
	}
	
	public interface Listener<T> {
		public void onAsynchronousExecution(T event);
	}
}

