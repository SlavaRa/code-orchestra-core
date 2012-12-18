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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.flex.compiler.definitions.IClassDefinition;
import org.apache.flex.compiler.definitions.IDefinition;
import org.apache.flex.compiler.definitions.IEffectDefinition;
import org.apache.flex.compiler.definitions.IEventDefinition;
import org.apache.flex.compiler.definitions.ISetterDefinition;
import org.apache.flex.compiler.definitions.IStyleDefinition;
import org.apache.flex.compiler.definitions.IVariableDefinition;
import org.apache.flex.compiler.internal.definitions.ClassDefinition;
import org.apache.flex.compiler.internal.mxml.MXMLDialect;
import org.apache.flex.compiler.internal.projects.FlexProject;
import org.apache.flex.compiler.internal.scopes.ASProjectScope;
import org.apache.flex.compiler.internal.tree.as.NodeBase;
import org.apache.flex.compiler.mxml.MXMLTagAttributeData;
import org.apache.flex.compiler.mxml.MXMLTagData;
import org.apache.flex.compiler.mxml.MXMLTextData;
import org.apache.flex.compiler.mxml.MXMLUnitData;
import org.apache.flex.compiler.problems.ICompilerProblem;
import org.apache.flex.compiler.problems.MXMLDuplicateChildTagProblem;
import org.apache.flex.compiler.projects.ICompilerProject;
import org.apache.flex.compiler.tree.as.IASNode;
import org.apache.flex.compiler.tree.mxml.IMXMLClassReferenceNode;
import org.apache.flex.compiler.tree.mxml.IMXMLEventSpecifierNode;
import org.apache.flex.compiler.tree.mxml.IMXMLNode;
import org.apache.flex.compiler.tree.mxml.IMXMLPropertySpecifierNode;
import org.apache.flex.compiler.tree.mxml.IMXMLSpecifierNode;

/**
 * {@code MXMLClassReferenceNodeBase} is the abstract base class for AST nodes
 * that represent MXML tags which map to ActionScript classes
 * (either as instances of those classes
 * or as definitions of subclasses of those classes).
 */
abstract class MXMLClassReferenceNodeBase extends MXMLNodeBase implements IMXMLClassReferenceNode
{
    /**
     * Constructor
     * 
     * @param parent The parent node of this node, or <code>null</code> if there
     * is no parent.
     */
    MXMLClassReferenceNodeBase(NodeBase parent)
    {
        super(parent);
    }

    /**
     * The class definition to which this node refers. For example,
     * <code>&lt;s:Button&gt;</code> typically refers to the class definition
     * for <code>spark.components.Button</code>. An {@code MXMLInstanceNode}
     * creates an instance of this class, while an
     * {@code MXMLClassDefinitionNode} declares a subclass of this class.
     */
    private IClassDefinition classReference;

    /**
     * A flag that keeps track of whether the node represents a class that
     * implements mx.core.IMXML.
     */
    private boolean isMXMLObject = false;

    /**
     * A flag that keeps track of whether the node represents an MX container
     * (i.e., an mx.core.IContainer).
     */
    private boolean isContainer = false;

    /**
     * A flag that keeps track of whether the node represents a visual element
     * container (i.e., an mx.core.IVisualElementContainer).
     */
    private boolean isVisualElementContainer = false;

    /**
     * A flag that keeps track of whether the node represents a UIComponent
     * supporting deferred instantiation.
     */
    private boolean isDeferredInstantiationUIComponent = false;

    /**
     * The child nodes of this node. For {@code MXMLInstanceNode} the children
     * will all be property/event/style specifiers. For
     * {@code MXMLClassDefinitionNode} the children may include other nodes such
     * as {@MXMLMetadataNode}, {@MXMLScriptNode
     * }, etc. If there are no children, this will be null.
     */
    private IMXMLNode[] children;

    /**
     * A map of the child nodes of this node which specify properties. The keys
     * are the property names. If there are no properties specified, this will
     * be null.
     */
    private Map<String, IMXMLPropertySpecifierNode> propertyNodeMap;

    /**
     * A map of child nodes of this node which specify events. The keys are the
     * event names. If there are no events specified, this will be null.
     */
    private Map<String, IMXMLEventSpecifierNode> eventNodeMap;

    /**
     * A map of suffix (specifying a state or state group) to the child nodes
     * with this suffix.
     */
    private Map<String, Collection<IMXMLSpecifierNode>> suffixSpecifierMap;

    /**
     * The definition of the default property. This gets lazily initialized by
     * {@code getDefaultPropertyDefinition()} if we need to know it.
     */
    private IVariableDefinition defaultPropertyDefinition;

