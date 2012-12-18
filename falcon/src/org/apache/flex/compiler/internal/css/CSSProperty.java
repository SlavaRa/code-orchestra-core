/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.flex.compiler.internal.css;

import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;

import org.apache.flex.compiler.css.ICSSProperty;
import org.apache.flex.compiler.css.ICSSPropertyValue;
import com.google.common.base.Strings;

/**
 * Implementation for a CSS property.
 */
public class CSSProperty extends CSSNodeBase implements ICSSProperty
{
    protected CSSProperty(final String name,
                          final CSSPropertyValue value,
                          final CommonTree tree,
                          final TokenStream tokenStream)
    {
        super(tree, tokenStream, CSSModelTreeType.PROPERTY);
        assert !Strings.isNullOrEmpty(name) : "CSS property name can't be empty.";
        assert value != null : "CSS property name can't be null.";
        this.rawName = name;
        this.normalizedName = normalize(name);
        this.value = value;
    }

    private final String normalizedName;
    private final String rawName;
    private final CSSPropertyValue value;

    @Override
    public String getName()
    {
        return normalizedName;
    }

    @Override
    public ICSSPropertyValue getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return String.format("%s : %s ;", rawName, value.toString());
    }

    /**
     * Normalize CSS property names to camel-case style names. Names alread in
     * camel-cases will be returned as-is.
     * <p>
     * For example:<br>
     * {@code normalize("font-family")} returns {@code "fontFamily"}.<br>
     * {@code normalize("fontFamily")} returns {@code "fontFamily"}.
     * 
     * @param name CSS property name
     * @return Camel-case style name.
     */
    protected static String normalize(final String name)
    {
        if (name == null)
            return null;

        final StringBuilder result = new StringBuilder();

        boolean lastCharIsHyphen = false;
        boolean isFirstChar = true;
        for (int i = 0; i < name.length(); i++)
        {
            final char c = name.charAt(i);
            if ('-' == c)
            {
                lastCharIsHyphen = true;
            }
            else if (lastCharIsHyphen)
            {
                result.append(Character.toUpperCase(c));
                lastCharIsHyphen = false;
            }
            else if (isFirstChar)
            {
                result.append(Character.toLowerCase(c));
                isFirstChar = false;
            }
            else
            {
                result.append(c);
            }
        }

        return result.toString();
    }

}
