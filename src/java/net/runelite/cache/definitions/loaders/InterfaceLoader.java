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
package net.runelite.cache.definitions.loaders;

import net.runelite.cache.definitions.ClientScript1Instruction;
import net.runelite.cache.definitions.InterfaceDefinition;
import net.runelite.cache.io.InputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InterfaceLoader
{
	public InterfaceDefinition load(int id, byte[] b)
	{
		InterfaceDefinition iface = new InterfaceDefinition();
		iface.id = id;
		if (b[0] == -1)
		{
			decodeIf3(iface, new InputStream(b));
		}
		else
		{
			decodeIf1(iface, new InputStream(b));
		}

		return iface;
	}

	private void decodeIf1(InterfaceDefinition iface, InputStream var1)
	{
		iface.isIf3 = false;
		iface.type = var1.readUnsignedByte();
		iface.buttonType = var1.readUnsignedByte();
		iface.contentType = var1.readUnsignedShort();
		iface.rawX = var1.readShort();
		iface.rawY = var1.readShort();
		iface.rawWidth = var1.readUnsignedShort();
		iface.rawHeight = var1.readUnsignedShort();
		iface.transparencyTop = var1.readUnsignedByte();
		iface.parentId = var1.readUnsignedShort();
		if (iface.parentId == 0xFFFF)
		{
			iface.parentId = -1;
		}
		else
		{
			iface.parentId += iface.id & ~0xFFFF;
		}

		iface.mouseOverRedirect = var1.readUnsignedShort();
		if (iface.mouseOverRedirect == 0xFFFF)
		{
			iface.mouseOverRedirect = -1;
		}

		int var2 = var1.readUnsignedByte();
		int var3;
		if (var2 > 0)
		{
			iface.cs1Comparisons = new int[var2];
			iface.cs1ComparisonValues = new int[var2];

			for (var3 = 0; var3 < var2; ++var3)
			{
				iface.cs1Comparisons[var3] = var1.readUnsignedByte();
				iface.cs1ComparisonValues[var3] = var1.readUnsignedShort();
			}
		}

		var3 = var1.readUnsignedByte();
		int var4;
		int var5;
		int var6;
		if (var3 > 0)
		{
			iface.cs1Instructions = new ClientScript1Instruction[var3][];

			for (var4 = 0; var4 < var3; ++var4)
			{
				var5 = var1.readUnsignedShort();
				int[] bytecode = new int[var5];

				for (var6 = 0; var6 < var5; ++var6)
				{
					bytecode[var6] = var1.readUnsignedShort();
					if (bytecode[var6] == 0xFFFF)
					{
						bytecode[var6] = -1;
					}

					List<ClientScript1Instruction> instructions = new ArrayList<>();
					for (int i = 0; i < bytecode.length;)
					{
						ClientScript1Instruction ins = new ClientScript1Instruction();

						ins.opcode = ClientScript1Instruction.Opcode.values()[bytecode[i++]];

						int ac = ins.opcode.argumentCount;
						ins.operands = Arrays.copyOfRange(bytecode, i, i + ac);

						instructions.add(ins);
						i += ac;
					}
					iface.cs1Instructions[var4] = instructions.toArray(new ClientScript1Instruction[0]);
				}
			}
		}

		if (iface.type == 0)
		{
			iface.scrollHeight = var1.readUnsignedShort();
			iface.isHidden = var1.readUnsignedByte() == 1;
		}

		if (iface.type == 1)
		{
			var1.readUnsignedShort();
			var1.readUnsignedByte();
		}

		if (iface.type == 2)
		{
			iface.itemIds = new int[iface.rawWidth * iface.rawHeight];
			iface.itemQuantities = new int[iface.rawHeight * iface.rawWidth];
			var4 = var1.readUnsignedByte();
			if (var4 == 1)
			{
				iface.flags |= 268435456;
			}

			var5 = var1.readUnsignedByte();
			if (var5 == 1)
			{
				iface.flags |= 1073741824;
			}

			var6 = var1.readUnsignedByte();
			if (var6 == 1)
			{
				iface.flags |= Integer.MIN_VALUE;
			}

			int var7 = var1.readUnsignedByte();
			if (var7 == 1)
			{
				iface.flags |= 536870912;
			}

			iface.xPitch = var1.readUnsignedByte();
			iface.yPitch = var1.readUnsignedByte();
			iface.xOffsets = new int[20];
			iface.yOffsets = new int[20];
			iface.sprites = new int[20];

			int var8;
			for (var8 = 0; var8 < 20; ++var8)
			{
				int var9 = var1.readUnsignedByte();
				if (var9 == 1)
				{
					iface.xOffsets[var8] = var1.readShort();
					iface.yOffsets[var8] = var1.readShort();
					iface.sprites[var8] = var1.readInt();
				}
				else
				{
					iface.sprites[var8] = -1;
				}
			}

			iface.configActions = new String[5];

			for (var8 = 0; var8 < 5; ++var8)
			{
				String var11 = var1.readString();
				if (var11.length() > 0)
				{
					iface.configActions[var8] = var11;
					iface.flags |= 1 << var8 + 23;
				}
			}
		}

		if (iface.type == 3)
		{
			iface.fill = var1.readUnsignedByte() == 1;
		}

		if (iface.type == 4 || iface.type == 1)
		{
			iface.textXAlignment = var1.readUnsignedByte();
			iface.textYAlignment = var1.readUnsignedByte();
			iface.textLineHeight = var1.readUnsignedByte();
			iface.fontId = var1.readUnsignedShort();
			if (iface.fontId == 0xFFFF)
			{
				iface.fontId = -1;
			}

			iface.textShadowed = var1.readUnsignedByte() == 1;
		}

		if (iface.type == 4)
		{
			iface.text = var1.readString();
			iface.alternateText = var1.readString();
		}

		if (iface.type == 1 || iface.type == 3 || iface.type == 4)
		{
			iface.color = var1.readInt();
		}

		if (iface.type == 3 || iface.type == 4)
		{
			iface.alternateTextColor = var1.readInt();
			iface.hoveredTextColor = var1.readInt();
			iface.alternateHoveredTextColor = var1.readInt();
		}

		if (iface.type == 5)
		{
			iface.spriteId2 = var1.readInt();
			iface.alternateSpriteId = var1.readInt();
		}

		if (iface.type == 6)
		{
			iface.modelType = 1;
			iface.modelId = var1.readUnsignedShort();
			if (iface.modelId == 0xFFFF)
			{
				iface.modelId = -1;
			}

			iface.alternateModelId = var1.readUnsignedShort();
			if (iface.alternateModelId == 0xFFFF)
			{
				iface.alternateModelId = -1;
			}

			iface.sequenceId = var1.readUnsignedShort();
			if (iface.sequenceId == 0xFFFF)
			{
				iface.sequenceId = -1;
			}

			iface.alternateAnimation = var1.readUnsignedShort();
			if (iface.alternateAnimation == 0xFFFF)
			{
				iface.alternateAnimation = -1;
			}

			iface.modelZoom = var1.readUnsignedShort();
			iface.modelAngleX = var1.readUnsignedShort();
			iface.modelAngleY = var1.readUnsignedShort();
		}

		if (iface.type == 7)
		{
			iface.itemIds = new int[iface.rawWidth * iface.rawHeight];
			iface.itemQuantities = new int[iface.rawWidth * iface.rawHeight];
			iface.textXAlignment = var1.readUnsignedByte();
			iface.fontId = var1.readUnsignedShort();
			if (iface.fontId == 0xFFFF)
			{
				iface.fontId = -1;
			}

			iface.textShadowed = var1.readUnsignedByte() == 1;
			iface.color = var1.readInt();
			iface.xPitch = var1.readShort();
			iface.yPitch = var1.readShort();
			var4 = var1.readUnsignedByte();
			if (var4 == 1)
			{
				iface.flags |= 1073741824;
			}

			iface.configActions = new String[5];

			for (var5 = 0; var5 < 5; ++var5)
			{
				String var10 = var1.readString();
				if (var10.length() > 0)
				{
					iface.configActions[var5] = var10;
					iface.flags |= 1 << var5 + 23;
				}
			}
		}

		if (iface.type == 8)
		{
			iface.text = var1.readString();
		}

		if (iface.buttonType == 2 || iface.type == 2)
		{
			iface.spellActionName = var1.readString();
			iface.spellName = var1.readString();
			var4 = var1.readUnsignedShort() & 63;
			iface.flags |= var4 << 11;
		}

		if (iface.buttonType == 1 || iface.buttonType == 4 || iface.buttonType == 5 || iface.buttonType == 6)
		{
			iface.tooltip = var1.readString();
			if (iface.tooltip.length() == 0)
			{
				if (iface.buttonType == 1)
				{
					iface.tooltip = "Ok";
				}

				if (iface.buttonType == 4)
				{
					iface.tooltip = "Select";
				}

				if (iface.buttonType == 5)
				{
					iface.tooltip = "Select";
				}

				if (iface.buttonType == 6)
				{
					iface.tooltip = "Continue";
				}
			}
		}

		if (iface.buttonType == 1 || iface.buttonType == 4 || iface.buttonType == 5)
		{
			iface.flags |= 4194304;
		}

		if (iface.buttonType == 6)
		{
			iface.flags |= 1;
		}

	}

	private void decodeIf3(InterfaceDefinition iface, InputStream var1)
	{
		var1.readUnsignedByte();
		iface.isIf3 = true;
		iface.type = var1.readUnsignedByte();
		iface.contentType = var1.readUnsignedShort();
		iface.rawX = var1.readShort();
		iface.rawY = var1.readShort();
		iface.rawWidth = var1.readUnsignedShort();
		if (iface.type == 9)
		{
			iface.rawHeight = var1.readShort();
		}
		else
		{
			iface.rawHeight = var1.readUnsignedShort();
		}

		iface.widthAlignment = var1.readByte();
		iface.heightAlignment = var1.readByte();
		iface.xAlignment = var1.readByte();
		iface.yAlignment = var1.readByte();
		iface.parentId = var1.readUnsignedShort();
		if (iface.parentId == 0xFFFF)
		{
			iface.parentId = -1;
		}
		else
		{
			iface.parentId += iface.id & ~0xFFFF;
		}

		iface.isHidden = var1.readUnsignedByte() == 1;
		if (iface.type == 0)
		{
			iface.scrollWidth = var1.readUnsignedShort();
			iface.scrollHeight = var1.readUnsignedShort();
			iface.noClickThrough = var1.readUnsignedByte() == 1;
		}

		if (iface.type == 5)
		{
			iface.spriteId2 = var1.readInt();
			iface.spriteAngle = var1.readUnsignedShort();
			iface.spriteTiling = var1.readUnsignedByte() == 1;
			iface.transparencyTop = var1.readUnsignedByte();
			iface.outline = var1.readUnsignedByte();
			iface.spriteShadow = var1.readInt();
			iface.spriteFlipV = var1.readUnsignedByte() == 1;
			iface.spriteFlipH = var1.readUnsignedByte() == 1;
		}

		if (iface.type == 6)
		{
			iface.modelType = 1;
			iface.modelId = var1.readUnsignedShort();
			if (iface.modelId == 0xFFFF)
			{
				iface.modelId = -1;
			}

			iface.modelOffsetX = var1.readShort();
			iface.modelOffsetY = var1.readShort();
			iface.modelAngleX = var1.readUnsignedShort();
			iface.modelAngleY = var1.readUnsignedShort();
			iface.modelAngleZ = var1.readUnsignedShort();
			iface.modelZoom = var1.readUnsignedShort();
			iface.sequenceId = var1.readUnsignedShort();
			if (iface.sequenceId == 0xFFFF)
			{
				iface.sequenceId = -1;
			}

			iface.modelOrthog = var1.readUnsignedByte() == 1;
			var1.readUnsignedShort();
			if (iface.widthAlignment != 0)
			{
				iface.modelRotation = var1.readUnsignedShort();
			}

			if (iface.heightAlignment != 0)
			{
				var1.readUnsignedShort();
			}
		}

		if (iface.type == 4)
		{
			iface.fontId = var1.readUnsignedShort();
			if (iface.fontId == 0xFFFF)
			{
				iface.fontId = -1;
			}

			iface.text = var1.readString();
			iface.textLineHeight = var1.readUnsignedByte();
			iface.textXAlignment = var1.readUnsignedByte();
			iface.textYAlignment = var1.readUnsignedByte();
			iface.textShadowed = var1.readUnsignedByte() == 1;
			iface.color = var1.readInt();
		}

		if (iface.type == 3)
		{
			iface.color = var1.readInt();
			iface.fill = var1.readUnsignedByte() == 1;
			iface.transparencyTop = var1.readUnsignedByte();
		}

		if (iface.type == 9)
		{
			iface.lineWid = var1.readUnsignedByte();
			iface.color = var1.readInt();
			iface.field2943 = var1.readUnsignedByte() == 1;
		}

		iface.flags = var1.read24BitInt();
		iface.dataText = var1.readString();
		int var2 = var1.readUnsignedByte();
		if (var2 > 0)
		{
			iface.actions = new String[var2];

			for (int var3 = 0; var3 < var2; ++var3)
			{
				iface.actions[var3] = var1.readString();
			}
		}

		iface.dragZoneSize = var1.readUnsignedByte();
		iface.dragThreshold = var1.readUnsignedByte();
		iface.isScrollBar = var1.readUnsignedByte() == 1;
		iface.spellActionName = var1.readString();
		iface.onLoad = this.decodeListener(iface, var1);
		iface.onMouseOver = this.decodeListener(iface, var1);
		iface.onMouseLeave = this.decodeListener(iface, var1);
		iface.onTargetLeave = this.decodeListener(iface, var1);
		iface.onTargetEnter = this.decodeListener(iface, var1);
		iface.onVarTransmit = this.decodeListener(iface, var1);
		iface.onInvTransmit = this.decodeListener(iface, var1);
		iface.onStatTransmit = this.decodeListener(iface, var1);
		iface.onTimer = this.decodeListener(iface, var1);
		iface.onOp = this.decodeListener(iface, var1);
		iface.onMouseRepeat = this.decodeListener(iface, var1);
		iface.onClick = this.decodeListener(iface, var1);
		iface.onClickRepeat = this.decodeListener(iface, var1);
		iface.onRelease = this.decodeListener(iface, var1);
		iface.onHold = this.decodeListener(iface, var1);
		iface.onDrag = this.decodeListener(iface, var1);
		iface.onDragComplete = this.decodeListener(iface, var1);
		iface.onScroll = this.decodeListener(iface, var1);
		iface.varTransmitTriggers = this.decodeTriggers(var1);
		iface.invTransmitTriggers = this.decodeTriggers(var1);
		iface.statTransmitTriggers = this.decodeTriggers(var1);
	}

	private Object[] decodeListener(InterfaceDefinition iface, InputStream var1)
	{
		int var2 = var1.readUnsignedByte();
		if (var2 == 0)
		{
			return null;
		}
		else
		{
			Object[] var3 = new Object[var2];

			for (int var4 = 0; var4 < var2; ++var4)
			{
				int var5 = var1.readUnsignedByte();
				if (var5 == 0)
				{
					var3[var4] = new Integer(var1.readInt());
				}
				else if (var5 == 1)
				{
					var3[var4] = var1.readString();
				}
			}

			iface.hasListener = true;
			return var3;
		}
	}

	private int[] decodeTriggers(InputStream var1)
	{
		int var2 = var1.readUnsignedByte();
		if (var2 == 0)
		{
			return null;
		}
		else
		{
			int[] var3 = new int[var2];

			for (int var4 = 0; var4 < var2; ++var4)
			{
				var3[var4] = var1.readInt();
			}

			return var3;
		}
	}
}