    /**
     * A flag that keeps track of whether the {@code defaultPropertyDefinition}
     * field has been initialized. Simply checking whether it is
     * <code>null</code> doesn't work, because <code>null</code> means
     * "no default property" rather than "default property not determined yet".
     */
    private boolean defaultPropertyDefinitionInitialized = false;

    /**
     * A flag that keeps track of whether we are processing a content unit for
     * the default property. For example, you can have MXML like
     * 
     * <pre>
     * &lt;Application&gt;
     *     &lt;width&gt;100&lt;/width&gt;
     *     &lt;Button/&gt;
     *     &lt;Button/&gt;
     *     &lt;height&gt;100&lt;/height&gt;
     * &lt;/Application&gt;
     * </pre>
     * 
     * where the two <code>Button</code> tags specify an implicit array for the
     * <code>mxmlContentFactory</code> property. This flag is set true on the
     * first <code>Button</code> tag and then set back to false on the
     * <code>height</code> tag.
     */
    private boolean processingDefaultProperty = false;

    /**
     * A flag that keeps track of whether we have complete the processing of the
     * content units for the default property, so that we don't process
     * non-contiguous units.
     */
    private boolean processedDefaultProperty = false;

    /**
     * The implicit node created to represent the default property.
     */
    private MXMLPropertySpecifierNode defaultPropertyNode;

    /**
     * A list that accumulates content units for the default property.
     */
    private List<MXMLUnitData> defaultPropertyContentUnits;

    @Override
    public IASNode getChild(int i)
    {
        return children != null ? children[i] : null;
    }

    @Override
    public int getChildCount()
    {
        return children != null ? children.length : 0;
    }

    @Override
    public String getName()
    {
        // The classReference can be null when getName() is called from toString()
        // in the debugger if the node is not yet fully initialized.
        return classReference != null ? classReference.getQualifiedName() : "";
    }

    @Override
    public IClassDefinition getClassReference(ICompilerProject project)
    {
        return classReference;
    }

    @Override
    public boolean isMXMLObject()
    {
        return isMXMLObject;
    }

    @Override
    public boolean isContainer()
    {
        return isContainer;
    }

    @Override
    public boolean isVisualElementContainer()
    {
        return isVisualElementContainer;
    }

    @Override
    public boolean isDeferredInstantiationUIComponent()
    {
        return isDeferredInstantiationUIComponent;
    }

    /**
     * Sets the definition of the ActionScript class to which this node refers.
     */
    void setClassReference(FlexProject project, IClassDefinition classReference)
    {
        this.classReference = classReference;

        // TODO Optimize this by enumerating all interfaces one time.

        // Keep track of whether the class implements mx.core.IMXML,
        // because that affects code generation.
        String mxmlObjectInterface = project.getMXMLObjectInterface();
        isMXMLObject = classReference.isInstanceOf(mxmlObjectInterface, project);

        // Keep track of whether the class implements mx.core.IVisualElementContainer,
        // because that affects code generation.
        String visualElementContainerInterface = project.getVisualElementContainerInterface();
        isVisualElementContainer = classReference.isInstanceOf(visualElementContainerInterface, project);

        // Keep track of whether the class implements mx.core.IContainer,
        // because that affects code generation.
        String containerInterface = project.getContainerInterface();
        isContainer = classReference.isInstanceOf(containerInterface, project);

        // Keep track of whether the class implements mx.core.IDeferredInstantiationUIComponent
        // because that affects code generation.
        String deferredInstantiationUIComponentInterface = project.getDeferredInstantiationUIComponentInterface();
        isDeferredInstantiationUIComponent = classReference.isInstanceOf(deferredInstantiationUIComponentInterface, project);
    }

    /**
     * Sets the definition of the ActionScript class to which this node refers,
     * from its fully qualified name.
     * 
     * @param project An {@code ICompilerProject}, used for finding the class by
     * name.
     * @param qname A fully qualified class name.
     */
    void setClassReference(FlexProject project, String qname)
    {
        ASProjectScope projectScope = (ASProjectScope)project.getScope();
        IDefinition definition = projectScope.findDefinitionByName(qname);
        // TODO This method is getting called by MXML tree-building
        // with an interface qname if there is a property whose type is an interface.
        // Until databinding is implemented, we need to protect against this.
        if (definition instanceof IClassDefinition)
            setClassReference(project, (IClassDefinition)definition);
    }

