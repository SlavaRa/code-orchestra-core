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

package org.apache.flex.compiler.internal.tree.mxml;

import java.util.Collection;

import org.antlr.runtime.ANTLRStringStream;

import org.apache.flex.compiler.css.ICSSDocument;
import org.apache.flex.compiler.internal.caches.CSSDocumentCache;
import org.apache.flex.compiler.internal.css.CSSDocument;
import org.apache.flex.compiler.internal.tree.as.NodeBase;
import org.apache.flex.compiler.mxml.IMXMLLanguageConstants;
import org.apache.flex.compiler.mxml.MXMLTagAttributeData;
import org.apache.flex.compiler.mxml.MXMLTagData;
import org.apache.flex.compiler.mxml.MXMLTextData;
import org.apache.flex.compiler.problems.ICompilerProblem;
import org.apache.flex.compiler.problems.MXMLDualContentProblem;
import org.apache.flex.compiler.tree.ASTNodeID;
import org.apache.flex.compiler.tree.mxml.IMXMLStyleNode;

import static org.apache.flex.compiler.mxml.IMXMLLanguageConstants.*;

class MXMLStyleNode extends MXMLNodeBase implements IMXMLStyleNode
{
    /**
     * Constructor
     * 
     * @param parent The parent node of this node, or <code>null</code> if there
     * is no parent.
     */
    MXMLStyleNode(NodeBase parent)
    {
        super(parent);
    }

    /**
     * This field caches parsed CSS model in this node for
     * {@link #getCSSDocument(Collection)}.
     */
    private ICSSDocument cssDocument;

    /**
     * All CSS text in this node will be aggregated here. The CSS text can
     * either be text of {@code <fx:Style>} node or included by
     * {@code <fx:Style src="styles.css" />}.
     */
    private String cssText;

    @Override
    public ASTNodeID getNodeID()
    {
        return ASTNodeID.MXMLStyleID;
    }

    @Override
    public String getName()
    {
        return IMXMLLanguageConstants.STYLE;
    }

    @Override
    public ICSSDocument getCSSDocument(final Collection<ICompilerProblem> problems)
    {
        if (cssDocument == null)
        {
            if (cssText != null && !cssText.isEmpty() && !cssText.trim().isEmpty())
            {
                ANTLRStringStream stream = new ANTLRStringStream(cssText);
                stream.name = getSourcePath();
                cssDocument = CSSDocument.parse(stream, problems);
            }
            else
            {
                cssDocument = CSSDocumentCache.EMPTY_CSS_DOCUMENT;
            }
                          
        }

        return cssDocument;
    }

    @Override
    protected MXMLNodeInfo createNodeInfo(MXMLTreeBuilder builder)
    {
        return new MXMLNodeInfo(builder);
    }

    @Override
    protected void processTagSpecificAttribute(MXMLTreeBuilder builder, MXMLTagData tag,
                                               MXMLTagAttributeData attribute,
                                               MXMLNodeInfo info)
    {
        if (attribute.isSpecialAttribute(ATTRIBUTE_SOURCE))
        {
            // Resolve the attribute value to a normalized path.
            // Doing so makes this compilation unit dependent on that file.
            final String sourcePath = resolveSourceAttributePath(builder, attribute, info);
            if (sourcePath != null)
                cssText = builder.readExternalFile(attribute, sourcePath);
        }
        else
        {
            super.processTagSpecificAttribute(builder, tag, attribute, info);
        }
    }

    @Override
    protected void processChildNonWhitespaceUnit(MXMLTreeBuilder builder, MXMLTagData tag,
                                                 MXMLTextData text,
                                                 MXMLNodeInfo info)
    {
        info.hasDualContent = true;
    }

    @Override
    protected void initializationComplete(MXMLTreeBuilder builder, MXMLTagData tag,
                                          MXMLNodeInfo info)
    {
        super.initializationComplete(builder, tag, info);

        if (info.hasSourceAttribute && info.hasDualContent)
        {
            ICompilerProblem problem = new MXMLDualContentProblem(tag, tag.getShortName());
            builder.addProblem(problem);
            return;
        }

        // If <fx:Style> tag has a valid 'source' attribute, the text of the node is ignored.
        if (cssText == null)
            cssText = tag.getCompilableText();

        // Register this "style" node with the root "MXMLFileNode".
        builder.getFileNode().getStyleNodes().add(this);
    }
}
