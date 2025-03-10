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
package net.runelite.cache;

import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.definitions.exporters.ItemExporter;
import net.runelite.cache.definitions.loaders.ItemLoader;
import net.runelite.cache.definitions.providers.ItemProvider;
import net.runelite.cache.fs.*;
import net.runelite.cache.util.IDClass;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ItemManager implements ItemProvider
{
	private final Store store;
	private final Map<Integer, ItemDefinition> items = new HashMap<>();

	public ItemManager(Store store)
	{
		this.store = store;
	}

	public void load() throws IOException
	{
		ItemLoader loader = new ItemLoader();

		Storage storage = store.getStorage();
		Index index = store.getIndex(IndexType.CONFIGS);
		Archive archive = index.getArchive(ConfigType.ITEM.getId());

		byte[] archiveData = storage.loadArchive(archive);
		ArchiveFiles files = archive.getFiles(archiveData);

		for (FSFile f : files.getFiles())
		{
			ItemDefinition def = loader.load(f.getFileId(), f.getContents());
			items.put(f.getFileId(), def);
		}
	}

	public void link()
	{
		for (ItemDefinition oc : items.values())
		{
			link(oc);
		}
	}

	private void link(ItemDefinition item)
	{
		if (item.notedTemplate != -1)
		{
			item.linkNote(getItem(item.notedTemplate), getItem(item.notedID));
		}
		if (item.boughtTemplateId != -1)
		{
			item.linkBought(getItem(item.boughtTemplateId), getItem(item.boughtId));
		}
		if (item.placeholderTemplateId != -1)
		{
			item.linkPlaceholder(getItem(item.placeholderTemplateId), getItem(item.placeholderId));
		}
	}

	public Collection<ItemDefinition> getItems()
	{
		return Collections.unmodifiableCollection(items.values());
	}

	public ItemDefinition getItem(int itemId)
	{
		return items.get(itemId);
	}

	public void export(File out) throws IOException
	{
		out.mkdirs();

		for (ItemDefinition def : items.values())
		{
			ItemExporter exporter = new ItemExporter(def);

			File targ = new File(out, def.id + ".json");
			exporter.exportTo(targ);
		}
	}

	public void java(File java) throws IOException
	{
		java.mkdirs();
		try (IDClass ids = IDClass.create(java, "ItemID");
			IDClass nulls = IDClass.create(java, "NullItemID"))
		{
			for (ItemDefinition def : items.values())
			{
				if (def.name.equalsIgnoreCase("NULL"))
				{
					nulls.add(def.name, def.id);
				}
				else
				{
					ids.add(def.name, def.id);
				}
			}
		}
	}

	@Override
	public ItemDefinition provide(int itemId)
	{
		return getItem(itemId);
	}
}
