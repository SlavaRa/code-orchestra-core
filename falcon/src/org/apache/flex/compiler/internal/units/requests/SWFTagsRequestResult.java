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

package org.apache.flex.compiler.internal.units.requests;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.flex.compiler.internal.embedding.EmbedData;
import org.apache.flex.compiler.internal.embedding.transcoders.TranscoderBase;
import org.apache.flex.compiler.problems.ICompilerProblem;
import org.apache.flex.compiler.units.requests.ISWFTagsRequestResult;
import org.apache.flex.swf.SWFFrame;
import org.apache.flex.swf.tags.DoABCTag;
import org.apache.flex.swf.tags.ICharacterTag;
import org.apache.flex.swf.tags.ITag;

/**
 * Create a {@code DoABC} SWF tag for the given ABC byte code. The
 * {@link #addToFrame(SWFFrame)} call back method will add the {@code DoABC} tag
 * to a frame.
 */
public class SWFTagsRequestResult implements ISWFTagsRequestResult
{
    /**
     * Create a {@code SWFTagsRequestResult}.
     * 
     * @param abcData Raw ABC byte array.
     * @param tagName SWF tag name.
     */
    public SWFTagsRequestResult(final byte[] abcData, final String tagName)
    {
        this(abcData, tagName, Collections.<EmbedData>emptySet());
    }

    /**
     * Create a {@code SWFTagsRequestResult}.
     * 
     * @param abcData Raw ABC byte array.
     * @param tagName SWF tag name.
     * @param embeddedAssets embedded assets
     */
    public SWFTagsRequestResult(final byte[] abcData, final String tagName,
                         final Collection<EmbedData> embeddedAssets)
    {
        this.abcData = abcData;
        this.tagName = tagName;
        this.problems = new LinkedList<ICompilerProblem>();
        this.additionalTags = new LinkedList<ITag>();
        this.assetTags = new LinkedHashMap<String, ICharacterTag>();

        for (EmbedData embedData : embeddedAssets)
        {
            TranscoderBase transcoder = embedData.getTranscoder();
            Map<String, ICharacterTag> tags = transcoder.getTags(additionalTags, this.problems);

            if (tags != null)
                assetTags.putAll(tags);
        }
    }

    private final byte[] abcData;
    private final String tagName;
    private final List<ITag> additionalTags;
    private final Map<String, ICharacterTag> assetTags;
    private final Collection<ICompilerProblem> problems;

    @Override
    public ICompilerProblem[] getProblems()
    {
        return problems.toArray(new ICompilerProblem[problems.size()]);
    }

    @Override
    public boolean addToFrame(SWFFrame f)
    {
        if (abcData == null)
            return false;

        final DoABCTag doABC = new DoABCTag();
        doABC.setABCData(abcData);
        doABC.setName(tagName);

        for (ITag tag : additionalTags)
        {
            f.addTag(tag);
        }

        f.addTag(doABC);

        for (Entry<String, ICharacterTag> entrySet : assetTags.entrySet())
        {
            ICharacterTag assetTag = entrySet.getValue();
            f.addTag(assetTag);
            f.defineSymbol(assetTag, entrySet.getKey());
        }

        return true;
    }

    @Override
    public String getDoABCTagName()
    {
        return tagName;
    }
}
