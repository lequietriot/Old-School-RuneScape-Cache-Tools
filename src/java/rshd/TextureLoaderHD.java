/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package rshd;

import net.runelite.cache.definitions.TextureDetails;
import net.runelite.cache.io.InputStream;

public class TextureLoaderHD
{
	public TextureDetails[] textureDefinitions;

	public void load(byte[] b)
	{
		InputStream stream = new InputStream(b);

		int size = stream.readUnsignedShort();
		textureDefinitions = new TextureDetails[size];
		int index;
		for (index = 0; index < size; index++) {
			if (stream.readUnsignedByte() == 1) {
				textureDefinitions[index] = new TextureDetails();
				textureDefinitions[index].setId(index);
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].isGroundMesh = stream.readUnsignedByte() == 0;
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].isHalfSize = stream.readUnsignedByte() == 1;
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].skipTriangles = stream.readUnsignedByte() == 1;
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].brightness = stream.readByte();
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].shadowFactor = stream.readByte();
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].effectId = stream.readByte();
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].effectParam1 = stream.readByte();
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].color = (short) stream.readUnsignedShort();
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].textureSpeedU = stream.readByte();
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].textureSpeedV = stream.readByte();
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].aBool2087 = stream.readUnsignedByte() == 1;
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].isBrickTile = stream.readUnsignedByte() == 1;
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].useMipmaps = stream.readByte();
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].repeatS = stream.readUnsignedByte() == 1;
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].repeatT = stream.readUnsignedByte() == 1;
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].hdr = stream.readUnsignedByte() == 1;
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].combineMode = stream.readUnsignedByte();
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].effectParam2 = stream.readInt();
			}
		}
		for (index = 0; index < size; index++) {
			if (textureDefinitions[index] != null) {
				textureDefinitions[index].blendType = stream.readUnsignedByte();
			}
		}
	}
}