    /**
     * Sets the child nodes of this node.
     * 
     * @param children An array of {@code IMXMLNode} objects.
     */
    void setChildren(IMXMLNode[] children)
    {
        this.children = children;

        if (children != null)
        {
            for (IMXMLNode child : children)
            {
                if (child instanceof IMXMLPropertySpecifierNode)
                {
                    if (propertyNodeMap == null)
                        propertyNodeMap = new HashMap<String, IMXMLPropertySpecifierNode>();

                    propertyNodeMap.put(child.getName(), (IMXMLPropertySpecifierNode)child);
                }
                else if (child instanceof IMXMLEventSpecifierNode)
                {
                    if (eventNodeMap == null)
                        eventNodeMap = new HashMap<String, IMXMLEventSpecifierNode>();

                    eventNodeMap.put(child.getName(), (IMXMLEventSpecifierNode)child);
                }

                if (child instanceof IMXMLSpecifierNode)
                {
                    if (suffixSpecifierMap == null)
                        suffixSpecifierMap = new HashMap<String, Collection<IMXMLSpecifierNode>>();

                    //                    suffixSpecifierMap.put(((IMXMLSpecifierNode)child).getSuffix(),
                    //                                           (IMXMLSpecifierNode)child);
                }
            }
        }
    }

    @Override
    public IMXMLPropertySpecifierNode getPropertySpecifierNode(String name)
    {
        return propertyNodeMap != null ? propertyNodeMap.get(name) : null;
    }

    @Override
    public IMXMLPropertySpecifierNode[] getPropertySpecifierNodes()
    {
        return propertyNodeMap != null ?
                propertyNodeMap.values().toArray(new IMXMLPropertySpecifierNode[0]) :
                null;
    }

    @Override
    public IMXMLEventSpecifierNode getEventSpecifierNode(String name)
    {
        return eventNodeMap != null ? eventNodeMap.get(name) : null;
    }

    @Override
    public IMXMLEventSpecifierNode[] getEventSpecifierNodes()
    {
        return eventNodeMap != null ?
                eventNodeMap.values().toArray(new IMXMLEventSpecifierNode[0]) :
                null;
    }

    @Override
    public IMXMLSpecifierNode[] getSpecifierNodesWithSuffix(String suffix)
    {
        return suffixSpecifierMap != null ?
                suffixSpecifierMap.get(suffix).toArray(new IMXMLSpecifierNode[0]) :
                null;
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

        MXMLSpecifierNodeBase childNode = createSpecifierNode(builder, attribute.getName());
        if (childNode != null)
        {
            childNode.setLocation(attribute);
            childNode.setSuffix(builder, attribute.getStateName());
            childNode.initializeFromAttribute(builder, attribute, info);
            info.addChildNode(childNode);
        }
        else
        {
            super.processTagSpecificAttribute(builder, tag, attribute, info);
        }
    }

    @Override
    protected void processChildTag(MXMLTreeBuilder builder, MXMLTagData tag,
                                   MXMLTagData childTag,
                                   MXMLNodeInfo info)
    {
        if (info.hasSpecifierWithName(childTag.getShortName(), childTag.getStateName()))
        {
            ICompilerProblem problem = new MXMLDuplicateChildTagProblem(childTag);
            builder.addProblem(problem);
            return ;
        }
        
        FlexProject project = builder.getProject();

        // Handle child tags that are property/style/event specifiers.
        MXMLSpecifierNodeBase childNode = createSpecifierNode(builder, childTag.getShortName());
        if (childNode != null)
        {
            // This tag is not part of the default property value.
            processNonDefaultPropertyContentUnit(builder, info);

            childNode.setSuffix(builder, childTag.getStateName());
            childNode.initializeFromTag(builder, childTag);
            info.addChildNode(childNode);
        }
        else if (builder.getFileScope().isScriptTag(childTag) &&
                 (builder.getMXMLDialect().isEqualToOrBefore(MXMLDialect.MXML_2009)))
        {
            // In MXML 2006 and 2009, allow a <Script> tag
            // inside any class reference tag

            if (!processingDefaultProperty)
            {
                // Not processing the default property, just make a script
                // node and put it in the tree.
                MXMLScriptNode scriptNode = new MXMLScriptNode(this);
                scriptNode.initializeFromTag(builder, childTag);
                info.addChildNode(scriptNode);
            }
            else
            {
                // We are processing a default property.  Script nodes need
                // to be a child of that default specifier nodes so that
                // finding a node by offset works properly.
                // See: http://bugs.adobe.com/jira/browse/CMP-955
                processDefaultPropertyContentUnit(builder, childTag, info);
            }

        }
        else if (builder.getFileScope().isReparentTag(childTag))
        {
            MXMLReparentNode reparentNode = new MXMLReparentNode(this);
            reparentNode.initializeFromTag(builder, childTag);
            info.addChildNode(reparentNode);
        }
        else
        {
            IDefinition definition = builder.getFileScope().resolveTagToDefinition(childTag);
            if (definition instanceof ClassDefinition)
            {
                // Handle child tags that are instance tags.

                IVariableDefinition defaultPropertyDefinition = getDefaultPropertyDefinition(builder);
                if (defaultPropertyDefinition != null && !processedDefaultProperty)
                {
                    // Since there is a default property and we haven't already processed it,
                    // assume this child instance tag is part of its value.
                    processDefaultPropertyContentUnit(builder, childTag, info);
                }
                else
                {
                    // This tag is not part of the default property value.
                    processNonDefaultPropertyContentUnit(builder, info);

                    MXMLInstanceNode instanceNode = MXMLInstanceNode.createInstanceNode(
                            builder, definition.getQualifiedName(), this);
                    instanceNode.setClassReference(project, (IClassDefinition)definition); // TODO Move this logic to initializeFromTag().
                    instanceNode.initializeFromTag(builder, childTag);
                    info.addChildNode(instanceNode);
                }
            }
            else
            {
                // Handle child tags that are something other than property/style/event tags
                // or instance tags.

                // This tag is not part of the default property value.
                processNonDefaultPropertyContentUnit(builder, info);

                super.processChildTag(builder, tag, childTag, info);
            }
        }
    }

