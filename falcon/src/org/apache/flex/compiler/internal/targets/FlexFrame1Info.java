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

package org.apache.flex.compiler.internal.targets;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.flex.compiler.definitions.IDefinition;
import org.apache.flex.compiler.definitions.references.IResolvedQualifiersReference;
import org.apache.flex.compiler.definitions.references.ReferenceFactory;
import org.apache.flex.compiler.internal.definitions.ClassDefinition;
import org.apache.flex.compiler.internal.embedding.transcoders.MovieTranscoder;
import org.apache.flex.compiler.internal.embedding.transcoders.TranscoderBase;
import org.apache.flex.compiler.internal.projects.FlexProject;
import org.apache.flex.compiler.internal.units.EmbedCompilationUnit;
import org.apache.flex.compiler.internal.units.ResourceBundleCompilationUnit;
import org.apache.flex.compiler.problems.DependencyNotCompatibleProblem;
import org.apache.flex.compiler.problems.ICompilerProblem;
import org.apache.flex.compiler.units.ICompilationUnit;
import org.apache.flex.compiler.units.ICompilationUnit.UnitType;
import org.apache.flex.compiler.units.requests.IFileScopeRequestResult;
import org.apache.flex.swc.ISWC;

/**
 * Abstract base class that contains information about code that uses the flex
 * framework that is needed by the compiler to generate code in the first frame
 * of flex application SWFs or library.swfs in flex SWCs.
 * <p>
 * This information is collected by looking at all the {@link ICompilationUnit}s
 * that will be built into a flex application SWF or flex SWC.
 */
abstract class FlexFrame1Info
{
    protected FlexFrame1Info(FlexProject flexProject)
    {
        this.flexProject = flexProject;
        IResolvedQualifiersReference resourceBundleRef = ReferenceFactory.packageQualifiedReference(
                flexProject.getWorkspace(), flexProject.getResourceBundleClass());
        resourceBundleDefinition = (ClassDefinition)resourceBundleRef.resolve(flexProject);
        
        // sorted set of resouce bundles
        compiledResourceBundleNames = new TreeSet<String>();
        
        embeddedFonts = new TreeMap<String, FlexFontInfo>();
        
        problems = new ArrayList<ICompilerProblem>();
    }
    
    protected final FlexProject flexProject;
    private final ClassDefinition resourceBundleDefinition;
    final TreeSet<String> compiledResourceBundleNames;
    final TreeMap<String, FlexFontInfo> embeddedFonts;
    
    final ArrayList<ICompilerProblem> problems;
    
    /**
     * Method called by sub-classes to iterate over all the
     * {@link ICompilationUnit}s.
     * 
     * @param builtCompilationUnits {@link Iterable} that can be used to iterate
     * over all the {@link ICompilationUnit}s being built into a flex
     * application SWF or flex SWC.
     * @throws InterruptedException
     */
    protected final void collectFromCompilationUnits(Iterable<ICompilationUnit> builtCompilationUnits) throws InterruptedException
    {
        for(ICompilationUnit cu : builtCompilationUnits) 
        {
            collectFromCompilationUnit(cu);
        }
    }
    
    /**
     * virtual method that sub-classes override to collection information need
     * to generate code for frame1 from a particular {@link ICompilationUnit}.
     * <p>
     * This class collects:
     * <ul>
     * <li>Resource bundle names</li>
     * <li>Embedded font information</li>
     * <li>SWC version problems</li>
     * </ul>
     * 
     * @param cu The {@link ICompilationUnit} to collect information from.
     * @throws InterruptedException
     */
    protected void collectFromCompilationUnit(ICompilationUnit cu) throws InterruptedException
    {
        if (resourceBundleDefinition != null)
            collectCompiledResourceBundleNames(cu);
        collectEmbeddedFonts(cu);
        checkSWCVersioningFilter(problems, cu);
    }
    
