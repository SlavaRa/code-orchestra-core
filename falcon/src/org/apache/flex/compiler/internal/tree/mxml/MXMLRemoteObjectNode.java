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

import org.apache.flex.compiler.common.XMLName;
import org.apache.flex.compiler.internal.projects.FlexProject;
import org.apache.flex.compiler.internal.tree.as.NodeBase;
import org.apache.flex.compiler.mxml.MXMLTagData;
import org.apache.flex.compiler.tree.ASTNodeID;
import org.apache.flex.compiler.tree.mxml.IMXMLRemoteObjectNode;

/**
 * Implementation of the {@link IMXMLRemoteObjectNode} interface.
 */
class MXMLRemoteObjectNode extends MXMLInstanceNode implements IMXMLRemoteObjectNode
{
    /**
     * Short name of the special {@code <method>} child tag.
     */
    private static final String TAG_METHOD = "method";

    /**
     * The XML base name to which a {@code <method>} tag under a
     * {@code <RemoteObject>} tag is mapped.
     */
    private static final String BASENAME_REMOTE_OBJECT_METHOD = "RemoteObjectOperation";

    /**
     * Constructor.
     * 
     * @param parent The parent node of this node, or <code>null</code> if there
     * is no parent.
     */
    MXMLRemoteObjectNode(NodeBase parent)
    {
        super(parent);
    }

    @Override
    public ASTNodeID getNodeID()
    {
        return ASTNodeID.MXMLRemoteObjectID;
    }

    @Override
    protected void processChildTag(MXMLTreeBuilder builder, MXMLTagData tag, MXMLTagData childTag, MXMLNodeInfo info)
    {
        // Create MXMLRemoteObjectMethodNode for a {@code <method>} child tag.
        if (childTag.getShortName().equals(TAG_METHOD))
        {
            final FlexProject project = builder.getProject();
            final XMLName name = new XMLName(childTag.getXMLName().getXMLNamespace(), BASENAME_REMOTE_OBJECT_METHOD);
            final String qname = builder.getFileScope().resolveXMLNameToQualifiedName(name, builder.getMXMLDialect());
            final String remoteObjectMethodQName = project.getRemoteObjectMethodQName();
            if (qname != null && qname.equals(remoteObjectMethodQName))
            {
                final MXMLRemoteObjectMethodNode methodNode = new MXMLRemoteObjectMethodNode(this);
                methodNode.setClassReference(project, remoteObjectMethodQName);
                methodNode.initializeFromTag(builder, childTag);
                info.addChildNode(methodNode);
            }
        }
        else
        {
            super.processChildTag(builder, tag, childTag, info);
        }
    }
}
