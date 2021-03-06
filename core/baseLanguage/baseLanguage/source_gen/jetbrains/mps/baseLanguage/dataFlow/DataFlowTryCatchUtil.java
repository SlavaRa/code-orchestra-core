package jetbrains.mps.baseLanguage.dataFlow;

/*Generated by MPS */

import java.util.List;
import jetbrains.mps.smodel.SNode;
import java.util.ArrayList;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SNodeOperations;
import java.util.Set;
import jetbrains.mps.baseLanguage.behavior.Statement_Behavior;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SLinkOperations;
import jetbrains.mps.typesystem.inference.TypeChecker;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import java.util.HashSet;
import jetbrains.mps.smodel.SModelUtil_new;
import jetbrains.mps.project.GlobalScope;
import jetbrains.mps.smodel.SReference;
import jetbrains.mps.smodel.SModelReference;
import jetbrains.mps.smodel.SNodeId;

public class DataFlowTryCatchUtil {
  public DataFlowTryCatchUtil() {
  }

  public static List<SNode> getPossibleCatches(SNode source, List<SNode> catchClauses) {
    List<SNode> result = new ArrayList<SNode>();
    SNode statement = SNodeOperations.getAncestor(source, "jetbrains.mps.baseLanguage.structure.Statement", false, false);
    Set<SNode> uncaughtThrowables = Statement_Behavior.call_uncaughtThrowables_5412515780383108857(statement, false);
    for (SNode catchClause : catchClauses) {
      SNode caughtType = SLinkOperations.getTarget(SLinkOperations.getTarget(catchClause, "throwable", true), "type", true);
      if (TypeChecker.getInstance().getSubtypingManager().isSubtype(caughtType, new DataFlowTryCatchUtil.QuotationClass_l1x7gt_a1a0a0b0d0a().createNode()) || TypeChecker.getInstance().getSubtypingManager().isSubtype(caughtType, new DataFlowTryCatchUtil.QuotationClass_l1x7gt_a1a0a0b0d0a_0().createNode()) || TypeChecker.getInstance().getSubtypingManager().isSubtype(new DataFlowTryCatchUtil.QuotationClass_l1x7gt_a0a0a1a3a0().createNode(), caughtType)) {
        ListSequence.fromList(result).addElement(catchClause);
      } else {
        for (SNode throwed : uncaughtThrowables) {
          if (TypeChecker.getInstance().getSubtypingManager().isSubtype(new DataFlowTryCatchUtil.QuotationClass_l1x7gt_a0a0a0a0a1a3a0().createNode(throwed), caughtType)) {
            ListSequence.fromList(result).addElement(catchClause);
          }
        }
      }
    }
    return result;
  }

  public static class QuotationClass_l1x7gt_a1a0a0b0d0a {
    public QuotationClass_l1x7gt_a1a0a0b0d0a() {
    }

    public SNode createNode() {
      SNode result = null;
      Set<SNode> _parameterValues_129834374 = new HashSet<SNode>();
      SNode quotedNode_1 = null;
      {
        quotedNode_1 = SModelUtil_new.instantiateConceptDeclaration("jetbrains.mps.baseLanguage.structure.ClassifierType", null, GlobalScope.getInstance(), false);
        SNode quotedNode1_2 = quotedNode_1;
        quotedNode1_2.addReference(SReference.create("classifier", quotedNode1_2, SModelReference.fromString("f:java_stub#6354ebe7-c22a-4a0f-ac54-50b52ab9b065#java.lang(java.lang@java_stub)"), SNodeId.fromString("~Error")));
        result = quotedNode1_2;
      }
      return result;
    }
  }

  public static class QuotationClass_l1x7gt_a1a0a0b0d0a_0 {
    public QuotationClass_l1x7gt_a1a0a0b0d0a_0() {
    }

    public SNode createNode() {
      SNode result = null;
      Set<SNode> _parameterValues_129834374 = new HashSet<SNode>();
      SNode quotedNode_1 = null;
      {
        quotedNode_1 = SModelUtil_new.instantiateConceptDeclaration("jetbrains.mps.baseLanguage.structure.ClassifierType", null, GlobalScope.getInstance(), false);
        SNode quotedNode1_2 = quotedNode_1;
        quotedNode1_2.addReference(SReference.create("classifier", quotedNode1_2, SModelReference.fromString("f:java_stub#6354ebe7-c22a-4a0f-ac54-50b52ab9b065#java.lang(java.lang@java_stub)"), SNodeId.fromString("~RuntimeException")));
        result = quotedNode1_2;
      }
      return result;
    }
  }

  public static class QuotationClass_l1x7gt_a0a0a1a3a0 {
    public QuotationClass_l1x7gt_a0a0a1a3a0() {
    }

    public SNode createNode() {
      SNode result = null;
      Set<SNode> _parameterValues_129834374 = new HashSet<SNode>();
      SNode quotedNode_1 = null;
      {
        quotedNode_1 = SModelUtil_new.instantiateConceptDeclaration("jetbrains.mps.baseLanguage.structure.ClassifierType", null, GlobalScope.getInstance(), false);
        SNode quotedNode1_2 = quotedNode_1;
        quotedNode1_2.addReference(SReference.create("classifier", quotedNode1_2, SModelReference.fromString("f:java_stub#6354ebe7-c22a-4a0f-ac54-50b52ab9b065#java.lang(java.lang@java_stub)"), SNodeId.fromString("~Exception")));
        result = quotedNode1_2;
      }
      return result;
    }
  }

  public static class QuotationClass_l1x7gt_a0a0a0a0a1a3a0 {
    public QuotationClass_l1x7gt_a0a0a0a0a1a3a0() {
    }

    public SNode createNode(Object parameter_3) {
      SNode result = null;
      Set<SNode> _parameterValues_129834374 = new HashSet<SNode>();
      SNode quotedNode_1 = null;
      {
        quotedNode_1 = SModelUtil_new.instantiateConceptDeclaration("jetbrains.mps.baseLanguage.structure.ClassifierType", null, GlobalScope.getInstance(), false);
        SNode quotedNode1_2 = quotedNode_1;
        quotedNode1_2.setReferent("classifier", (SNode) parameter_3);
        result = quotedNode1_2;
      }
      return result;
    }
  }
}
