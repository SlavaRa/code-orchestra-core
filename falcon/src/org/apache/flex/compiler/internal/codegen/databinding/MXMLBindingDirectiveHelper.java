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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


import org.apache.flex.abc.instructionlist.InstructionList;
import org.apache.flex.abc.semantics.MethodInfo;
import org.apache.flex.abc.semantics.Name;
import org.apache.flex.abc.visitors.IABCVisitor;
import org.apache.flex.compiler.common.DependencyType;
import org.apache.flex.compiler.constants.IASLanguageConstants;
import org.apache.flex.compiler.definitions.IDefinition;
import org.apache.flex.compiler.definitions.references.IResolvedQualifiersReference;
import org.apache.flex.compiler.definitions.references.ReferenceFactory;
import org.apache.flex.compiler.internal.as.codegen.MXMLClassDirectiveProcessor;
import org.apache.flex.compiler.internal.codegen.databinding.WatcherInfoBase.WatcherType;
import org.apache.flex.compiler.internal.projects.FlexProject;
import org.apache.flex.compiler.internal.scopes.ASScope;
import org.apache.flex.compiler.mxml.IMXMLTypeConstants;
import org.apache.flex.compiler.tree.mxml.IMXMLBindingNode;
import org.apache.flex.compiler.tree.mxml.IMXMLDataBindingNode;
import org.apache.flex.compiler.workspaces.IWorkspace;

/**
 * Keeps track of all the Data Bindings in an MXML file and helps with codegen 
 * for data binding. 
 * 
 * This class is closely linked with MXMLDocumentDirectiveProcessor - they both call each
 * other and could be merged. They are separate just to manage size and complexity.
 * 
 * TODO: 
 *      Document the runtime dependencies on SDK
 *      add problem reporting.
 *      
 * Cases not yet working:
 *      bind to function, xml, xml list, array
 *      
 * Improve code gen
 *      don't make getter functions when not needed
 *      share getters when possible
 *      
 *      
 * known bugs:
 *      Generated traits not in mx_internal
 *      
 */
public class MXMLBindingDirectiveHelper
{
    // Some AET names we use locally
    private static final Name NAME_OBJTYPE = new Name(IASLanguageConstants.Object);
    private static final Name NAME_ARRAYTYPE = new Name(IASLanguageConstants.Array);
 
    
    public MXMLBindingDirectiveHelper(MXMLClassDirectiveProcessor ddp, IABCVisitor emitter)
    {
        host = ddp;
        this.emitter = emitter;
    }
    
    // -------------------- private fields ------------------------------------------------
    
    private final MXMLClassDirectiveProcessor host;
    
    private final IABCVisitor emitter;
    
    // This helper will do all the analysis of the binding expressions
    private BindingDatabase bindingDataBase = new BindingDatabase();
    
    // If we generate a propertyGetter for use by PropertyWatchers, we
    // store it here. It will remain null if we don't need one.
    private MethodInfo propertyGetter = null;
    
    
    
    //-------------------------- public methods -------------------------------------
    
    /**
     * host should visit all the databinding nodes before trying to codegen them
     */
    public void visitNode(IMXMLDataBindingNode node)
    { 
        // analyze the node for later CG
       bindingDataBase.analyze(node, host.getProblems(), host);

    }

    /**
     * Visit an IMXMLBindingNode - this node has explicit source and destination expressions
     * instead of the destination being determined by the nodes location in the AST, as in the IMXMLInstanceNode
     * version above
     * @param node  the Binding Node to vist
     */
    public void visitNode( IMXMLBindingNode node )
    {
        bindingDataBase.analyzeBindingNode(node, host.getProblems(), host);
    }

