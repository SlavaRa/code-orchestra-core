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
import java.util.EnumSet;
import java.util.List;

import org.apache.flex.compiler.common.SourceLocation;
import org.apache.flex.compiler.constants.IASLanguageConstants;
import org.apache.flex.compiler.definitions.IClassDefinition;
import org.apache.flex.compiler.definitions.IDefinition;
import org.apache.flex.compiler.definitions.ITypeDefinition;
import org.apache.flex.compiler.definitions.IVariableDefinition;
import org.apache.flex.compiler.internal.definitions.ClassDefinition;
import org.apache.flex.compiler.internal.mxml.MXMLDialect.TextParsingFlags;
import org.apache.flex.compiler.internal.parsing.ISourceFragment;
import org.apache.flex.compiler.internal.projects.FlexProject;
import org.apache.flex.compiler.internal.scopes.ASScope;
import org.apache.flex.compiler.internal.scopes.MXMLFileScope;
import org.apache.flex.compiler.internal.tree.as.NodeBase;
import org.apache.flex.compiler.mxml.MXMLTagAttributeData;
import org.apache.flex.compiler.mxml.MXMLTagData;
import org.apache.flex.compiler.mxml.MXMLTextData;
import org.apache.flex.compiler.mxml.MXMLUnitData;
import org.apache.flex.compiler.problems.ICompilerProblem;
import org.apache.flex.compiler.problems.MXMLUnresolvedTagProblem;
import org.apache.flex.compiler.tree.ASTNodeID;
import org.apache.flex.compiler.tree.as.IASNode;
import org.apache.flex.compiler.tree.mxml.IMXMLInstanceNode;
import org.apache.flex.compiler.tree.mxml.IMXMLPropertySpecifierNode;

/**
 * {@code MXMLEventSpecifierNode} represents an MXML event attribute or event
 * child tag.
 * <p>
 * Its child nodes are ActionScript nodes representing the statements to be
 * executed.
 * <p>
 * For example, the MXML code
 * 
 * <pre>
 * &lt;s:Button click="doThis(); doThat()"/&gt;
 * </pre>
 * 
 * or
 * 
 * <pre>
 * &lt;s:Button&gt;
 *     &lt;s:click&gt;
 *     &lt;![CDATA[
 *         doThis();
 *         doThat();
 *     ]]&gt;
 *     &lt;/s:click&gt;
 * &lt;/s:Button&gt;
 * </pre>
 * 
 * produces an AST of the form
 * 
 * <pre>
 * MXMLInstanceNode "spark.components.Button"
 *   MXMLEventSpecifierNode "click"
 *     FunctionCallNode
 *       IdentifierNode "doThis"
 *       ContainerNode
 *     FunctionCallNode
 *       IdentifierNode "doThat"
 *       ContainerNode
 * </pre>
 */
class MXMLPropertySpecifierNode extends MXMLSpecifierNodeBase implements IMXMLPropertySpecifierNode
{
    private static final EnumSet<TextParsingFlags> FLAGS = EnumSet.of(
            TextParsingFlags.ALLOW_ARRAY,
            TextParsingFlags.ALLOW_BINDING,
            TextParsingFlags.ALLOW_COLOR_NAME,
            TextParsingFlags.ALLOW_COMPILER_DIRECTIVE,
            TextParsingFlags.ALLOW_ESCAPED_COMPILER_DIRECTIVE,
            TextParsingFlags.ALLOW_PERCENT);

    /**
     * Constructor
     * 
     * @param parent The parent node of this node, or <code>null</code> if there
     * is no parent.
     */
    MXMLPropertySpecifierNode(NodeBase parent)
    {
        super(parent);
    }

    /**
     * The name of the property, if it is a dynamic property. Otherwise, this is
     * <code>null</code>.
     */
    private String dynamicName;

    /**
     * The sole child node, which is the value of the property.
     */
    private MXMLInstanceNode instanceNode;

    @Override
    public ASTNodeID getNodeID()
    {
        return ASTNodeID.MXMLPropertySpecifierID;
    }

    @Override
    public IASNode getChild(int i)
    {
        return i == 0 ? instanceNode : null;
    }

    @Override
    public int getChildCount()
    {
        return instanceNode != null ? 1 : 0;
    }

    @Override
    public IMXMLInstanceNode getInstanceNode()
    {
        return instanceNode;
    }

    void setInstanceNode(MXMLInstanceNode instanceNode)
    {
        this.instanceNode = instanceNode;
    }

    private ITypeDefinition getPropertyType(MXMLTreeBuilder builder)
    {
        IDefinition definition = getDefinition();

        FlexProject project = builder.getProject();

        // If there is no property definition, this is a dynamic property with type "*".
        if (definition == null)
            return (ITypeDefinition)project.getBuiltinType(IASLanguageConstants.BuiltinType.ANY_TYPE);

        return definition.resolveType(project);
    }

