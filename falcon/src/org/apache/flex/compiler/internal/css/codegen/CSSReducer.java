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

package org.apache.flex.compiler.internal.css.codegen;

import static org.apache.flex.compiler.internal.as.codegen.ABCGeneratingReducer.pushNumericConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.flex.abc.ABCConstants;
import org.apache.flex.abc.instructionlist.InstructionList;
import org.apache.flex.abc.semantics.MethodBodyInfo;
import org.apache.flex.abc.semantics.MethodInfo;
import org.apache.flex.abc.semantics.Name;
import org.apache.flex.abc.visitors.IABCVisitor;
import org.apache.flex.abc.visitors.IMethodBodyVisitor;
import org.apache.flex.abc.visitors.IMethodVisitor;
import org.apache.flex.abc.visitors.ITraitsVisitor;
import org.apache.flex.compiler.constants.IASLanguageConstants;
import org.apache.flex.compiler.css.ICSSDocument;
import org.apache.flex.compiler.css.ICSSNode;
import org.apache.flex.compiler.css.ICSSProperty;
import org.apache.flex.compiler.css.ICSSPropertyValue;
import org.apache.flex.compiler.css.ICSSRule;
import org.apache.flex.compiler.css.ICSSSelector;
import org.apache.flex.compiler.css.ICSSSelectorCondition;
import org.apache.flex.compiler.definitions.references.IResolvedQualifiersReference;
import org.apache.flex.compiler.definitions.references.ReferenceFactory;
import org.apache.flex.compiler.internal.as.codegen.LexicalScope;
import org.apache.flex.compiler.internal.css.CSSArrayPropertyValue;
import org.apache.flex.compiler.internal.css.CSSColorPropertyValue;
import org.apache.flex.compiler.internal.css.CSSFunctionCallPropertyValue;
import org.apache.flex.compiler.internal.css.CSSKeywordPropertyValue;
import org.apache.flex.compiler.internal.css.CSSNumberPropertyValue;
import org.apache.flex.compiler.internal.css.CSSRule;
import org.apache.flex.compiler.internal.css.CSSSelector;
import org.apache.flex.compiler.internal.css.CSSStringPropertyValue;
import org.apache.flex.compiler.internal.css.codegen.Pair.InstructionListAndClosure;
import org.apache.flex.compiler.internal.css.codegen.Pair.InstructionListAndString;
import org.apache.flex.compiler.internal.css.codegen.Pair.PairOfInstructionLists;
import org.apache.flex.compiler.internal.css.semantics.CSSSemanticAnalyzer;
import org.apache.flex.compiler.internal.units.EmbedCompilationUnit;
import org.apache.flex.compiler.problems.CSSCodeGenProblem;
import org.apache.flex.compiler.problems.ICompilerProblem;
import org.apache.flex.compiler.projects.IFlexProject;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * This reducer generates a valid CSS file from a CSS model tree. The target
 * code has two main parts - an array of selector data and an object literal
 * with function closures to set the style properties.
 */
public class CSSReducer implements ICSSCodeGenResult
{

    /**
     * The {@code global} CSS selector.
     */
    private static final String GLOBAL_SELECTOR = "global";

    /**
     * AET name for {@code var inheiritingStyles:String}.
     */
    private static final Name NAME_INHERITING_STYLES = new Name("inheritingStyles");

    /**
     * AET name for {@code var data:Array}.
     */
    public static final Name NAME_DATA_ARRAY = new Name("data");

    /**
     * ABC {@code Name} for<br>
     * <code>public static var factoryFunctions:Object = generateFactoryFunctions();</code>
     */
    public static final Name NAME_FACTORY_FUNCTIONS = new Name("factoryFunctions");

    /**
     * Parameter types for a method without any parameters.
     */
    private static final Vector<Name> EMPTY_PARAM_TYPES = new Vector<Name>();