    /**
     * get the instruction list for code to be added to constructor
     * to set up data binding. This is the main entry point for codegen of
     * databindings
     * 
     * This function must be called only once, after then entire document has been traversed.
     * @return the {@link InstructionList} the instruction list for code to be added to constructor
     */
    public InstructionList getConstructorCode()
    {
        if (bindingDataBase.getBindingInfo().isEmpty())
            return null;
        
        if (!establishSDKDependencies())
            return null;
       
        bindingDataBase.finishAnalysis();
        
        // Please leave this in here - it is a very commonly used diagnostic
        // Just comment it out before checking
        //System.out.println("db: " + bindingDataBase);
        
        InstructionList ret = new InstructionList();
        
        makeSpecialMemberVariablesForBinding();
        
        makePropertyGetterIfNeeded();
        ret.addAll(makeBindingsAndGetters());
        
        // now nothing on the stack, _bindings has the array of bindings
        ret.addAll(makeAllWatchers());
        
        ret.addAll(BindingCodeGenUtils.fireInitialBindings());
        
        return ret;
    }
    
    /**
     * Generates all the Binding objects, and and "getter" functions required to implement them
     * puts Binding into this._bindings
     */
    private InstructionList makeBindingsAndGetters()
    {
        InstructionList insns = new InstructionList();
        log(insns, "makeBindingsAndGetters");
        
        // TODO: do we really want to push these all onto the stack, then make them into an array?
        // we could make the array first, then add then all later.
        for (BindingInfo bindingInfo : bindingDataBase.getBindingInfo())
        {
            makeBindingAndGetter(insns, bindingInfo);
        }
        
        // stack : binding.n, binding.n-1..., binding.0
        
        // now turn bindings into array
        insns.addInstruction(OP_newarray, bindingDataBase.getBindingInfo().size());
        // stack : bindings[]
        
        // now save array to _bindings property
        insns.addInstruction(OP_getlocal0);
        // stack : this, bindings
        insns.addInstruction(OP_swap);
        // stack : bindings, this
        insns.addInstruction(OP_setproperty, IMXMLTypeConstants.NAME_BINDINGS);
        // stack : <empty>
        
        
        // Now that we have saved the _bindings array, do any needed
        // two way binding hook up.
        linkTwoWayCounterparts(insns);
       
        log(insns, "leave makebindingsArray");
        return insns;
    }
    
    /**
     * Goes through the BindingInfo's looking for two way pairs that need to be hooked up.
     * Generate the code to hook them up
     */
    private void linkTwoWayCounterparts(InstructionList insns)
    {
        Map<Integer, Integer> pairs = bindingDataBase.getTwoWayBindingInfoPairs();
        Set<Entry<Integer, Integer>> entries = pairs.entrySet();
        for (Entry<Integer, Integer> pair : entries)
        {
            setTwoWayCounterpart(insns, pair.getKey(), pair.getValue());        // one way...
            setTwoWayCounterpart(insns, pair.getValue(), pair.getKey());        // ... then the other
        }
    }
    
    /** 
     * Generate code for _bindings[dest].twoWayCounterpart = _bindings[src];
     */
    private void setTwoWayCounterpart(InstructionList insns, int dest, int src)
    {
        assert dest >= 0;
        assert src >= 0;
        assert src != dest;
  
     
        insns.addInstruction(OP_getlocal0);
        // stack: this
        insns.addInstruction(OP_getproperty, IMXMLTypeConstants.NAME_BINDINGS);
        // stack: this.bindings
        
        insns.pushNumericConstant(dest);
        // stack: this.bindings, dest

        insns.addInstruction(OP_getproperty,  IMXMLTypeConstants.NAME_ARRAYINDEXPROP); 
        // stack: bindings[dest]

         
        insns.addInstruction(OP_getlocal0);
        // stack: ..., this
        insns.addInstruction(OP_getproperty, IMXMLTypeConstants.NAME_BINDINGS);
        // stack: ..., this.bindings
        
        insns.pushNumericConstant(src);
        // stack: ...,  this.bindings, src
      
        insns.addInstruction(OP_getproperty,  IMXMLTypeConstants.NAME_ARRAYINDEXPROP); 
        // stack: bindings[dest], bindings[src];
      
        insns.addInstruction(OP_setproperty, IMXMLTypeConstants.NAME_TWOWAYCOUNTERPART);
        // stack: <empty>
    }