    /**
     * Determines, and caches, the default property for the class to which this
     * node refers.
     */
    private IVariableDefinition getDefaultPropertyDefinition(MXMLTreeBuilder builder)
    {
        if (!defaultPropertyDefinitionInitialized)
        {
            FlexProject project = builder.getProject();
            String defaultPropertyName = classReference.getDefaultPropertyName(project);
            if (defaultPropertyName != null)
            {
                defaultPropertyDefinition =
                        (IVariableDefinition)project.resolveSpecifier(classReference, defaultPropertyName);
            }

            defaultPropertyDefinitionInitialized = true;
        }

        return defaultPropertyDefinition;
    }

    /**
     * Called on each content unit that is part of the default value.
     */
    private void processDefaultPropertyContentUnit(MXMLTreeBuilder builder,
                                                   MXMLTagData childTag,
                                                   MXMLNodeInfo info)
    {
        // If this gets called and we're not already processing the default property,
        // then childTag is the first child tag of the default property value.
        if (!processingDefaultProperty)
        {
            processingDefaultProperty = true;

            String defaultPropertyName = getDefaultPropertyDefinition(builder).getBaseName();

            // Create an implicit MXMLPropertySpecifierNode for the default property,
            // at the correct location in the child list.
            defaultPropertyNode =
                    (MXMLPropertySpecifierNode)createSpecifierNode(builder, defaultPropertyName);
            info.addChildNode(defaultPropertyNode);

            // Create a list in which we'll accumulate the tags for the default property.
            defaultPropertyContentUnits = new ArrayList<MXMLUnitData>(1);
        }

        defaultPropertyContentUnits.add(childTag);
    }

    /**
     * Called on each content unit that is not part of the default value.
     */
    private void processNonDefaultPropertyContentUnit(MXMLTreeBuilder builder, MXMLNodeInfo info)
    {
        // If this gets called and we're processing the default property,
        // then childTag is the first child tag after the default property value tags.
        if (processingDefaultProperty)
        {
            processingDefaultProperty = false;
            processedDefaultProperty = true;

            assert defaultPropertyContentUnits.size() > 0;
            assert !builder.getFileScope().isScriptTag(defaultPropertyContentUnits.get(0)) : "First default property content unit must not be a script tag!";
            // We've accumulated all the default property child tags
            // in defaultPropertyChildTags. Use them to initialize
            // the defaultPropertyNode.

            // But first find all the trailing script tags
            // and remove those from the list of default
            // property content units.
            // Script tags are put in the defaultPropertyContentUnits collection
            // to fix http://bugs.adobe.com/jira/browse/CMP-955.
            int lastNonScriptTagIndex;
            for (lastNonScriptTagIndex = (defaultPropertyContentUnits.size() - 1); lastNonScriptTagIndex > 0; --lastNonScriptTagIndex)
            {
                MXMLUnitData unitData = defaultPropertyContentUnits.get(lastNonScriptTagIndex);
                if (!builder.getFileScope().isScriptTag(unitData))
                    break;
            }
            assert lastNonScriptTagIndex >= 0;
            assert lastNonScriptTagIndex < defaultPropertyContentUnits.size();

            List<MXMLUnitData> trailingScriptTags = defaultPropertyContentUnits.subList(lastNonScriptTagIndex + 1, defaultPropertyContentUnits.size());
            List<MXMLUnitData> defaultPropertyContentUnitsWithoutTrailingScriptTags =
                    defaultPropertyContentUnits.subList(0, lastNonScriptTagIndex + 1);

            // process the default property content units with the trailing
            // script tags removed.
            IVariableDefinition defaultPropertyDefinition =
                    getDefaultPropertyDefinition(builder);
            defaultPropertyNode.initializeDefaultProperty(
                    builder, defaultPropertyDefinition, defaultPropertyContentUnitsWithoutTrailingScriptTags);

            // Now create MXMLScriptNode's for all the trailing script tags.
            for (MXMLUnitData scriptTagData : trailingScriptTags)
            {
                assert builder.getFileScope().isScriptTag(scriptTagData);
                MXMLScriptNode scriptNode = new MXMLScriptNode(this);
                scriptNode.initializeFromTag(builder, (MXMLTagData)scriptTagData);
                info.addChildNode(scriptNode);
            }
        }
    }

