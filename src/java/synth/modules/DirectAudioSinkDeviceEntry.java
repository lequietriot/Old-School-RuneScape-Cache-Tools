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
package synth.modules;

import static synth.modules.DirectAudioSink.*;

/**
 * A description class for direct audio sink devices.
 * 
 * @author florian
 */
public class DirectAudioSinkDeviceEntry {

	private String devName;
	private String name;
	private int minChannels;
	private int maxChannels;
	private int minRate;
	private int maxRate;
	private int formatMask;
	private int minTransfer;
	private int fifoSize;
	private boolean blockTransfers;
	private boolean doubleBuffering;

	DirectAudioSinkDeviceEntry(String devName, String name, int minChannels,
			int maxChannels, int minRate, int maxRate, int formatMask,
			int minTransfer, int fifoSize, boolean blockTransfers,
			boolean doubleBuffering) {
		this.devName = devName;
		this.name = name;
		this.minChannels = minChannels;
		this.maxChannels = maxChannels;
		this.minRate = minRate;
		this.maxRate = maxRate;
		this.formatMask = formatMask;
		this.minTransfer = minTransfer;
		this.fifoSize = fifoSize;
		this.blockTransfers = blockTransfers;
		this.doubleBuffering = doubleBuffering;
		if (devName.startsWith("plug")) {
			// plug allows wide usage
			this.minChannels = 1;
			this.minRate = minRate / 2;
			this.maxRate = (int) (maxRate * 1.25);
			this.formatMask |= BIT_TYPE_16_BIT | BIT_TYPE_24_BIT4 | BIT_TYPE_32_BIT;
			this.doubleBuffering = true;
			this.blockTransfers = true;
		}
	}

	/**
	 * @return the blockTransfers
	 */
	public boolean isBlockTransfers() {
		return blockTransfers;
	}

	/**
	 * @return the devName
	 */
	public String getDevName() {
		return devName;
	}

	/**
	 * @return the doubleBuffering
	 */
	public boolean isDoubleBuffering() {
		return doubleBuffering;
	}

	/**
	 * @return the fifoSize
	 */
	public int getFifoSize() {
		return fifoSize;
	}

	/**
	 * @return the formatMask
	 */
	public int getFormatMask() {
		return formatMask;
	}

	/**
	 * @return the maxChannels
	 */
	public int getMaxChannels() {
		return maxChannels;
	}

	/**
	 * @return the maxRate
	 */
	public int getMaxRate() {
		return maxRate;
	}

	/**
	 * @return the minChannels
	 */
	public int getMinChannels() {
		return minChannels;
	}

	/**
	 * @return the minRate
	 */
	public int getMinRate() {
		return minRate;
	}

	/**
	 * @return the minTransfer
	 */
	public int getMinTransfer() {
		return minTransfer;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	public String getBitSizeString() {
		String res = "";
		if ((formatMask & BIT_TYPE_16_BIT) != 0) {
			res += "16|";
		}
		if ((formatMask & BIT_TYPE_24_BIT3) != 0) {
			res += "24_3|";
		}
		if ((formatMask & BIT_TYPE_24_BIT4) != 0) {
			res += "24_4|";
		}
		if ((formatMask & BIT_TYPE_32_BIT) != 0) {
			res += "32|";
		}
		if (res != "") {
			res = res.substring(0, res.length()-1)+" ";
			if ((formatMask & BIT_TYPE_BIG_ENDIAN_FLAG) != 0) {
				res += "BE";
			} else {
				res += "LE";
			}
		} else {
			res = "[no bit widths]";
		}
		return res;
	}
	
	public String getFormatString() {
		String channels = (minChannels == maxChannels)?""+minChannels:""+minChannels+"-"+maxChannels;
		String rate = (minRate == maxRate)?""+minRate:""+minRate+"Hz-"+maxRate;
		String result = channels+" channels, "
			+rate+"Hz, "
			+getBitSizeString();
		return result;
	}
	
	private String getPaddedDevName(int length) {
		StringBuffer result = new StringBuffer(devName);
		while (result.length() < length) {
			result.append(" ");
		}
		return result.toString();
	}
	
	public String getFullInfoString() {
		return getPaddedDevName(12)+": "
			+getName()+" ("+getFormatString()
			//+", "
			//+(blockTransfers?"block transfers":"no block transfers")
			//+", "
			//+(doubleBuffering?"double buffering":"direct transfers")
			//+", fifo size = " + getFifoSize()
			+")";
	}
	
	public String toString() {
		return getDevName()+"|"+getName();
	}

}
