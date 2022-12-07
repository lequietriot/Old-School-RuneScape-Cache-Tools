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

import java.util.ArrayList;
import java.util.List;

/**
 * The maintenance thread is a low-priority thread that calls in regular
 * Serviceable classes and synchronizes adjustable clocks to one master clock.
 * 
 * @author florian
 * 
 */

public class MaintenanceThread implements Runnable {
	public static boolean DEBUG_MAINTENANCE = false;
	
	/**
	 * flag to notify the thread to stop
	 */
	private volatile boolean stopped;

	/**
	 * The thread instance
	 */
	private Thread thread;

	/**
	 * The instance of the master clock that will be used to synchronize other
	 * adjustable clocks with.
	 */
	private AudioClock masterClock;

	/**
	 * The interval in milliseconds at which the service methods are called.
	 */
	private int serviceIntervalMillis = 100;

	/**
	 * The interval in milliseconds at which the clocks are synchronized with
	 * the master clock.
	 */
	private int synchronizeIntervalMillis = 1000;

	/**
	 * The list of adjustable clocks that should be synchronized to the master
	 * clock.
	 */
	private List<AdjustableAudioClock> adjustableClocks =
			new ArrayList<AdjustableAudioClock>();

	/**
	 * The list of adjustable clocks that should be synchronized to the master
	 * clock.
	 */
	private List<Serviceable> serviceables = new ArrayList<Serviceable>();

	/**
	 * Empty constructor.
	 */

	public MaintenanceThread() {
		// do nothing
	}

	public synchronized void start() {
		stop();
		thread = new Thread(this, "Synth Maintenance Thread");
		thread.setDaemon(true);
		thread.start();
		stopped = false;
		if (masterClock == null) {
			if (adjustableClocks.size() > 0) {
				System.out.println("MaintenanceThread: synchronizeClocks called, and adjustableClocks "
						+ "are registered, but master clock is not set!");
			}
		}
	}

	public synchronized void stop() {
		if (thread != null) {
			stopped = true;
			synchronized (thread) {
				thread.notifyAll();
				try {
					thread.join();
				} catch (InterruptedException ie) {
				}
			}
			thread = null;
		}
	}

	/**
	 * @return Returns the masterClock.
	 */
	public AudioClock getMasterClock() {
		return masterClock;
	}

	/**
	 * If the master clock is set to null, no clock synchronization is executed.
	 * 
	 * @param masterClock The masterClock to set.
	 */
	public void setMasterClock(AudioClock masterClock) {
		this.masterClock = masterClock;
	}

	/**
	 * Adds an adjustabkle clock to the list of clocks that are synchronized in
	 * regular intervals with the master clock. Synchronization is done by
	 * setting the offset of the AdjustableAudioClock to the difference of the
	 * master clock and the AdjustableClock.
	 * 
	 * @param ac the AdjustableAudioClock to be added to the list of
	 *            synchronized clocks
	 */
	public void addAdjustableClock(AdjustableAudioClock ac) {
		if (ac != null) {
			synchronized (adjustableClocks) {
				adjustableClocks.add(ac);
			}
		}
		
	}

	/**
	 * Remove this adjustable clock from the list of synchronized clocks.
	 */
	public void removeAdjustableClock(AdjustableAudioClock ac) {
		synchronized (adjustableClocks) {
			adjustableClocks.remove(ac);
		}
		
	}

	/**
	 * Adds this Serviceable object to the list of objects whose service()
	 * method is called in regular intervals.
	 */
	public void addServiceable(Serviceable s) {
		if (s != null) {
			synchronized (serviceables) {
				serviceables.add(s);
			}
		}
	}

	/**
	 * Remove this Serviceable from the list of serviced objects.
	 */
	public void removeServiceable(Serviceable s) {
		synchronized (serviceables) {
			serviceables.remove(s);
		}
	}

	/**
	 * Synchronizes all registered AdjustableAudioClocks with the master clock.
	 * @param reset if true, reset the slave clocks to the current master clock. Otherwise, gradually adjust
	 */
	public void synchronizeClocks(boolean reset) {
		final AudioClock thisMasterClock = masterClock;
		if (thisMasterClock == null) return;

		synchronized (adjustableClocks) {
			int i = 0;
			for (AdjustableAudioClock aac : adjustableClocks) {
				AudioTime aacTime = aac.getAudioTime();
				AudioTime masterTime = thisMasterClock.getAudioTime();
				AudioTime diff = aacTime.subtract(masterTime);
				
				// don't use reset parameter for now, always fully adjust
				AudioTime newOffset = aac.getTimeOffset().subtract(diff);

				if (DEBUG_MAINTENANCE) {
					// do not show maintenance for closed devices
					boolean closed = ((aac instanceof MidiIn) && !((MidiIn) aac).isOpen());
					
					if (!closed) {
						System.out.println("Mainten.synchClocks(" + aac + "): "
								+ "master="+ masterTime
								+ " slave=" + aacTime
								//+ " slaveOrig=" + aacTime.subtract(aac.getTimeOffset())
								+ " oldOff=" + aac.getTimeOffset()
								+ " newOff=" + newOffset
								+ " diff=" + diff
								);
					}
				}
				
				aac.setTimeOffset(newOffset);
				i++;
			}
		}
	}

	/**
	 * Call all Serviceables' service() methods.
	 */
	private void doServices() {
		synchronized (serviceables) {
			for (Serviceable serviceable : serviceables) {
				serviceable.service();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		int waitService = serviceIntervalMillis;
		int waitSynchro = synchronizeIntervalMillis;
		while (!stopped) {
			int waitTime = Math.min(waitService, waitSynchro);
			synchronized (thread) {
				try {
					thread.wait(waitTime);
				} catch (InterruptedException ie) {
					break;
				}
			}
			if (stopped) {
				break;
			}
			waitService -= waitTime;
			waitSynchro -= waitTime;
			if (waitService <= 0) {
				try {
					doServices();
					waitService = serviceIntervalMillis;
				} catch (Exception e) {
					System.out.println(String.valueOf(e));
				}
			}
			if (waitSynchro <= 0) {
				waitSynchro = synchronizeIntervalMillis;
				try {
					synchronizeClocks(false);
				} catch (Exception e) {
					System.out.println(String.valueOf(e));
				}
			}
		}

	}

}