    /**
     * Create a CSS reducer.
     * 
     * @param project Owner project.
     * @param cssDocument CSS DOM tree.
     * @param abcVisitor {@link IABCVisitor} to which generated ABC constructs
     * are added.
     * @param session CSS compilation session data.
     * @param isDefaultFactory If true, the generated code will register the
     * styles with {@link ICSSRuntimeConstants#DEFAULT_FACTORY}.
     */
    public CSSReducer(final IFlexProject project,
                      final ICSSDocument cssDocument,
                      final IABCVisitor abcVisitor,
                      final CSSCompilationSession session,
                      final boolean isDefaultFactory)
    {
        assert project != null : "Expected a Flex project.";
        assert cssDocument != null : "Expected a CSS model.";
        assert abcVisitor != null : "Expected an ABC visitor.";
        assert session != null : "Expected a CSSCompilationSession.";

        this.problems = new HashSet<ICompilerProblem>();
        this.session = session;
        this.resolvedSelectors = ImmutableMap.copyOf(session.resolvedSelectors);
        this.abcVisitor = abcVisitor;
        this.project = project;
        if (isDefaultFactory)
            this.factory = ICSSRuntimeConstants.DEFAULT_FACTORY;
        else
            this.factory = ICSSRuntimeConstants.FACTORY;
    }

    /**
     * Stores CSS semantic analysis results.
     */
    private final CSSCompilationSession session;

    /**
     * CSS code generation problems.
     */
    private final Set<ICompilerProblem> problems;

    /**
     * A dictionary for "selector" to "class definition".
     */
    private final ImmutableMap<ICSSSelector, String> resolvedSelectors;

    /**
     * Populate the target ABC instructions by using this visitor.
     */
    private final IABCVisitor abcVisitor;

    /**
     * Instructions for the class init method of a generated style's class.
     */
    private InstructionList cinitInstructionList;

    /**
     * Owner project.
     */
    private final IFlexProject project;

    /**
     * The "factory" with which the styles will be registered.
     */
    private final Integer factory;

    /**
     * Root reduction rule. It aggregates all the instructions and emit ABC code
     * of {@code StyleDateClass}.
     * 
     * @param site {@link ICSSDocument} node.
     * @param namespaceList Instructions to create array and closure for
     * namespace declarations.
     * @param ruleList Instructions to create array and closure for rules.
     * @return Instructions to create array and closure for the CSS document.
     */
    public PairOfInstructionLists reduceDocument(ICSSNode site, PairOfInstructionLists namespaceList, PairOfInstructionLists ruleList)
    {
        // Instructions to push an array object on the stack.
        final int elementSize = ruleList.arrayReduction.getInstructions().size();
        final InstructionList arrayInstructions = new InstructionList();
        arrayInstructions.addAll(ruleList.arrayReduction);
        arrayInstructions.addInstruction(ABCConstants.OP_newarray, elementSize);

        final PairOfInstructionLists pair = new PairOfInstructionLists(arrayInstructions, ruleList.closureReduction);
        generateABC(pair);

        return pair;
    }

    /**
     * Generate ABC instructions for both array and closure.
     * 
     * @param pair Instructions to create array and closure for the
     * {@code StyleDataClass}.
     */
    private void generateABC(final PairOfInstructionLists pair)
    {
        assert cinitInstructionList == null : "generateABC should only be called once per reducer because each document should only be reduced once.";
        cinitInstructionList = new InstructionList();

        // Generate instructions for "StyleDataClass$cinit()".
        final InstructionList initializeFactoryFunctions = cinitInstructionList;

        // Initialize "factoryFunctions".
        initializeFactoryFunctions.addInstruction(ABCConstants.OP_getlocal0);
        initializeFactoryFunctions.addAll(pair.closureReduction);
        initializeFactoryFunctions.addInstruction(ABCConstants.OP_initproperty, NAME_FACTORY_FUNCTIONS);

        // Initialize "data".
        initializeFactoryFunctions.addInstruction(ABCConstants.OP_getlocal0);
        initializeFactoryFunctions.addAll(pair.arrayReduction);
        initializeFactoryFunctions.addInstruction(ABCConstants.OP_initproperty, NAME_DATA_ARRAY);

        // Initialize "inheritingStyles".
        final String inheritingStylesText =
                Joiner.on(",").skipNulls().join(session.inheritingStyles);
        initializeFactoryFunctions.addInstruction(ABCConstants.OP_getlocal0);
        initializeFactoryFunctions.addInstruction(ABCConstants.OP_pushstring, inheritingStylesText);
        initializeFactoryFunctions.addInstruction(ABCConstants.OP_initproperty, NAME_INHERITING_STYLES);
    }

