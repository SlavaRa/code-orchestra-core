package jetbrains.mps.baseLanguage.regexp.structure;

/*Generated by MPS */

import jetbrains.mps.baseLanguage.structure.Closure;
import jetbrains.mps.smodel.SNode;
import jetbrains.mps.smodel.SModel;
import jetbrains.mps.smodel.SModelUtil_new;
import jetbrains.mps.project.GlobalScope;

public class ReplaceBlock extends Closure {
  public static final String concept = "jetbrains.mps.baseLanguage.regexp.structure.ReplaceBlock";

  public ReplaceBlock(SNode node) {
    super(node);
  }

  public static ReplaceBlock newInstance(SModel sm, boolean init) {
    return (ReplaceBlock) SModelUtil_new.instantiateConceptDeclaration("jetbrains.mps.baseLanguage.regexp.structure.ReplaceBlock", sm, GlobalScope.getInstance(), init).getAdapter();
  }

  public static ReplaceBlock newInstance(SModel sm) {
    return ReplaceBlock.newInstance(sm, false);
  }
}