    /**
     * Generate the getter for a single binding object, then create the binding object
     * using the getter as a ctor argument.
     * 
     * On entry: nothing special
     * On exit : binding(n) on stack
     */
    private void makeBindingAndGetter(InstructionList insns, BindingInfo bindingInfo)
    {
        log(insns, "makeBindingAndGetter");

        // IL to hold code for putting the source and dest funcs on the stack,
        // we just add them in at the right place to avoid OP_swaps
        InstructionList methodIsns = new InstructionList();
        // Step 1: make getter and leave on stack
        if (bindingInfo.isSourceSimplePublicProperty())
        {
            // Special case - we don't need a getter for the simplest case.
            methodIsns.addInstruction(OP_pushnull);
        }
        else
        {
            BindingCodeGenUtils.generateGetter(
                    emitter,
                    methodIsns,
                    bindingInfo.getExpressionNodesForGetter(),
                    host.getInstanceScope());
        }
       // stack : getter

        if(bindingInfo.getExpressionNodeForDestination() != null )
        {
            BindingCodeGenUtils.generateSetter(
                    methodIsns,
                    bindingInfo.getExpressionNodeForDestination(),
                    host.getInstanceScope()
                    );

        }
        else
        {
            methodIsns.addInstruction(OP_pushnull);
        }
        // Step 2: make binding
       String destStr = bindingInfo.getDestinationString();
      
       String srcStr = bindingInfo.getSourceString();
       BindingCodeGenUtils.makeBinding(insns, host.getProject(), destStr, srcStr, methodIsns);
    }
    
    /** Since we use SDK classes, we need to establish the correct dependency
     * so that they classes get linked in.
     * 
     * @return true if we succeed
     */
    private boolean establishSDKDependencies()
    {
        // List of SDK classed we depend on
        // TODO: generated complete and minimal list dynamically.
        FlexProject project = host.getProject();
        
        String[] depends = {
          project.getBindingClass(),
          project.getPropertyWatcherClass(),
          project.getStaticPropertyWatcherClass(),
          project.getFunctionReturnWatcherClass(),
          project.getXMLWatcherClass(),
          project.getBindingManagerClass()
        };
        
        ASScope scope = bindingDataBase.getScope();
        IWorkspace workspace = project.getWorkspace();
      
     
        for (String depend : depends)
        {
             IResolvedQualifiersReference ref = ReferenceFactory.packageQualifiedReference(workspace, depend);
             if (ref==null)
             {
                 // TODO: generate compiler problem if we can't resolve these
                 assert false;
                 return false;
             }
             IDefinition def = ref.resolve(project, scope, DependencyType.EXPRESSION, false);
             if (def==null)
             {
                 // TODO: generate compiler problem if we can't resolve these
                 // CMP-886
                 assert false;
                 return false;
             }
        }
        return true;   
    }
    
    
    /**
     * Creates all the watchers we need for the class we are CG'ing, setup of their parent child relationships,
     * and saves them in this._watchers
     * 
     * @return
     * 
     * VM Context:
     *      during: local1=_bindings
     *              local2=_watchers
     */
    
    private InstructionList makeAllWatchers()
    {
        InstructionList insns = new InstructionList();
        
        log(insns, "makeWatchers");
        if (BindingCodeGenUtils.doLog) log("db: " + bindingDataBase);
        
        allocateNullWatchersArray(insns, bindingDataBase.getNumWatchers());
        
        // set up the locals
        insns.addInstruction(OP_getlocal0);                     // stack: this
        insns.addInstruction(OP_dup);                           // this, this
        insns.addInstruction(OP_getproperty, IMXMLTypeConstants.NAME_BINDINGS);    // stack: _bindings, this
        insns.addInstruction(OP_setlocal1);                     // local1 = _bindings
                                                                // stack: this
        insns.addInstruction(OP_getproperty, IMXMLTypeConstants.NAME_WATCHERS);    // stack: _watchers
        insns.addInstruction(OP_setlocal2);                     // local2 = _watchers
                                                             
        Set<Entry<Object, WatcherInfoBase>> watcherChains = bindingDataBase.getWatcherChains();
        if (watcherChains != null)
        {
            for (Entry<Object, WatcherInfoBase> ent : watcherChains)
              {
                  makeWatcherChain(insns, ent.getValue());
              }   
        }

        log(insns, "leave makeWatchersArray");
        return insns;  
    }
    
    