    @Override
    public InstructionList getClassInitializationInstructions()
    {
        assert cinitInstructionList != null : "The initialize instructions may not be accessed until the document reduction has executed";
        return cinitInstructionList;
    }

    @Override
    public void visitClassTraits(ITraitsVisitor classTraitsVisitor)
    {

        // Resolve "Object" type.
        final IResolvedQualifiersReference referenceObject = ReferenceFactory.packageQualifiedReference(project.getWorkspace(), "Object");

        // Resolve "String" type.
        final IResolvedQualifiersReference referenceString = ReferenceFactory.packageQualifiedReference(project.getWorkspace(), "String");

        // Resolve "Array" type.
        final IResolvedQualifiersReference referenceArray = ReferenceFactory.packageQualifiedReference(project.getWorkspace(), "Array");

        // Generate "public static var factoryFunctions:Object;"
        classTraitsVisitor.visitSlotTrait(
                ABCConstants.TRAIT_Const,
                NAME_FACTORY_FUNCTIONS,
                ITraitsVisitor.RUNTIME_SLOT,
                referenceObject.getMName(),
                LexicalScope.noInitializer);

        // Generate "public static var data:Array;"
        classTraitsVisitor.visitSlotTrait(
                ABCConstants.TRAIT_Const,
                NAME_DATA_ARRAY,
                ITraitsVisitor.RUNTIME_SLOT,
                referenceArray.getMName(),
                LexicalScope.noInitializer);

        // Generate "public static var inheritingStyles:String"
        classTraitsVisitor.visitSlotTrait(
                ABCConstants.TRAIT_Const,
                NAME_INHERITING_STYLES,
                ITraitsVisitor.RUNTIME_SLOT,
                referenceString.getMName(),
                LexicalScope.noInitializer);
    }

    /**
     * Namespace node does not generate code.
     */
    public PairOfInstructionLists reduceNamespaceDefinition(ICSSNode site)
    {
        return null;
    }

    /**
     * Namespace list node does not generate code.
     */
    public PairOfInstructionLists reduceNamespaceList(ICSSNode site, List<PairOfInstructionLists> namespaces)
    {
        return null;
    }

    /**
     * Reduce a CSS property.
     * <p>
     * For example: {@code fontSize : 12; } will be translated into
     * ActionScript:<br>
     * <code>this.fontSize = 12;</code><br>
     * The ABC instructions for this statement are:<br>
     * 
     * <pre>
     * getlocal0
     * pushint 12
     * setproperty fontSize
     * </pre>
     * 
     * {@code getlocal0} will put "this" on the stack, then the property value
     * is put on the stack. Finally, {@code setproperty} will assign "12" to
     * "fontSize".
     * <p>
     * The code generation is based on the assumption that the CSS DOM tree has
     * been semantically checked, so that the validity of the property name and
     * type is not re-checked in the BURM.
     */
    public PairOfInstructionLists reduceProperty(ICSSNode site)
    {
        assert site instanceof ICSSProperty : "Expected ICSSProperty node but got " + site.getClass().getName();
        final ICSSProperty propertyNode = (ICSSProperty)site;
        final String name = propertyNode.getName();
        final ICSSPropertyValue value = propertyNode.getValue();
        final InstructionList inst = new InstructionList();

        final InstructionList valueInstructions = getInstructionListForPropertyValue(value);
        if (!valueInstructions.isEmpty())
        {
            // push "this" on the stack
            inst.addInstruction(ABCConstants.OP_getlocal0);
            // push value on the stack
            inst.addAll(valueInstructions);
            // set the property value
            inst.addInstruction(ABCConstants.OP_setproperty, new Name(name));
        }

        return new PairOfInstructionLists(new InstructionList(), inst);
    }

