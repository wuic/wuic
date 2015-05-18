//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package com.github.wuic.servlet.jetty;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


/**
 * Abstract Trie implementation.
 * <p>Provides some common implementations, which may not be the most
 * efficient. For byte operations, the assumption is made that the charset
 * is ISO-8859-1</p>
 *
 * @param <V> the type of object that the Trie holds
 */
abstract class AbstractTrie<V>
{
    final boolean _caseInsensitive;

    protected AbstractTrie(boolean insensitive)
    {
        _caseInsensitive=insensitive;
    }

    /* ------------------------------------------------------------ */
    /** Put and entry into the Trie
     * @param s The key for the entry
     * @param v The value of the entry
     * @return True if the Trie had capacity to add the field.
     */
    public abstract boolean put(String s, V v);

    /* ------------------------------------------------------------ */
    /** Get and exact match from a String key
     * @param s The key
     * @param offset The offset within the string of the key
     * @param len the length of the key
     * @return the value for the string / offset / length
     */
    public abstract V get(String s,int offset,int len);

    /* ------------------------------------------------------------ */
    /** Get the best match from key in a String.
     * @param s The string
     * @param offset The offset within the string of the key
     * @param len the length of the key
     * @return The value or null if not found
     */
    public abstract V getBest(String s,int offset,int len);


    /* ------------------------------------------------------------ */
    /** Get and exact match from a segment of a ByteBuufer as key
     * @param b The buffer
     * @param offset The offset within the buffer of the key
     * @param len the length of the key
     * @return The value or null if not found
     */
    public abstract V get(ByteBuffer b,int offset,int len);

    public boolean put(V v)
    {
        return put(v.toString(),v);
    }

    public V remove(String s)
    {
        V o=get(s);
        put(s,null);
        return o;
    }

    public V get(String s)
    {
        return get(s,0,s.length());
    }

    public V get(ByteBuffer b)
    {
        return get(b,0,b.remaining());
    }

    public V getBest(String s)
    {
        return getBest(s,0,s.length());
    }

    public V getBest(byte[] b, int offset, int len)
    {
        return getBest(new String(b,offset,len,StandardCharsets.ISO_8859_1));
    }

    public boolean isCaseInsensitive()
    {
        return _caseInsensitive;
    }

}
