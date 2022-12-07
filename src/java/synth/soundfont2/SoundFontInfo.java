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
package synth.soundfont2;

/**
 * A class to store all informational/meta data from the soundfont file.
 * 
 * @author florian
 * 
 */
public class SoundFontInfo {
	private int versionMajor;
	private int versionMinor;
	private String soundEngine = "EMU8000";
	protected String name = "(unknown)";
	private String romName;
	private int romVersionMajor;
	private int romVersionMinor;
	private String creationDate;
	private String engineer;
	private String product;
	private String copyright;
	private String comment;
	private String software;

	public int getVersionMajor() {
		return versionMajor;
	}

	public int getVersionMinor() {
		return versionMinor;
	}

	void setVersion(int versionMajor, int versionMinor) {
		this.versionMajor = versionMajor;
		this.versionMinor = versionMinor;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the soundEngine.
	 */
	public String getSoundEngine() {
		return soundEngine;
	}

	/**
	 * @param soundEngine The soundEngine to set.
	 */
	void setSoundEngine(String soundEngine) {
		this.soundEngine = soundEngine;
	}

	/**
	 * @return Returns the romName.
	 */
	public String getRomName() {
		return romName;
	}

	/**
	 * @param romName The romName to set.
	 */
	public void setRomName(String romName) {
		this.romName = romName;
	}

	void setROMVersion(int versionMajor, int versionMinor) {
		this.romVersionMajor = versionMajor;
		this.romVersionMinor = versionMinor;
	}

	public int getROMVersionMajor() {
		return romVersionMajor;
	}

	public int getROMVersionMinor() {
		return romVersionMinor;
	}

	/**
	 * @return Returns the comment.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment The comment to set.
	 */
	void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @return Returns the copyright.
	 */
	public String getCopyright() {
		return copyright;
	}

	/**
	 * @param copyright The copyright to set.
	 */
	void setCopyright(String copyright) {
		this.copyright = copyright;
	}

	/**
	 * @return Returns the creationDate.
	 */
	public String getCreationDate() {
		return creationDate;
	}

	/**
	 * @param creationDate The creationDate to set.
	 */
	void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	/**
	 * @return Returns the engineer.
	 */
	public String getEngineer() {
		return engineer;
	}

	/**
	 * @param engineer The engineer to set.
	 */
	void setEngineer(String engineer) {
		this.engineer = engineer;
	}

	/**
	 * @return Returns the product.
	 */
	public String getProduct() {
		return product;
	}

	/**
	 * @param product The product to set.
	 */
	void setProduct(String product) {
		this.product = product;
	}

	/**
	 * @return Returns the software.
	 */
	public String getSoftware() {
		return software;
	}

	/**
	 * @param software The software to set.
	 */
	void setSoftware(String software) {
		this.software = software;
	}

}