    /**
     * Get the AET instructions that can push values for the property on the AVM
     * stack.
     * 
     * @param value CSS property value.
     * @return AET instructions.
     */
    private InstructionList getInstructionListForPropertyValue(final ICSSPropertyValue value)
    {
        final InstructionList valueInstructions = new InstructionList();
        // push property value on the stack
        if (value instanceof CSSStringPropertyValue)
        {
            valueInstructions.addInstruction(ABCConstants.OP_pushstring, ((CSSStringPropertyValue)value).getValue());
        }
        else if (value instanceof CSSColorPropertyValue)
        {
            valueInstructions.addInstruction(ABCConstants.OP_pushint, new Integer(((CSSColorPropertyValue)value).getColorAsInt()));
        }
        else if (value instanceof CSSKeywordPropertyValue)
        {
            CSSKeywordPropertyValue keywordValue = (CSSKeywordPropertyValue)value;
            String keywordString = keywordValue.getKeyword();
            if (IASLanguageConstants.TRUE.equals(keywordString))
                valueInstructions.addInstruction(ABCConstants.OP_pushtrue);
            else if (IASLanguageConstants.FALSE.equals(keywordString))
                valueInstructions.addInstruction(ABCConstants.OP_pushfalse);
            else
                valueInstructions.addInstruction(ABCConstants.OP_pushstring, ((CSSKeywordPropertyValue)value).getKeyword());
        }
        else if (value instanceof CSSNumberPropertyValue)
        {
            valueInstructions.addInstruction(ABCConstants.OP_pushdouble, new Double(((CSSNumberPropertyValue)value).getNumber().doubleValue()));
        }
        else if (value instanceof CSSFunctionCallPropertyValue)
        {
            final CSSFunctionCallPropertyValue functionCall = (CSSFunctionCallPropertyValue)value;
            if ("ClassReference".equals(functionCall.name))
            {
                final String className = CSSFunctionCallPropertyValue.getSingleArgumentFromRaw(functionCall.rawArguments);
                if ("null".equals(className))
                {
                    // ClassReference(null) resets the property's class reference.
                    valueInstructions.addInstruction(ABCConstants.OP_pushnull);
                }
                else
                {
                    final IResolvedQualifiersReference reference = ReferenceFactory.packageQualifiedReference(project.getWorkspace(), className);
                    valueInstructions.addInstruction(ABCConstants.OP_getlex, reference.getMName());
                }
            }
            else if ("PropertyReference".equals(functionCall.name))
            {
                // TODO: implement me
            }
            else if ("Embed".equals(functionCall.name))
            {
                final EmbedCompilationUnit embedCompilationUnit = session.resolvedEmbedProperties.get(functionCall);
                if (embedCompilationUnit == null)
                {
                    final ICompilerProblem e = new CSSCodeGenProblem(
                            new IllegalStateException("Unable to find compilation unit for " + functionCall));
                    problems.add(e);
                }
                else
                {
                    final String qName = embedCompilationUnit.getName();
                    final IResolvedQualifiersReference reference = ReferenceFactory.packageQualifiedReference(project.getWorkspace(), qName);
                    valueInstructions.addInstruction(ABCConstants.OP_getlex, reference.getMName());
                }
            }
            else
            {
                assert false : "CSS parser bug: unexpected function call property value: " + functionCall;
                throw new IllegalStateException("Unexpected function call property value: " + functionCall);
            }
        }
        else if (value instanceof CSSArrayPropertyValue)
        {
            final CSSArrayPropertyValue arrayValue = (CSSArrayPropertyValue)value;
            for (final ICSSPropertyValue elementValue : arrayValue.getElements())
            {
                valueInstructions.addAll(getInstructionListForPropertyValue(elementValue));
            }
            valueInstructions.addInstruction(ABCConstants.OP_newarray, arrayValue.getElements().size());
        }
        else
        {
            assert false : "Unsupported property value: " + value;
        }
        return valueInstructions;
    }