    protected String getPropertyTypeName(MXMLTreeBuilder builder)
    {
        ITypeDefinition propertyType = getPropertyType(builder);

        // If there is no property definition, this is a dynamic property with type "*".
        if (propertyType == null)
            return IASLanguageConstants.ANY_TYPE;

        return propertyType.getQualifiedName();
    }

    void setDynamicName(String dynamicName)
    {
        this.dynamicName = dynamicName;
    }

    @Override
    public String getName()
    {
        return dynamicName != null ? dynamicName : super.getName();
    }

    @Override
    protected MXMLNodeInfo createNodeInfo(MXMLTreeBuilder builder)
    {
        return new MXMLNodeInfo(builder);
    }

    /**
     * This override handles a property attribute like label="OK".
     */
    @Override
    protected void initializeFromAttribute(MXMLTreeBuilder builder,
                                           MXMLTagAttributeData attribute,
                                           MXMLNodeInfo info)
    {
        super.initializeFromAttribute(builder, attribute, info);

        // Break the attribute value into fragments.
        // Each entity is a separate fragment.
        Collection<ICompilerProblem> problems = builder.getProblems();
        ISourceFragment[] fragments = attribute.getValueFragments(problems);

        info.addSourceFragments(attribute.getSourcePath(), fragments);

        // parse out text and make correct kind of child node
        processFragments(builder, attribute, info);

        info.clearFragments();
    }

    private void processFragments(MXMLTreeBuilder builder,
                                  SourceLocation sourceLocation,
                                  MXMLNodeInfo info)
    {
        ITypeDefinition propertyType = getPropertyType(builder);

        ISourceFragment[] fragments = info.getSourceFragments();

        SourceLocation location = info.getSourceLocation();
        if (location == null)
            location = sourceLocation;

        MXMLClassDefinitionNode classNode =
                (MXMLClassDefinitionNode)getClassDefinitionNode();

        EnumSet<TextParsingFlags> flags = FLAGS;

        IDefinition definition = getDefinition();
        FlexProject project = builder.getProject();
        if (definition instanceof IVariableDefinition)
        {
            if (((IVariableDefinition)definition).hasCollapseWhiteSpace(project))
                flags.add(TextParsingFlags.COLLAPSE_WHITE_SPACE);
            if (((IVariableDefinition)definition).hasRichTextContent(project))
                flags.add(TextParsingFlags.RICH_TEXT_CONTENT);
        }

        instanceNode = builder.createInstanceNode(
                this, propertyType, fragments, location, flags, classNode);

        // If we don't have a value, we can't do codegen.
        if (instanceNode == null)
            markInvalidForCodeGen();

        // createInstanceNode() may have parsed a percentage value
        // for a property with [PercentProxy(...)] metadata.
        // In that case, this property node is actually for
        // a different property than appears in MXML,
        // such as width="100%" really meaning percentWidth="100",
        // so make this node refer the definition of the proxy property.
        IDefinition percentProxyDefinition = builder.getPercentProxyDefinition();
        if (percentProxyDefinition != null)
            setDefinition(percentProxyDefinition);
    }

    /**
     * This override handles text specifying a default property.
     */
    @Override
    protected void initializeFromText(MXMLTreeBuilder builder,
                                      MXMLTextData text,
                                      MXMLNodeInfo info)
    {
        super.initializeFromText(builder, text, info);

        // use helpers to set offsets on fragments and create child node of correct type
        accumulateTextFragments(builder, text, info);

        processFragments(builder, text, info);
    }

    void initializeDefaultProperty(MXMLTreeBuilder builder, IVariableDefinition defaultPropertyDefinition,
                                   List<MXMLUnitData> contentUnits)
    {
        FlexProject project = builder.getProject();

        assert (contentUnits.isEmpty()) ||
                (!builder.getFileScope().isScriptTag(contentUnits.get(0))) : "Script tags should not start a default property!";

        assert (contentUnits.isEmpty()) ||
                (!builder.getFileScope().isScriptTag(contentUnits.get(contentUnits.size() - 1))) : "Trailing script tags should be removed from default property content units!";

        // Set the location of the default property node
        // to span the tags that specify the default property value.
        setLocation(builder, contentUnits);

        String propertyTypeName = getPropertyTypeName(builder);

        // If the property is of type IDeferredInstance or ITransientDeferredInstance,
        // create an implicit MXMLDeferredInstanceNode.
        if (propertyTypeName.equals(project.getDeferredInstanceInterface()) ||
            propertyTypeName.equals(project.getTransientDeferredInstanceInterface()))
        {
            instanceNode = new MXMLDeferredInstanceNode(this);
            ((MXMLDeferredInstanceNode)instanceNode).initializeDefaultProperty(
                    builder, defaultPropertyDefinition, contentUnits);
        }
        else if (propertyTypeName.equals(IASLanguageConstants.Array))
        {
            // Create an implicit array node.
            instanceNode = new MXMLArrayNode(this);
            ((MXMLArrayNode)instanceNode).initializeDefaultProperty(
                    builder, defaultPropertyDefinition, contentUnits);
        }
        else if (contentUnits.size() == 1 && contentUnits.get(0) instanceof MXMLTagData)
        {
            MXMLTagData tag = (MXMLTagData)contentUnits.get(0);
            IDefinition definition = builder.getFileScope().resolveTagToDefinition(tag);
            if (definition instanceof ClassDefinition)
            {
                instanceNode = MXMLInstanceNode.createInstanceNode(
                        builder, definition.getQualifiedName(), this);
                instanceNode.setClassReference(project, (ClassDefinition)definition); // TODO Move this logic to initializeFromTag().
                instanceNode.initializeFromTag(builder, tag);
            }
        }
    }