    /**
     * Generate ABC to allocate an array full of nulls, and assign to this._watchers
     */
    private void allocateNullWatchersArray(InstructionList insns, int numWatchers)
    {
        log(insns, "allocateNullWatchersArray");
        for (int i=0; i<numWatchers; ++i)
        {
            // TODO: should we emit a loop? this may use up too much stack!!
            insns.addInstruction(OP_pushnull);
        }
        
        insns.addInstruction(OP_newarray, numWatchers);
        // stack : watchers[]
        
        // now save array to _watchers propery
        insns.addInstruction(OP_getlocal0);
        // stack : this, watchers
        insns.addInstruction(OP_swap);
        // stack : watchers, this
        insns.addInstruction(OP_setproperty, IMXMLTypeConstants.NAME_WATCHERS);
        // stack : <empty>   
    }

    /**
     * Does code-gen for:
     *      Instantate all of the watchers in the watcher chain
     *      Save references to created watchers in _watchers
     *      Calls any necessary initialization code on the created watchers, including:
     *          any necessary updateParent call
     *          any necessary addChild calls
     * @param insns
     * @param watcherChain
     */
    private void makeWatcherChain(InstructionList insns, WatcherInfoBase watcherChain)
    {
        log(insns, "** makeWatcherChain " + watcherChain.getIndex());
        
        if (watcherChain.getChildren() == null && watcherChain.getEventNames().isEmpty())
        {
            // TODO: we already made an array slot for this - probably shouldn't have
            // also - what it the real algorithm here? don't we sometimes make these guys?
            log("not making watcher for non-bindable");
            
            if (watcherChain.getType() != WatcherType.FUNCTION)
                return;
        }
        
        // first populate the _watcher array with the ones in this chain
        makeWatcherAndChildren(insns, watcherChain);
        
        // then call update parent on and add child as needed
        watcherUpdateParent(insns, watcherChain);
        watcherAddChildren(insns, watcherChain);
    }

    /**
     * generates all of the watcher.addChild() called needed for a chain of watchers,
     * and any other parent/child code
     * 
     * typically this will look like:
     *          watchers[0].addChild( watchers[1] );
     *          watchers[1].addchild( watchers[2] );
     */
    private void watcherAddChildren(InstructionList insns, WatcherInfoBase watcher)
    {
        Set<Entry<Object, WatcherInfoBase>> children = watcher.getChildren();
        if (children == null) return;
        
        for (Entry<Object, WatcherInfoBase> child : children)
        {
            watcherAddChild(insns, watcher, child.getValue());     // set up children for this watcher
            watcherAddChildren(insns, child.getValue());
        }
    }

