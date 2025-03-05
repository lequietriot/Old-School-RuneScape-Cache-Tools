/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
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
package net.runelite.cache.definitions.savers;

import net.runelite.cache.definitions.ClientScript1Instruction;
import net.runelite.cache.definitions.InterfaceDefinition;
import net.runelite.cache.io.OutputStream;

public class InterfaceSaver
{
	public byte[] save(InterfaceDefinition def)
	{
		if (def.isIf3)
		{
			return saveIf3(def);
		}
		else
		{
			return saveIf1(def);
		}
	}

	private byte[] saveIf3(InterfaceDefinition def)
	{
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private byte[] saveIf1(InterfaceDefinition def)
	{
		OutputStream out = new OutputStream();
		out.writeByte(def.type);
		out.writeByte(def.buttonType);
		out.writeShort(def.contentType);
		out.writeShort(def.rawX);
		out.writeShort(def.rawY);
		out.writeShort(def.rawWidth);
		out.writeShort(def.rawHeight);
		out.writeByte(def.transparencyTop);
		out.writeShort(def.parentId);
		out.writeShort(def.mouseOverRedirect);
		if (def.cs1Comparisons != null)
		{
			out.writeByte(def.cs1Comparisons.length);
			for (int i = 0; i < def.cs1Comparisons.length; ++i)
			{
				out.writeByte(def.cs1Comparisons[i]);
				out.writeShort(def.cs1ComparisonValues[i]);
			}
		}
		else
		{
			out.writeByte(0);
		}
		if (def.cs1Instructions != null)
		{
			out.writeByte(def.cs1Instructions.length);
			for (int i = 0; i < def.cs1Instructions.length; ++i)
			{
				int len = 0;
				for (int j = 0; j < def.cs1Instructions[i].length; ++j)
				{
					ClientScript1Instruction ins = def.cs1Instructions[i][j];
					len++;
					if (ins.operands != null)
					{
						len += ins.operands.length;
					}
				}
				out.writeShort(len);
				for (int j = 0; j < def.cs1Instructions[i].length; ++j)
				{
					ClientScript1Instruction ins = def.cs1Instructions[i][j];
					out.writeShort(ins.opcode.ordinal());
					if (ins.operands != null)
					{
						for (int op : ins.operands)
						{
							out.writeShort(op);
						}
					}
				}
			}
		}
		else
		{
			out.writeByte(0);
		}
		if (def.type == 0)
		{
			out.writeShort(def.scrollHeight);
			out.writeByte(def.isHidden ? 1 : 0);
		}
		if (def.type == 1)
		{
			out.writeShort(0);
			out.writeByte(0);
		}
		if (def.type == 2)
		{
			out.writeByte((def.flags & 268435456) != 0 ? 1 : 0);
			out.writeByte((def.flags & 1073741824) != 0 ? 1 : 0);
			out.writeByte((def.flags & Integer.MIN_VALUE) != 0 ? 1 : 0);
			out.writeByte((def.flags & 536870912) != 0 ? 1 : 0);
			out.writeByte(def.xPitch);
			out.writeByte(def.yPitch);
			for (int i = 0; i < 20; ++i)
			{
				if (def.sprites[i] != -1)
				{
					out.writeByte(1);
					out.writeShort(def.xOffsets[i]);
					out.writeShort(def.yOffsets[i]);
					out.writeShort(def.sprites[i]);
				}
				else
				{
					out.writeByte(0);
				}
			}
			for (int i = 0; i < 5; ++i)
			{
				if (def.configActions[i] != null)
				{
					out.writeString(def.configActions[i]);
				}
				else
				{
					out.writeString("");
				}
			}
		}
		if (def.type == 3)
		{
			out.writeByte(def.fill ? 1 : 0);
		}
		if (def.type == 4 || def.type == 1)
		{
			out.writeByte(def.textXAlignment);
			out.writeByte(def.textYAlignment);
			out.writeByte(def.textLineHeight);
			out.writeShort(def.fontId);
			out.writeByte(def.textShadowed ? 1 : 0);
		}
		if (def.type == 4)
		{
			out.writeString(def.text);
			out.writeString(def.alternateText);
		}
		if (def.type == 1 || def.type == 3 || def.type == 4)
		{
			out.writeInt(def.color);
		}
		if (def.type == 3 || def.type == 4)
		{
			out.writeInt(def.alternateTextColor);
			out.writeInt(def.hoveredTextColor);
			out.writeInt(def.alternateHoveredTextColor);
		}
		if (def.type == 5)
		{
			out.writeInt(def.spriteId2);
			out.writeInt(def.alternateSpriteId);
		}
		if (def.type == 6)
		{
			out.writeShort(def.modelId);
			out.writeShort(def.alternateModelId);
			out.writeShort(def.sequenceId);
			out.writeShort(def.alternateAnimation);
			out.writeShort(def.modelZoom);
			out.writeShort(def.modelAngleX);
			out.writeShort(def.modelAngleY);
		}
		if (def.type == 7)
		{
			out.writeByte(def.textXAlignment);
			out.writeShort(def.fontId);
			out.writeByte(def.textShadowed ? 1 : 0);
			out.writeInt(def.color);
			out.writeShort(def.xPitch);
			out.writeShort(def.yPitch);
			out.writeByte((def.flags & 1073741824) != 0 ? 1 : 0);
			for (int i = 0; i < 5; ++i)
			{
				out.writeString(def.configActions[i]);
			}
		}
		if (def.type == 8)
		{
			out.writeString(def.text);
		}
		if (def.buttonType == 2 || def.type == 2)
		{
			out.writeString(def.spellActionName);
			out.writeString(def.spellName);
			out.writeShort((def.flags >>> 11) & 63);
		}
		if (def.buttonType == 1 || def.buttonType == 4 || def.buttonType == 5 || def.buttonType == 6)
		{
			out.writeString(def.tooltip);
		}
		return out.flip();
	}
}
