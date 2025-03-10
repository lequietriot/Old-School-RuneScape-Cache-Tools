/*
 * Copyright (c) 2022, Joshua Filby <joshua@filby.me>
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

import net.runelite.cache.definitions.DBTableIndex;
import net.runelite.cache.definitions.loaders.DBTableIndexLoader;
import net.runelite.cache.fs.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DBTableIndexManager
{
	private final Store store;
	private final Map<Integer, DBTableIndex> indexes = new HashMap<>();

	public DBTableIndexManager(Store store)
	{
		this.store = store;
	}

	public void load() throws IOException
	{
		DBTableIndexLoader loader = new DBTableIndexLoader();

		Storage storage = store.getStorage();
		Index index = store.getIndex(IndexType.DBTABLEINDEX);
		if (index == null)
		{
			return;
		}

		for (Archive archive : index.getArchives())
		{
			byte[] archiveData = storage.loadArchive(archive);
			ArchiveFiles files = archive.getFiles(archiveData);

			for (FSFile f : files.getFiles())
			{
				// file 0 = master index, all others are fileId - 1 = columnId
				DBTableIndex row = loader.load(archive.getArchiveId(), f.getFileId() - 1, f.getContents());
				indexes.put(archive.getArchiveId() << 16 | f.getFileId(), row);
			}
		}
	}

	public Collection<DBTableIndex> getIndexes()
	{
		return Collections.unmodifiableCollection(indexes.values());
	}

	/**
	 * Gets the master index for the specified table.
	 *
	 * @param tableId The table id to lookup.
	 * @return The master index with all rows belonging to the table.
	 */
	public DBTableIndex getMaster(int tableId)
	{
		return indexes.get(tableId << 16);
	}

	/**
	 * Gets the index for the specified table and column.
	 *
	 * @param tableId The table id to lookup.
	 * @param columnId The column id to lookup.
	 * @return The index for the specific table column.
	 */
	public DBTableIndex get(int tableId, int columnId)
	{
		return indexes.get(tableId << 16 | columnId + 1);
	}
}