    /* VM Context:
     *      entry: local1=_bindings
     *              local2=_watchers
     *              
     *      during:
     *            local3= parentWatcher
     *            local4=childWatcher
     */   
    private void watcherAddChild(InstructionList insns, WatcherInfoBase parent, WatcherInfoBase child)
    {
        log(insns, "add child. parent=" + parent.getIndex() + " child=" + child.getIndex());
        // push parent onto stack
        
        //------------ First, find the parent and child and put into local registers -----
        //
        insns.addInstruction(OP_getlocal2);     // stack: _watchers  
        insns.pushNumericConstant(parent.getIndex());
        // stack: parent_index, _watchers
       
        insns.addInstruction(OP_getproperty,  IMXMLTypeConstants.NAME_ARRAYINDEXPROP); 
        // stack: _watchers[ParentIndex]
        
        insns.addInstruction(OP_setlocal3);
        // stack empty, local3 = parent
        
        // push child onto stack
        insns.addInstruction(OP_getlocal2);     // stack: _watchers  
        insns.pushNumericConstant(child.getIndex());
        // stack: child_index, _watchers
        
        insns.addInstruction(OP_getproperty,  IMXMLTypeConstants.NAME_ARRAYINDEXPROP); 
        // stack: _watchers[child_index]
        
        insns.addInstruction(OP_setlocal, 4);
        // stack empty, local4 = child
    
        //------------------------------------------------------------
        // FunctionRetrunWatcher has a special field "parentWatcher" that must
        // be set to the parent
        //
        if (child.getType() == WatcherType.FUNCTION)
        {
            insns.addInstruction(OP_getlocal, 4);
            // stack: child 
            insns.addInstruction(OP_getlocal3);
            // stack: parent, child
            insns.addInstruction(OP_setproperty, IMXMLTypeConstants.NAME_PARENTWATCHER); 
        }
        
        //------------------ make the addChild call
        //
        insns.addInstruction(OP_getlocal3);
        // stack: parent 
        insns.addInstruction(OP_getlocal, 4);
        // stack: child, parent
        // call parent.addChild(child)
        insns.addInstruction(OP_callpropvoid, IMXMLTypeConstants.ARG_ADDCHILD); 
    }

    /* VM Context:
    *      entry: local1=_bindings
    *              local2=_watchers
    */             
    private void watcherUpdateParent(InstructionList insns, WatcherInfoBase watcherInfo)
    {
        assert(watcherInfo.getIndex() >= 0 && watcherInfo.getIndex() < bindingDataBase.getNumWatchers());
       
        //---------------------------------------------------
        // first,  push watcher [n]
        
        insns.addInstruction(OP_getlocal2);     // stack: _watchers  
        insns.pushNumericConstant(watcherInfo.getIndex());
        // stack: index, _watchers
        // stack: watcher_index
        insns.addInstruction(OP_getproperty,  IMXMLTypeConstants.NAME_ARRAYINDEXPROP); // stack: _watchers[index]
        
        switch (watcherInfo.getType())
        {
            case PROPERTY:
            case FUNCTION:
                // for regular property watcher, will will do updateParent(this)
                insns.addInstruction(OP_getlocal0);
                break;
            case STATIC_PROPERTY:
            {
                // for static watcher, we pass the class object of the class that contains the static
                StaticPropertyWatcherInfo pwinfo = (StaticPropertyWatcherInfo)watcherInfo;
                Name classMName = pwinfo.getContainingClass(host.getProject());
                insns.addInstruction(OP_getlex, classMName);
            }
                break;
            default:
                assert false;
        }
        //--------------------------------------------------------
        // next: call watcher.updateparent (thing)
      
        // stack : thing, watcher
        insns.addInstruction(OP_callpropvoid, IMXMLTypeConstants.ARG_UPDATEPARENT); 
    }

