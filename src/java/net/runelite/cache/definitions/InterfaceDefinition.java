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
package net.runelite.cache.definitions;

import lombok.Data;

@Data
public class InterfaceDefinition
{
	public boolean isIf3 = false;
	public int id = -1;
	public int type;
	public int contentType;
	public int rawX;
	public int rawY;
	public int rawWidth;
	public int rawHeight;
	public int widthAlignment;
	public int heightAlignment;
	public int xAlignment;
	public int yAlignment;
	public int parentId = -1;
	public boolean isHidden;
	public int scrollWidth;
	public int scrollHeight;
	public boolean noClickThrough;
	public int spriteId2 = -1;
	public int spriteAngle;
	public boolean spriteTiling;
	public int transparencyTop;
	public int outline;
	public int spriteShadow;
	public boolean spriteFlipV;
	public boolean spriteFlipH;
	public int modelType = 1;
	public int modelId = -1;
	public int modelOffsetX;
	public int modelOffsetY;
	public int modelAngleX;
	public int modelAngleZ;
	public int modelAngleY;
	public int modelZoom = 100;
	public int sequenceId = -1;
	public boolean modelOrthog;
	public int modelRotation;
	public int fontId = -1;
	public String text = "";
	public int textLineHeight;
	public int textXAlignment;
	public int textYAlignment;
	public boolean textShadowed;
	public int color;
	public boolean fill;
	public int lineWid = 1;
	public boolean field2943;
	public int flags;
	public String dataText = "";
	public String[] actions;
	public int dragZoneSize;
	public int dragThreshold;
	public boolean isScrollBar;
	public String spellActionName = "";
	public Object[] onLoad;
	public Object[] onMouseOver;
	public Object[] onMouseLeave;
	public Object[] onTargetLeave;
	public Object[] onTargetEnter;
	public Object[] onVarTransmit;
	public Object[] onInvTransmit;
	public Object[] onStatTransmit;
	public Object[] onTimer;
	public Object[] onOp;
	public Object[] onMouseRepeat;
	public Object[] onClick;
	public Object[] onClickRepeat;
	public Object[] onRelease;
	public Object[] onHold;
	public Object[] onDrag;
	public Object[] onDragComplete;
	public Object[] onScroll;
	public int[] varTransmitTriggers;
	public int[] invTransmitTriggers;
	public int[] statTransmitTriggers;
	public boolean hasListener;
	public int buttonType;
	public int mouseOverRedirect;
	public int[] cs1Comparisons;
	public int[] cs1ComparisonValues;
	public ClientScript1Instruction[][] cs1Instructions;
	public int[] itemIds;
	public int[] itemQuantities;
	public int xPitch;
	public int yPitch;
	public int[] xOffsets;
	public int[] yOffsets;
	public int[] sprites;
	public String[] configActions;
	public String alternateText = "";
	public int alternateTextColor;
	public int hoveredTextColor;
	public int alternateHoveredTextColor;
	public int alternateSpriteId = -1;
	public int alternateModelId = -1;
	public int alternateAnimation = -1;
	public String spellName = "";
	public String tooltip = "Ok";
}
