package jetbrains.mps.ui.structure;

/*Generated by MPS */

import jetbrains.mps.baseLanguage.closures.structure.ClosureLiteral;
import jetbrains.mps.smodel.SNode;
import jetbrains.mps.smodel.SModel;
import jetbrains.mps.smodel.SModelUtil_new;
import jetbrains.mps.project.GlobalScope;

public class ValidateBlock extends ClosureLiteral {
  public static final String concept = "jetbrains.mps.ui.structure.ValidateBlock";

  public ValidateBlock(SNode node) {
    super(node);
  }

  public static ValidateBlock newInstance(SModel sm, boolean init) {
    return (ValidateBlock) SModelUtil_new.instantiateConceptDeclaration("jetbrains.mps.ui.structure.ValidateBlock", sm, GlobalScope.getInstance(), init).getAdapter();
  }

  public static ValidateBlock newInstance(SModel sm) {
    return ValidateBlock.newInstance(sm, false);
  }
}
