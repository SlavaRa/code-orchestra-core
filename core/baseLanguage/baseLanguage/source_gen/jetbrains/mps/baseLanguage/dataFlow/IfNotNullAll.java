package jetbrains.mps.baseLanguage.dataFlow;

/*Generated by MPS */

import jetbrains.mps.analyzers.runtime.framework.DataFlowConstructor;
import jetbrains.mps.smodel.SNode;
import jetbrains.mps.smodel.SModelUtil_new;
import jetbrains.mps.lang.core.behavior.INamedConcept_Behavior;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SNodeOperations;
import jetbrains.mps.lang.dataFlow.framework.Program;
import java.util.List;
import jetbrains.mps.lang.dataFlow.framework.instructions.Instruction;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SLinkOperations;
import jetbrains.mps.internal.collections.runtime.ListSequence;

public class IfNotNullAll extends DataFlowConstructor {
  public IfNotNullAll() {
  }

  public boolean isApplicable(SNode node) {
    return SModelUtil_new.isAssignableConcept(INamedConcept_Behavior.call_getFqName_1213877404258(SNodeOperations.getConceptDeclaration(node)), getApplicableConceptFqName());
  }

  public String getApplicableConceptFqName() {
    return "jetbrains.mps.baseLanguage.structure.IfStatement";
  }

  public void performActions(Program o, SNode node) {
    List<SNode> conditions = NullableUtil.getAndConditions(node);
    for (SNode condition : conditions) {
      if (SNodeOperations.isInstanceOf(condition, "jetbrains.mps.baseLanguage.structure.NotEqualsExpression")) {
        SNode notNullNode = NullableUtil.getOtherThanNull(SNodeOperations.cast(condition, "jetbrains.mps.baseLanguage.structure.BinaryOperation"));
        if (notNullNode != null) {
          {
            Object object = condition;
            if (((Program) o).contains(object)) {
              boolean before = false;
              int position = ((Program) (o)).getEnd(object);
              Instruction instruction = new notNullInstruction(notNullNode);
              instruction.setSource(node);
              ((Program) (o)).insert(instruction, position, true, before);
            }
          }
          {
            Object object = node;
            if (((Program) o).contains(object)) {
              boolean before = false;
              int position = ((Program) (o)).getEnd(object);
              Instruction instruction = new nullableInstruction(notNullNode);
              instruction.setSource(node);
              ((Program) (o)).insert(instruction, position, true, before);
            }
          }
          if (SLinkOperations.getTarget(node, "ifFalseStatement", true) != null) {
            {
              Object object = SLinkOperations.getTarget(node, "ifFalseStatement", true);
              if (((Program) o).contains(object)) {
                boolean before = true;
                int position = ((Program) (o)).getStart(SLinkOperations.getTarget(node, "ifFalseStatement", true));
                Instruction instruction = new nullableInstruction(notNullNode);
                instruction.setSource(node);
                ((Program) (o)).insert(instruction, position, true, before);
              }
            }
          }
          if (ListSequence.fromList(SLinkOperations.getTargets(node, "elsifClauses", true)).isNotEmpty()) {
            {
              Object object = ListSequence.fromList(SLinkOperations.getTargets(node, "elsifClauses", true)).first();
              if (((Program) o).contains(object)) {
                boolean before = true;
                int position = ((Program) (o)).getStart(ListSequence.fromList(SLinkOperations.getTargets(node, "elsifClauses", true)).first());
                Instruction instruction = new nullableInstruction(notNullNode);
                instruction.setSource(node);
                ((Program) (o)).insert(instruction, position, true, before);
              }
            }
          }
        }
      }
    }
  }
}