    @Override
    protected void processChildNonWhitespaceUnit(MXMLTreeBuilder builder, MXMLTagData tag,
                                                 MXMLTextData text,
                                                 MXMLNodeInfo info)
    {
        // Non-whitespace may be the value of a default property.
        IVariableDefinition defaultPropertyDefinition = getDefaultPropertyDefinition(builder);
        if (defaultPropertyDefinition != null)
        {
            MXMLSpecifierNodeBase childNode =
                    createSpecifierNode(builder, defaultPropertyDefinition.getBaseName());
            if (childNode != null)
            {
                childNode.initializeFromText(builder, text, info);
                info.addChildNode(childNode);
            }
        }
        else
        {
            super.processChildNonWhitespaceUnit(builder, tag, text, info);
        }
    }

    /**
     * Resolve the specifier name in the class definition to a member
     * definition, and create a specifier node based on the member type.
     * 
     * @param builder MXML tree builder.
     * @param specifierName Specifier name.
     * @return A MXML specifier node.
     */
    protected MXMLSpecifierNodeBase createSpecifierNode(MXMLTreeBuilder builder, String specifierName)
    {
        MXMLSpecifierNodeBase specifierNode = null;

        // Check if the attribute is a declared property, style, or event.
        FlexProject project = builder.getProject();
        IDefinition specifierDefinition = project.resolveSpecifier(classReference, specifierName);

        if (specifierDefinition instanceof ISetterDefinition ||
            specifierDefinition instanceof IVariableDefinition)
        {
            specifierNode = new MXMLPropertySpecifierNode(this);
        }
        else if (specifierDefinition instanceof IEventDefinition)
        {
            specifierNode = new MXMLEventSpecifierNode(this);
        }
        else if (specifierDefinition instanceof IStyleDefinition)
        {
            specifierNode = new MXMLStyleSpecifierNode(this);
        }
        else if (specifierDefinition instanceof IEffectDefinition)
        {
            specifierNode = new MXMLEffectSpecifierNode(this);
        }

        if (specifierNode != null)
        {
            specifierNode.setDefinition(specifierDefinition); // TODO Move this logic
        }

        // If not, dynamic classes allow new properties to be set via attributes.
        else if (classReference.isDynamic())
        {
            specifierNode = new MXMLPropertySpecifierNode(this);
            ((MXMLPropertySpecifierNode)specifierNode).setDynamicName(specifierName); // TODO Move this logic
        }

        return specifierNode;
    }

    @Override
    protected void initializationComplete(MXMLTreeBuilder builder, MXMLTagData tag,
                                          MXMLNodeInfo info)
    {
        super.initializationComplete(builder, tag, info);

        // If the last child unit was part of the default property,
        // we don't know to process the default property units
        // until we get here.
        processNonDefaultPropertyContentUnit(builder, info);

        setChildren(info.getChildNodeList().toArray(new IMXMLNode[0]));

        // If the class references by this node implements mx.core.IContainer,
        // add an expression dependency on mx.core.UIComponentDescriptor
        // because we'll have to codegen descriptors.
        if (isContainer)
        {
            FlexProject project = builder.getProject();
            builder.addExpressionDependency(project.getUIComponentDescriptorClass());
        }
    }

    /**
     * For debugging only. Builds a string such as
     * <code>"spark.components.Application"</code> from the qualified name of
     * the class reference by the node.
     */
    @Override
    protected boolean buildInnerString(StringBuilder sb)
    {
        sb.append('"');
        sb.append(getName());
        sb.append('"');

        return true;
    }
}
