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
package synth.test;

/**
 * A class that allows to allocate memory for simulating regular memory
 * consumption. This can serve to activate the gc.
 * 
 * @author florian
 */

public class MemoryAllocator {

	/**
	 * debugging flag
	 */
	public static boolean DEBUG_ALLOCATE_THREAD = false;

	/**
	 * The thread instance
	 */
	private AllocateThread thread;

	/**
	 * Create a disabled MemoryAllocator instance
	 */
	public MemoryAllocator() {
	}

	/**
	 * Enable the memory allocator. If enable is true, the allocate thread is
	 * created and it will start allocating memory.
	 * 
	 * @param enable if true, start the allocator, otherwise stop it and remove
	 *            the thread
	 */
	public synchronized void setEnabled(boolean enable) {
		if (isEnabled() != enable) {
			if (enable) {
				thread = new AllocateThread();
			} else {
				thread.stopAllocator();
				thread = null;
			}
		}
	}

	/**
	 * @return true if the allocator is running
	 */
	public synchronized boolean isEnabled() {
		return (thread != null);
	}

	// status
	/**
	 * The actual allocator thread.
	 */
	private static class AllocateThread extends Thread {

		/**
		 * The number of bytes allocated in total
		 */
		private long totalAllocated = 0;

		/**
		 * flag to notify the thread to stop
		 */
		private volatile boolean stopped = false;

		/**
		 * the array to hold a reference to the last allocated objects
		 */
		private Object[] allocateArray = null;

		/*
		 * the next write index in allocateArray
		 */
		private int arrayIndex = 0;

		/**
		 * The start time of allocating with one parameter set
		 */
		private long checkpointMillis;

		/**
		 * the number of bytes allocated since the checkpoint
		 */
		private long allocatedSinceCheckpoint;

		/**
		 * Create and start the maintenance thread
		 */
		public AllocateThread() {
			super("Allocator Thread");
			change();
			start();
		}

		/**
		 * Stops and wait for the end of the execution of this thread.
		 */
		public void stopAllocator() {
			synchronized (this) {
				stopped = true;
				this.notifyAll();
			}
			if (isAlive()) {
				try {
					this.join(2000);
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
			}
		}

		/**
		 * Called to re-setup the internal runtime variables
		 */
		public synchronized void change() {
			int referenceObjectCount = 10;
			if (allocateArray == null
					|| allocateArray.length != referenceObjectCount) {
				// create new array
				Object[] newArray = new Object[referenceObjectCount];
				if (allocateArray != null) {
					System.arraycopy(allocateArray, 0, newArray, 0, Math.min(
							referenceObjectCount, allocateArray.length));
				}
				allocateArray = newArray;
			}
			// reset the checkpoint
			checkpointMillis = System.nanoTime() / 1000000L;
			allocatedSinceCheckpoint = 0;
		}

		/**
		 * allocate the specified number of bytes on the heap.
		 */
		private synchronized void allocate(int bytes) {
			Object newObject = new byte[bytes];
			if (arrayIndex >= allocateArray.length) {
				arrayIndex = 0;
			}
			totalAllocated += bytes;
			allocatedSinceCheckpoint += bytes;
			if (DEBUG_ALLOCATE_THREAD) {
				System.out.println("AllocateThread: allocating " + bytes
						+ " bytes at array index " + arrayIndex
						+ ". Total allocated: "
						+ (totalAllocated));
			}

			if (allocateArray != null && arrayIndex < allocateArray.length) {
				allocateArray[arrayIndex++] = newObject;
			}
		}

		/**
		 * in a loop, allocate the objects
		 */
		public void run() {
			if (DEBUG_ALLOCATE_THREAD) {
				System.out.println("AllocateThread: start");
			}
			while (!stopped) {
				try {
					synchronized (this) {
						long elapsedSinceCheckpoint = (System.nanoTime() / 1000000L)
								- checkpointMillis;
						int allocationRate = 1024;
						long shouldHaveAllocated = (long) (((double) allocationRate) * (elapsedSinceCheckpoint / 1000.0));
						// number of bytes that should be allocated now is
						// the difference of shouldHaveAllocated and allocatedSinceCheckpoint
						// we only allocate in chunks of allocateObjectSize
						int allocateObjectSize = 64;
						while ((shouldHaveAllocated - allocatedSinceCheckpoint) >= allocateObjectSize) {
							allocate(allocateObjectSize);
						}
						this.wait(10);
					}
				} catch (Throwable t) {
					System.out.println(String.valueOf(t));
				}
			}
			if (DEBUG_ALLOCATE_THREAD) {
				System.out.println("AllocateThread: stop");
			}
		}

	}
}