    /**
     * This override handles a child tag in a property tag, such as
     * <label><String>OK</String></label>.
     */
    @Override
    protected void processChildTag(MXMLTreeBuilder builder, MXMLTagData tag,
                                   MXMLTagData childTag,
                                   MXMLNodeInfo info)
    {
        MXMLFileScope fileScope = builder.getFileScope();

        // Check whether the tag is an <fx:Component> tag.
        if (fileScope.isComponentTag(childTag))
        {
            instanceNode = new MXMLComponentNode(this);
            instanceNode.initializeFromTag(builder, childTag);
        }
        else
        {
            String propertyTypeName = getPropertyTypeName(builder);
            FlexProject project = builder.getProject();

            // If the property is of type IDeferredInstance or ITransientDeferredInstance,
            // create an implicit MXMLDeferredInstanceNode.
            if (propertyTypeName.equals(project.getDeferredInstanceInterface()) ||
                propertyTypeName.equals(project.getTransientDeferredInstanceInterface()))
            {
                instanceNode = new MXMLDeferredInstanceNode(this);
                instanceNode.initializeFromTag(builder, tag);
            }
            else
            {
                // Check whether the child tag is an instance tag
                IDefinition definition = builder.getFileScope().resolveTagToDefinition(childTag);
                if (definition instanceof ClassDefinition)
                {
                    instanceNode = MXMLInstanceNode.createInstanceNode(
                            builder, definition.getQualifiedName(), this);
                    instanceNode.setClassReference(project, (ClassDefinition)definition); // TODO Move this logic to initializeFromTag().
                    instanceNode.initializeFromTag(builder, childTag);
                }
                else
                {
                    ICompilerProblem problem = new MXMLUnresolvedTagProblem(childTag);
                    builder.addProblem(problem);
                }
            }
        }

        // Report problem for second instance tag or for any other kind of tag.
    }

    /**
     * This override is called on each non-whitespace unit of text inside a
     * property tag, such as <label>O<!-- comment -->K</label> which must
     * compile as if it had been written <label>OK</label>. All the text units
     * will be processed later in initializationComplete().
     */
    @Override
    protected void processChildNonWhitespaceUnit(MXMLTreeBuilder builder, MXMLTagData tag,
                                                 MXMLTextData text,
                                                 MXMLNodeInfo info)
    {
        accumulateTextFragments(builder, text, info);
    }

    /**
     * This override is called on a property tag such as <label>O<!-- comment
     * -->K</label>. It concatenates all the text units to get "OK" and uses
     * that to specify the property value.
     */
    @Override
    protected void initializationComplete(MXMLTreeBuilder builder, MXMLTagData tag,
                                          MXMLNodeInfo info)
    {
        super.initializationComplete(builder, tag, info);

        FlexProject project = builder.getProject();

        // If this property is type Array, and it didn't get set to an Array tag,
        // then create an implicit Array tag and initialize it from the
        // child tags of the property tag.
        IDefinition definition = getDefinition();
        if (definition != null && definition.getTypeAsDisplayString().equals(IASLanguageConstants.Array))
        {
            if (instanceNode == null ||
                !instanceNode.getClassReference(project).getQualifiedName().equals(IASLanguageConstants.Array))
            {
                instanceNode = new MXMLArrayNode(this);
                instanceNode.setClassReference(project, IASLanguageConstants.Array); // TODO Move to MXMLArrayNode
                ((MXMLArrayNode)instanceNode).initializeFromTag(builder, tag);
            }
        }

        if (instanceNode == null)
        {
            // use helpers for parse for bindings, @functions, create correct child node
            processFragments(builder, tag, info);
        }
    }

    @Override
    public IVariableDefinition getPercentProxyDefinition(FlexProject project)
    {
        // Get the name of the proxy property
        // from the [PercentProxy(...)] metadata.
        IVariableDefinition propertyDefinition = (IVariableDefinition)getDefinition();
        String percentProxy = propertyDefinition.getPercentProxy(project);
        if (percentProxy == null)
            return null;

        // Find the definition for the proxy property.
        ASScope classScope = (ASScope)propertyDefinition.getContainingScope();
        IClassDefinition classDefinition = (IClassDefinition)classScope.getDefinition();
        IDefinition proxyDefinition = classScope.getPropertyFromDef(
                project, classDefinition, percentProxy, false);
        if (!(proxyDefinition instanceof IVariableDefinition))
            return null;

        return (IVariableDefinition)proxyDefinition;
    }
}
