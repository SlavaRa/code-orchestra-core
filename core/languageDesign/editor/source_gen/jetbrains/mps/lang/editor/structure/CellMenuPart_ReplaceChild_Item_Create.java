package jetbrains.mps.lang.editor.structure;

/*Generated by MPS */

import jetbrains.mps.baseLanguage.structure.ConceptFunction;
import jetbrains.mps.smodel.SNode;
import jetbrains.mps.smodel.SModel;
import jetbrains.mps.smodel.SModelUtil_new;
import jetbrains.mps.project.GlobalScope;

public class CellMenuPart_ReplaceChild_Item_Create extends ConceptFunction {
  public static final String concept = "jetbrains.mps.lang.editor.structure.CellMenuPart_ReplaceChild_Item_Create";

  public CellMenuPart_ReplaceChild_Item_Create(SNode node) {
    super(node);
  }

  public static CellMenuPart_ReplaceChild_Item_Create newInstance(SModel sm, boolean init) {
    return (CellMenuPart_ReplaceChild_Item_Create) SModelUtil_new.instantiateConceptDeclaration("jetbrains.mps.lang.editor.structure.CellMenuPart_ReplaceChild_Item_Create", sm, GlobalScope.getInstance(), init).getAdapter();
  }

  public static CellMenuPart_ReplaceChild_Item_Create newInstance(SModel sm) {
    return CellMenuPart_ReplaceChild_Item_Create.newInstance(sm, false);
  }
}
