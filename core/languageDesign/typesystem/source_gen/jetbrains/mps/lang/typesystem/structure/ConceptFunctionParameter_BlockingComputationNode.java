package jetbrains.mps.lang.typesystem.structure;

/*Generated by MPS */

import jetbrains.mps.baseLanguage.structure.ConceptFunctionParameter;
import jetbrains.mps.smodel.SNode;
import jetbrains.mps.smodel.SModel;
import jetbrains.mps.smodel.SModelUtil_new;
import jetbrains.mps.project.GlobalScope;

public class ConceptFunctionParameter_BlockingComputationNode extends ConceptFunctionParameter {
  public static final String concept = "jetbrains.mps.lang.typesystem.structure.ConceptFunctionParameter_BlockingComputationNode";

  public ConceptFunctionParameter_BlockingComputationNode(SNode node) {
    super(node);
  }

  public static ConceptFunctionParameter_BlockingComputationNode newInstance(SModel sm, boolean init) {
    return (ConceptFunctionParameter_BlockingComputationNode) SModelUtil_new.instantiateConceptDeclaration("jetbrains.mps.lang.typesystem.structure.ConceptFunctionParameter_BlockingComputationNode", sm, GlobalScope.getInstance(), init).getAdapter();
  }

  public static ConceptFunctionParameter_BlockingComputationNode newInstance(SModel sm) {
    return ConceptFunctionParameter_BlockingComputationNode.newInstance(sm, false);
  }
}
