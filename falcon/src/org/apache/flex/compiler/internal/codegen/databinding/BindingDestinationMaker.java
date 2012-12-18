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

package org.apache.flex.compiler.internal.codegen.databinding;

import static org.apache.flex.abc.ABCConstants.*;

import java.util.LinkedList;

import org.apache.flex.abc.instructionlist.InstructionList;
import org.apache.flex.abc.semantics.Name;
import org.apache.flex.compiler.internal.as.codegen.InstructionListNode;
import org.apache.flex.compiler.tree.as.IASNode;
import org.apache.flex.compiler.tree.as.IExpressionNode;
import org.apache.flex.compiler.tree.mxml.IMXMLDataBindingNode;
import org.apache.flex.compiler.tree.mxml.IMXMLModelNode;
import org.apache.flex.compiler.tree.mxml.IMXMLModelPropertyNode;
import org.apache.flex.compiler.tree.mxml.IMXMLModelRootNode;

/**
 * Utility class for analyze binding destinations and making
 * "destination functions" for them.
 * 
 * Note that a "destination function" is passed as an argument to the 
 * constructor of the SDK's mx.binding.Binding object.
 * 
 * A typical destination function looks like:
 *  function (_sourceFunctionReturnValue:*): void
 *  {
 *      some_complex_lvalue = _sourceFunctionReturnValue;
 *  }
 */
public class BindingDestinationMaker
{
    public static class Destination
    {
        public Destination(IExpressionNode destinationASTNode, boolean transformationRequired)
        {
            this.destinationASTNode = destinationASTNode;
            this.transformationRequired = transformationRequired;
        }
        public IExpressionNode destinationASTNode;
        boolean transformationRequired;
    }
    
    /**
     * Generate destination function from an IMXMLConcatenatedDataBindingNode 
     * or IMXMLDataBindingNode
     * 
     * Do this by walking up and down the tree, building up instruction list
     */
    public  static IExpressionNode makeDestinationFunctionInstructionList(IMXMLDataBindingNode dbnode)
    {   
        IExpressionNode ret = null;
        final IASNode parent = dbnode.getParent();
        
      
        // We only know how to make dest functions in very specific cases  
        // Case 1: we are in the <fx:Model> tag
        if (parent instanceof IMXMLModelPropertyNode)
        {
            // search up until we find the model tag
            // keep an ordered list of the nodes that will contribute to the instruction list
            LinkedList<IASNode > nodes = new LinkedList<IASNode >(); 
            IASNode tempNode = parent;

            for (boolean done=false; !done; )
            {
                if (tempNode instanceof IMXMLModelNode)
                {
                    // we found the root. terminate
                    done = true;  
                }
                
                // Save the node in our array, if we care about it
                if (!(tempNode instanceof IMXMLModelRootNode))
                {
                    nodes.addFirst(tempNode);
                }
                tempNode = tempNode.getParent();
                if (tempNode == null)
                {
                    assert false;   // no root. tree is mal-formed
                    return null;
                }
            }
            
            // Now we can generate the instructions by going through our node list in order,
            // which is like traversing the model tree from Model tag down to leaf
            InstructionList insns = new InstructionList();
            insns.addInstruction(OP_getlocal0);
   
            int nodeIndex = 0;
            for (IASNode n : nodes)
            {
                boolean last = nodeIndex == (nodes.size()-1);
                generateOne(insns, n, last);
                ++nodeIndex;
            }

           ret = new InstructionListNode(insns);    // Wrap the IL in a node and return it
        }
        return ret;   
    }
    
    /**
     * Generate the instructions for one level of the fx:Model tree
     * 
     */
    private static void generateOne(InstructionList insns, IASNode node, boolean lastOne)
    {
        if (node instanceof IMXMLModelPropertyNode)
            generateOneLevel(insns, (IMXMLModelPropertyNode)node, lastOne);
        else if (node instanceof IMXMLModelNode)
            generateModel(insns, (IMXMLModelNode)node, lastOne);
        else assert false;
    }
    
    private static void generateOneLevel(InstructionList insns, IMXMLModelPropertyNode node, boolean lastOne)
    { 
        if (!lastOne)
        {
            // get the next property in the destination chain
            insns.addInstruction(OP_getproperty, new Name(node.getName()));
            if (node.getIndex() >= 0)
            {
                // If it's an array, then get the correct element out of it
                String nodeIndexName = Integer.toString(node.getIndex());
                insns.addInstruction(OP_getproperty, new Name(nodeIndexName));
            }
        }
        else if (node.getIndex() < 0)
        {
            // last node, not array
            // get the passed in function parameter, and store it into
            // the final property in the model node chain
            insns.addInstruction(OP_getlocal1);    
            insns.addInstruction(OP_setproperty, new Name(node.getName()));
        }
        else
        {
            // last node, it's an array
            // get the array...
            insns.addInstruction(OP_getproperty, new Name(node.getName()));
            
            // get the passed in function parameter, and store it into
            // the correct element of the array
            insns.addInstruction(OP_getlocal1); 
            String nodeIndexName = Integer.toString(node.getIndex());
            insns.addInstruction(OP_setproperty, new Name(nodeIndexName));
        }
    }

    private static void generateModel(InstructionList insns, IMXMLModelNode model, boolean lastOne)
    {
        if (!lastOne)
        {
            String id = model.getEffectiveID();
            assert id != null;
            insns.addInstruction(OP_getproperty, new Name(id));
        }
        else
            assert false;// This would correspond to <Model><a>{}</a></Model>
                        // Since this is invalid, higher level code detects that
                        // and never calls us
    }
}
