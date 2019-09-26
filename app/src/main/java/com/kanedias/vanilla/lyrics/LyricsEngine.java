/*
 * Copyright (C) 2016-2019 Oleg `Kanedias` Chernovskiy <adonai@xaker.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.kanedias.vanilla.lyrics;

import android.content.Loader;
import android.os.HandlerThread;
/**
 * Interface for various engines for lyrics extraction
 *
 * @author Oleg Chernovskiy
 */
public interface LyricsEngine {

    /**
     * Synchronous call to engine to retrieve lyrics. Most likely to be used in {@link HandlerThread}
     * or {@link Loader}
     *
     * @param artistName band or artist name to search for
     * @param songTitle  full song title to search for
     * @return string containing song lyrics if available, null if nothing found
     */
    String getLyrics(String artistName, String songTitle);
}