    /**
     * Reduce a property list node. This method aggregates all the instructions
     * to set property values. It also add instructions to setup the stack frame
     * for this function closure.
     */
    public PairOfInstructionLists reducePropertyList(ICSSNode site, List<PairOfInstructionLists> properties)
    {
        final InstructionList closureInstructions = new InstructionList();
        for (final PairOfInstructionLists inst : properties)
            closureInstructions.addAll(inst.closureReduction);

        closureInstructions.addInstruction(ABCConstants.OP_returnvoid);
        return new PairOfInstructionLists(null, closureInstructions);
    }

    /**
     * Reduce CSS rule node. The ABC method for the closures are generated here
     * by merging the instructions from {@code propertyList} into the
     * {@code selector}'s closure map.
     * 
     * @param site {@link ICSSRule} node.
     * @param selector Instructions to construct the array data, and a map of
     * named closures.
     * @param propertyList Instructions to create array and closure that set the
     * properties.
     * @return Instructions for array data and a map of closures.
     */
    public InstructionListAndClosure reduceRule(ICSSNode site, InstructionListAndClosure selector, PairOfInstructionLists propertyList)
    {
        // Generate anonymous function.
        final MethodInfo methodInfo = new MethodInfo();
        methodInfo.setMethodName(((CSSRule)site).getSelectorGroup().get(0).getElementName());
        methodInfo.setParamTypes(EMPTY_PARAM_TYPES);
        final IMethodVisitor methodVisitor = abcVisitor.visitMethod(methodInfo);
        methodVisitor.visit();

        // Generate method body.
        final MethodBodyInfo methodBodyInfo = new MethodBodyInfo();
        methodBodyInfo.setMethodInfo(methodInfo);
        final IMethodBodyVisitor methodBodyVisitor = methodVisitor.visitBody(methodBodyInfo);
        methodBodyVisitor.visit();
        methodBodyVisitor.visitInstructionList(propertyList.closureReduction);
        methodBodyVisitor.visitEnd();

        // Finish anonymous function.
        methodVisitor.visitEnd();

        // Populate the closure name-body map with method info objects.
        for (final String name : selector.closureReduction.keySet())
        {
            selector.closureReduction.put(name, methodInfo);
        }

        return selector;
    }

    /**
     * Reduce a "selector" node. The node can have zero or more ascendant
     * selectors.
     */
    public InstructionListAndString reduceSelector(ICSSNode site)
    {
        assert site instanceof ICSSSelector : "Expected a 'selector' node, but got '" + site.getClass().getName() + "'.";

        final InstructionList arrayInstructions = new InstructionList();
        final List<String> resolvedSimpleSelectorNames = new ArrayList<String>();
        final ICSSSelector selectorNode = (ICSSSelector)site;
        final ImmutableList<ICSSSelector> selectors = CSSSelector.getCombinedSelectorList(selectorNode);
        for (final ICSSSelector selector : selectors)
        {
            final String selectorLiteral = getSelecterLiteralForABC(selector);

            // Generate array data for conditional selector.
            for (final ICSSSelectorCondition condition : selector.getConditions())
            {
                pushNumericConstant(ICSSRuntimeConstants.CONDITION, arrayInstructions);
                arrayInstructions.addInstruction(ABCConstants.OP_pushstring, condition.getConditionType().name().toLowerCase());
                arrayInstructions.addInstruction(ABCConstants.OP_pushstring, condition.getValue());
            }

            // Generate array data for type selector.
            pushNumericConstant(ICSSRuntimeConstants.SELECTOR, arrayInstructions);
            arrayInstructions.addInstruction(ABCConstants.OP_pushstring, selectorLiteral);

            // Collect resolved name for the simple selector in the combined selectors.
            // For example: "spark.components.Button#loginButton", ".highlight#main:up"
            final String resolvedSelectorName = selectorLiteral.concat(Joiner.on("").join(selector.getConditions()));
            resolvedSimpleSelectorNames.add(resolvedSelectorName);
        }

        final String combinedSelectors = Joiner.on(" ").join(resolvedSimpleSelectorNames);
        return new InstructionListAndString(arrayInstructions, combinedSelectors);
    }