    /**
     * Collects embedded font information from the specified
     * {@link ICompilationUnit}.
     * 
     * @param compilationUnit {@link ICompilationUnit} to collect embedded font
     * information from.
     */
    private final void collectEmbeddedFonts(ICompilationUnit compilationUnit)
    {
        if (!(compilationUnit instanceof EmbedCompilationUnit))
            return;

        TranscoderBase transcoder = ((EmbedCompilationUnit)compilationUnit).getEmbedData().getTranscoder();
        if (!(transcoder instanceof MovieTranscoder))
            return;

        MovieTranscoder movieTranscoder = (MovieTranscoder)transcoder;
        FlexFontInfo flexFontInfo = movieTranscoder.getFlexFontInfo();
        if (flexFontInfo != null)
            embeddedFonts.put(movieTranscoder.getSymbol(), flexFontInfo);
    }

    /**
     * Collects the compiled resource bundle names. A resource bundle can exist
     * in two different formats. Either it can be a properties file or a class 
     * that extends mx.resources.ResourceBundle class. This method collects 
     * names for resource bundles that match either of these two formats.
     * 
     * @throws InterruptedException 
     */
    private void collectCompiledResourceBundleNames(ICompilationUnit compilationUnit) 
            throws InterruptedException
    {
        
        //this check is lighter than resolving whether it extends ResourceBundle, 
        //therefore do this for properties files.
        if (compilationUnit instanceof ResourceBundleCompilationUnit)
        {
            //it is a properties file, so add the bundle name to the list
            compiledResourceBundleNames.add(((ResourceBundleCompilationUnit)compilationUnit).getBundleNameInColonSyntax());                   
        }
        else
        {
            IFileScopeRequestResult result = compilationUnit.getFileScopeRequest().get();

            for(IDefinition def : result.getExternallyVisibleDefinitions()) 
            {
                if(def instanceof ClassDefinition) 
                {
                    ClassDefinition classDef = (ClassDefinition)def;
                    //check whether class extends ResourceBundle, but not ResourceBundle.
                    if(!resourceBundleDefinition.equals(classDef) &&
                            classDef.isInstanceOf(resourceBundleDefinition, flexProject)) 
                    {
                        //this is a class that extents mx.resources.ResourceBundle, so add it to the list
                        compiledResourceBundleNames.add(def.getQualifiedName());
                    }
                }
            }
        }
    }
    
    /**
     * Make sure we are not using any SWCs that have a higher minimum version that the 
     * compatibility version.
     * 
     * @param cu
     * @throws InterruptedException 
     */
    private void checkSWCVersioningFilter(ArrayList<ICompilerProblem> problems, ICompilationUnit cu) throws InterruptedException
    {
        Integer compatibilityVersion = flexProject.getCompatibilityVersion();
        // if compatibility-version is not set then we won't be doing any filtering.
        if (compatibilityVersion == null)
            return;
        
        // Only check the dependencies of hand written units
        if (cu.getCompilationUnitType() == UnitType.SWC_UNIT)
            return;
        
        Set<ICompilationUnit> dependencies = flexProject.getDependencies(cu);
        
        for (ICompilationUnit dependency : dependencies)
        {
            if (dependency.getCompilationUnitType() != UnitType.SWC_UNIT)
                continue;

            ISWC swc = flexProject.getWorkspace().getSWCManager().get(
                    new File(dependency.getAbsoluteFilename()));
            int flexMinSupportedVersion = swc.getVersion().getFlexMinSupportedVersionInt();
            if (compatibilityVersion.intValue() < flexMinSupportedVersion)
            {
                problems.add(new DependencyNotCompatibleProblem(dependency.getQualifiedNames().get(0),
                              dependency.getAbsoluteFilename(),
                              swc.getVersion().getFlexMinSupportedVersion(),
                              flexProject.getCompatibilityVersionString()));
            }
        }
    }
    
    /**
     * @return A {@link Set} of {@link String}s containing the dotted fully
     * qualified names of mix-in classes.
     */
    Set<String> getMixins()
    {
        return Collections.emptySet();
    }
    
}
