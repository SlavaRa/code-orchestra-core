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

package org.apache.flex.compiler.tree.as;

import org.apache.flex.compiler.definitions.IDefinition;
import org.apache.flex.compiler.projects.ICompilerProject;

/**
 * An AST node representing a function call. It may be a constructor call.
 * <p>
 * The typical shape of this node is:
 * <pre>
 * IFunctionCallNode 
 *   IExpressionNode   <-- getNameNode()
 *   IContainerNode    <-- getArgumentsContainerNode()
 *     IExpressionNode <-- getArgumentNodes()[0]
 *     IExpressionNode <-- getArgumentNodes()[1]
 *     ...
 * </pre>
 * For example, <code>f(a, b)</code> is represented as
 * <pre>
 * IFunctionCallNode
 *   IIdentifierNode "f"
 *   IContainerNode
 *     IIdentifierNode "a"
 *     IIdentiferNode "b"
 * </pre>
 * For a constructor call, the first child represents the <code>new</code> keyword:
 * <pre>
 * IFunctionCallNode 
 *   IKeywordNode      <-- getNewKeywordNode()
 *   IExpressionNode   <-- getNameNode()
 *   IContainerNode    <-- getArgumentsContainerNode()
 *     IExpressionNode <-- getArgumentNodes()[0]
 *     IExpressionNode <-- getArgumentNodes()[1]
 *     ...
 * </pre>
 * For example, <code>new C(a, b)</code> is represented as
 * <pre>
 * IFunctionCallNode
 *   IKeywordNode "new"
 *   IIdentifierNode "C"
 *   IContainerNode
 *     IIdentifierNode "a"
 *     IIdentiferNode "b"
 * </pre>
 */
public interface IFunctionCallNode extends IExpressionNode
{
    /**
     * The expression being called.
     * 
     * @return the name of this function, as an {@link IExpressionNode}
     */
    IExpressionNode getNameNode();

    /**
     * Resolves the expression being called to a definition.
     * <p>
     * The result may or may not be an <code>IFunctionDefinition</code>.
     * <p>
     * For example, in <code>f()</code>, <code>f</code> might be
     * a variable of type <code>Function</code>, and so
     * this function would return an <code>IVariableDefinition</code>.
     * <p>
     * As another example, in <code>new Sprite()</code>, <code>Sprite</code>
     * resolves to a class, not to the constructor for that class, and so
     * this function would return an <code>IClassDefinition</code>.
     * 
     * @param project The {@link ICompilerProject} to use for lookups.
     * @return An {@link IDefinition} or <code>null</code>.
     */
    IDefinition resolveCalledExpression(ICompilerProject project);

    /**
     * The name of the function being called
     * 
     * @return the name of this function
     */
    String getFunctionName();

    /**
     * Returns an array of {@link IExpressionNode} nodes that are passed in as
     * parameters to this function call
     * 
     * @return an array of {@link IExpressionNode}s or an empty array
     */
    IExpressionNode[] getArgumentNodes();

    /**
     * Returns true if this function call is part of a new expression
     * <code> new String(); </code>
     * 
     * @return true if this is a new expression
     */
    boolean isNewExpression();
}
