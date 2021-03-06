package jetbrains.mps.nanoc.structure;

/*Generated by MPS */

import jetbrains.mps.smodel.SNode;
import jetbrains.mps.smodel.SModel;
import jetbrains.mps.smodel.SModelUtil_new;
import jetbrains.mps.project.GlobalScope;

public class CFor extends CStatement {
  public static final String concept = "jetbrains.mps.nanoc.structure.CFor";
  public static final String INITIAL = "initial";
  public static final String CONDITION = "condition";
  public static final String ITERATION = "iteration";
  public static final String BODY = "body";

  public CFor(SNode node) {
    super(node);
  }

  public CExpression getInitial() {
    return (CExpression) this.getChild(CExpression.class, CFor.INITIAL);
  }

  public void setInitial(CExpression node) {
    super.setChild(CFor.INITIAL, node);
  }

  public CExpression getCondition() {
    return (CExpression) this.getChild(CExpression.class, CFor.CONDITION);
  }

  public void setCondition(CExpression node) {
    super.setChild(CFor.CONDITION, node);
  }

  public CExpression getIteration() {
    return (CExpression) this.getChild(CExpression.class, CFor.ITERATION);
  }

  public void setIteration(CExpression node) {
    super.setChild(CFor.ITERATION, node);
  }

  public CBody getBody() {
    return (CBody) this.getChild(CBody.class, CFor.BODY);
  }

  public void setBody(CBody node) {
    super.setChild(CFor.BODY, node);
  }

  public static CFor newInstance(SModel sm, boolean init) {
    return (CFor) SModelUtil_new.instantiateConceptDeclaration("jetbrains.mps.nanoc.structure.CFor", sm, GlobalScope.getInstance(), init).getAdapter();
  }

  public static CFor newInstance(SModel sm) {
    return CFor.newInstance(sm, false);
  }
}
