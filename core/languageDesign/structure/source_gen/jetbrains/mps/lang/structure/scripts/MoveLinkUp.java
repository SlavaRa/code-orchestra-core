package jetbrains.mps.lang.structure.scripts;

/*Generated by MPS */

import jetbrains.mps.refactoring.framework.BaseGeneratedRefactoring;
import jetbrains.mps.lang.core.scripts.MoveNodes;
import jetbrains.mps.refactoring.framework.RefactoringTarget;
import jetbrains.mps.refactoring.framework.RefactoringContext;
import jetbrains.mps.smodel.SNode;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SNodeOperations;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SConceptOperations;
import jetbrains.mps.util.NameUtil;
import jetbrains.mps.kernel.model.SModelUtil;
import jetbrains.mps.ide.findusages.model.SearchResults;
import jetbrains.mps.ide.findusages.view.FindUtils;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import jetbrains.mps.project.GlobalScope;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SPropertyOperations;
import java.util.Map;
import jetbrains.mps.project.IModule;
import java.util.List;
import jetbrains.mps.smodel.SModel;
import jetbrains.mps.internal.collections.runtime.MapSequence;
import java.util.LinkedHashMap;
import jetbrains.mps.smodel.Language;
import jetbrains.mps.refactoring.framework.RefactoringUtil;
import jetbrains.mps.refactoring.framework.IChooseComponent;
import jetbrains.mps.refactoring.framework.HierarchicalChooseNodeComponent;
import jetbrains.mps.refactoring.framework.ConceptAncestorsProvider;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import java.util.ArrayList;

public class MoveLinkUp extends BaseGeneratedRefactoring {
  public static final String targetConcept = "targetConcept";

  public MoveLinkUp() {
    this.addTransientParameter("targetConcept");
  }

  public String getUserFriendlyName() {
    return "Move Link Up";
  }

  public String getKeyStroke() {
    return getKeyStroke_static();
  }

  public Class getOverridenRefactoringClass() {
    return MoveNodes.class;
  }

  public RefactoringTarget getRefactoringTarget() {
    return RefactoringTarget.NODE;
  }

  public boolean isApplicable(RefactoringContext refactoringContext) {
    SNode concept = SNodeOperations.getAncestor(refactoringContext.getSelectedNode(), "jetbrains.mps.lang.structure.structure.AbstractConceptDeclaration", false, false);

    if (concept == null) {
      return false;
    }
    return ((SNode) refactoringContext.getParameter("targetConcept")) != concept && SConceptOperations.isSuperConceptOf(((SNode) refactoringContext.getParameter("targetConcept")), NameUtil.nodeFQName(concept));
  }

  public boolean isApplicableWRTConcept(SNode node) {
    return SModelUtil.isAssignableConcept(SNodeOperations.getConceptDeclaration(node), SConceptOperations.findConceptDeclaration("jetbrains.mps.lang.structure.structure.LinkDeclaration"));
  }

  public boolean showsAffectedNodes() {
    return true;
  }

  public SearchResults getAffectedNodes(final RefactoringContext refactoringContext) {
    return FindUtils.getSearchResults(new EmptyProgressIndicator(), refactoringContext.getSelectedNode(), GlobalScope.getInstance(), "jetbrains.mps.lang.structure.findUsages.NodeAndDescendantsUsages_Finder");
  }

  public void doRefactor(final RefactoringContext refactoringContext) {
    /*
      SNode linkToReplace = RefUtil.findLinkToMerge(((SNode) refactoringContext.getParameter("targetConcept")), refactoringContext.getSelectedNode());
      if ((linkToReplace != null)) {
        refactoringContext.replaceRefsToNodeWithNode(refactoringContext.getSelectedNode(), linkToReplace);
      } else {
        refactoringContext.moveNodeToNode(refactoringContext.getSelectedNode(), refactoringContext.getSelectedNode().getRole_(), ((SNode) refactoringContext.getParameter("targetConcept")));
      }
    */
    refactoringContext.moveNodeToNode(refactoringContext.getSelectedNode(), refactoringContext.getSelectedNode().getRole_(), ((SNode) refactoringContext.getParameter("targetConcept")));
    refactoringContext.changeFeatureName(refactoringContext.getSelectedNode(), SNodeOperations.getModel(((SNode) refactoringContext.getParameter("targetConcept"))).getSModelFqName() + "." + SPropertyOperations.getString(((SNode) refactoringContext.getParameter("targetConcept")), "name"), SPropertyOperations.getString(refactoringContext.getSelectedNode(), "role"));
  }

  public Map<IModule, List<SModel>> getModelsToGenerate(final RefactoringContext refactoringContext) {
    Map<IModule, List<SModel>> result = MapSequence.fromMap(new LinkedHashMap<IModule, List<SModel>>(16, (float) 0.75, false));
    Language sourceLanguage = Language.getLanguageFor(SNodeOperations.getModel(refactoringContext.getSelectedNode()).getModelDescriptor());
    if (sourceLanguage != null) {
      MapSequence.fromMap(result).putAll(RefactoringUtil.getLanguageAndItsExtendingLanguageModels(refactoringContext.getSelectedMPSProject(), sourceLanguage));
    }
    Language targetLanguage = Language.getLanguageFor(SNodeOperations.getModel(((SNode) refactoringContext.getParameter("targetConcept"))).getModelDescriptor());
    if (targetLanguage != null) {
      MapSequence.fromMap(result).putAll(RefactoringUtil.getLanguageAndItsExtendingLanguageModels(refactoringContext.getSelectedMPSProject(), targetLanguage));
    }
    return result;
  }

  public void updateModel(SModel model, final RefactoringContext refactoringContext) {
    refactoringContext.updateByDefault(model);
  }

  public boolean doesUpdateModel() {
    return true;
  }

  public boolean isOneTargetOnly() {
    return true;
  }

  public IChooseComponent<SNode> targetConcept_componentCreator(final RefactoringContext refactoringContext) {
    SNode abstractConceptDeclaration = SNodeOperations.getAncestor(refactoringContext.getSelectedNode(), "jetbrains.mps.lang.structure.structure.AbstractConceptDeclaration", false, false);
    return new HierarchicalChooseNodeComponent(refactoringContext.getCurrentOperationContext(), new ConceptAncestorsProvider(), abstractConceptDeclaration);
  }

  public List<IChooseComponent> getChooseComponents(final RefactoringContext refactoringContext) {
    List<IChooseComponent> components = ListSequence.fromList(new ArrayList<IChooseComponent>());
    {
      IChooseComponent<SNode> chooseComponent;
      chooseComponent = MoveLinkUp.this.targetConcept_componentCreator(refactoringContext);
      chooseComponent.setPropertyName("targetConcept");
      chooseComponent.setCaption("choose target concept");
      chooseComponent.initComponent();
      ListSequence.fromList(components).addElement(chooseComponent);
    }
    return components;
  }

  public static String getKeyStroke_static() {
    return MoveNodes.getKeyStroke_static();
  }
}