    /** Generates the abc for the watcher, assigns to _watchers entry, and does the same for its
     * children by calling self recursively
     * 
     * @param insns
     * @param watcherInfo
     */
    private  void makeWatcherAndChildren(InstructionList insns, WatcherInfoBase watcherInfo)
    {
        log(insns, "makeWatchers for " + watcherInfo.getIndex());
        makeWatcher(insns, watcherInfo);        // make this one
        
        // then recurse into children
        Set<Entry<Object, WatcherInfoBase>> children = watcherInfo.getChildren();
        if (children != null)
        {
            for ( Entry<Object, WatcherInfoBase> ent : children)
            {
                makeWatcherAndChildren(insns, ent.getValue());
            }
        }
    }
    
    
    /**
     * Generates code to create single watcher,and assign the watcher to the _watchers array
     * 
     * VM Context:
     *      on entry: local1=_bindings
     *              local2=_watchers
     */
    private  void makeWatcher(InstructionList insns, WatcherInfoBase watcherInfo)
    {
        log(insns, "makeWatcher slot " + watcherInfo.getIndex());
        
        insns.addInstruction(OP_getlocal2);         // stack: _watchers
        insns.pushNumericConstant(watcherInfo.getIndex());
        WatcherType type = watcherInfo.getType();
                                                    // stack: index, _watchers      
        if (type == WatcherType.FUNCTION)
        {
            FunctionWatcherInfo functionWatcherInfo = (FunctionWatcherInfo)watcherInfo;
           
            BindingCodeGenUtils.makeFunctionWatcher(
                    insns,
                    host.getProject(),
                    emitter,
                    functionWatcherInfo.getFunctionName(),
                    functionWatcherInfo.getEventNames(),
                    functionWatcherInfo.getBindings());
        }
        else if ((type == WatcherType.STATIC_PROPERTY) || (type == WatcherType.PROPERTY))
        {
            PropertyWatcherInfo propertyWatcherInfo = (PropertyWatcherInfo)watcherInfo;
           
            boolean makeStaticWatcher = (watcherInfo.getType() == WatcherType.STATIC_PROPERTY);
            
            // round up the getter function for the watcher, or null if we don't need one
            MethodInfo propertyGetterFunction = null;
            if (watcherInfo.isRoot && !makeStaticWatcher)
            {
                propertyGetterFunction = this.propertyGetter;
                assert propertyGetterFunction != null;
            }
            else if (watcherInfo.isRoot && makeStaticWatcher)
            {
                 // TOTO: implement getter func for static watcher.
            }
            BindingCodeGenUtils.makePropertyWatcher(
                    makeStaticWatcher,
                    insns, 
                    propertyWatcherInfo.getPropertyName(),
                    propertyWatcherInfo.getEventNames(), 
                    propertyWatcherInfo.getBindings(),
                    propertyGetterFunction,
                    host.getProject());
        }
        else if (type == WatcherType.XML)
        {
            XMLWatcherInfo xmlWatcherInfo = (XMLWatcherInfo)watcherInfo;
            BindingCodeGenUtils.makeXMLWatcher(
                    insns,
                    xmlWatcherInfo.getPropertyName(),
                    xmlWatcherInfo.getBindings(),
                    host.getProject()
                    );
        }
        else assert false;     
        bindingDataBase._watchersCreated++;         // note one more created. 
        
        // now save the newly created watcher back to the _watchers array
        // stack : new_watcher, index, _watcher
        insns.addInstruction(OP_setproperty,  IMXMLTypeConstants.NAME_ARRAYINDEXPROP); 
    }
  
    /** SDK BindingsManager contract forces us to have some names member variables
     * 
     */
    private void makeSpecialMemberVariablesForBinding()
    {
        // TODO: make these mx.internal
       
        host.addVariableTrait(IMXMLTypeConstants.NAME_BINDINGSBYDESTINATION, NAME_OBJTYPE);
        host.addVariableTrait(IMXMLTypeConstants.NAME_BINDINGSBEGINWITHWORD, NAME_OBJTYPE);
        
        host.addVariableTrait(IMXMLTypeConstants.NAME_WATCHERS, NAME_ARRAYTYPE);
        host.addVariableTrait(IMXMLTypeConstants.NAME_BINDINGS, NAME_ARRAYTYPE);

        // We aren't initiaizing any of these. SDK seems to do if for us, so why bother?
    }
    
    private void makePropertyGetterIfNeeded()
    {
        assert propertyGetter == null;
        if (bindingDataBase.getRequiresPropertyGetter())
        {
            propertyGetter = BindingCodeGenUtils.generatePropertyGetterFunction(
                    emitter,
                    host.getProject(),
                    bindingDataBase.getScope()
                    );
        }
    }

    private static void log(String s)
    {
       BindingCodeGenUtils.log(s);
    }
     
    // this version logs to console, but also to ABC output (as debugfile)
    private static void log(InstructionList insns, String s)
    {
        BindingCodeGenUtils.log(insns, s);
    }
  
}