    /**
     * Convert from CSS type names to ActionScript QNames.
     * 
     * @param selector CSS selector.
     * @return String value of the selector name that works with Flex CSS
     * runtime.
     */
    private String getSelecterLiteralForABC(final ICSSSelector selector)
    {
        final String selectorQname;
        if (project.getCSSManager().isFlex3CSS())
        {
            assert !selector.isAdvanced() : "Advanced selector is not supported in Flex 3 mode. " + selector;
            // Flex 3 style CSS are emitted "as-is".
            final String elementName = selector.getElementName();
            selectorQname = elementName == null ? "" : elementName;
        }
        else if (CSSSemanticAnalyzer.isWildcardSelector(selector))
        {
            // Selectors without specified types are normalized to have wildcard type "*".
            // From SDK 4.5.2 and up, the CSS runtime queries such selectors by Qname "*".
            if (GLOBAL_SELECTOR.equals(selector.getElementName()))
                selectorQname = GLOBAL_SELECTOR;
            else
                selectorQname = "";
        }
        else
        {
            final String qname = resolvedSelectors.get(selector);
            assert qname != null : "Unable to resolve type selector: " + selector;
            selectorQname = qname;
        }
        return selectorQname;
    }

    /**
     * Reduce rule list node.
     */
    public PairOfInstructionLists reduceRuleList(ICSSNode site, List<InstructionListAndClosure> rules)
    {
        final InstructionList arrayInstructions = new InstructionList();
        final InstructionList closureInstructions = new InstructionList();
        int closureCount = 0;
        for (final InstructionListAndClosure ruleReduction : rules)
        {
            arrayInstructions.addAll(ruleReduction.arrayReduction);

            for (final Entry<String, MethodInfo> entry : ruleReduction.closureReduction.entrySet())
            {
                closureInstructions.addInstruction(ABCConstants.OP_pushstring, entry.getKey());
                closureInstructions.addInstruction(ABCConstants.OP_newfunction, entry.getValue());
                closureCount++;
            }
        }
        closureInstructions.addInstruction(ABCConstants.OP_newobject, closureCount);

        return new PairOfInstructionLists(arrayInstructions, closureInstructions);
    }

    /**
     * Reduce instructions for selector group.
     * <p>
     * The array reduction is instruction list. The closure reduction is a map.
     * The keys in the map are the resolved selector names that will be used as
     * names for the generated closures. The values in the map are all null.
     * They will be populated in the parent tree, because the {@link MethodInfo}
     * for the closure bodies come from a sibling "properties" tree.
     */
    public InstructionListAndClosure reduceSelectorGroup(ICSSNode site, List<InstructionListAndString> selectors)
    {
        final InstructionList arrayInstructions = new InstructionList();
        final Map<String, MethodInfo> closureNames = new HashMap<String, MethodInfo>();
        for (final InstructionListAndString selectorReduction : selectors)
        {
            arrayInstructions.addAll(selectorReduction.arrayReduction);
            closureNames.put(selectorReduction.closureReduction, null);
        }
        pushNumericConstant(ICSSRuntimeConstants.STYLE_DECLARATION, arrayInstructions);
        pushNumericConstant(factory, arrayInstructions);

        return new InstructionListAndClosure(arrayInstructions, closureNames);
    }

    public PairOfInstructionLists reduceFontFaceList(ICSSNode site, List<PairOfInstructionLists> fontFaces)
    {
        // TODO Implement @font-face code generation
        return null;
    }

    public PairOfInstructionLists reduceFontFace(ICSSNode site)
    {
        // TODO Implement @font-face code generation
        return null;
    }

    public PairOfInstructionLists reduceMediaQuery(ICSSNode site, List<PairOfInstructionLists> conditions)
    {
        // TODO Implement @media code generation
        return null;
    }

    public PairOfInstructionLists reduceMediaQueryCondition(ICSSNode site)
    {
        // TODO Implement @media code generation
        return null;
    }

    /**
     * @return CSS reduction problems.
     */
    public Set<ICompilerProblem> getProblems()
    {
        return problems;
    }
}
