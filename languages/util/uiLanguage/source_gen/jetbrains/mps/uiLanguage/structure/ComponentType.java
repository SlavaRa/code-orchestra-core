package jetbrains.mps.uiLanguage.structure;

/*Generated by MPS */

import jetbrains.mps.baseLanguage.classifiers.structure.BaseClassifierType;
import jetbrains.mps.smodel.SNode;
import jetbrains.mps.smodel.SModel;
import jetbrains.mps.smodel.SModelUtil_new;
import jetbrains.mps.project.GlobalScope;

public class ComponentType extends BaseClassifierType {
  public static final String concept = "jetbrains.mps.uiLanguage.structure.ComponentType";
  public static final String COMPONENT = "component";

  public ComponentType(SNode node) {
    super(node);
  }

  public ComponentDeclaration getComponent() {
    return (ComponentDeclaration) this.getReferent(ComponentDeclaration.class, ComponentType.COMPONENT);
  }

  public void setComponent(ComponentDeclaration node) {
    super.setReferent(ComponentType.COMPONENT, node);
  }

  public static ComponentType newInstance(SModel sm, boolean init) {
    return (ComponentType) SModelUtil_new.instantiateConceptDeclaration("jetbrains.mps.uiLanguage.structure.ComponentType", sm, GlobalScope.getInstance(), init).getAdapter();
  }

  public static ComponentType newInstance(SModel sm) {
    return ComponentType.newInstance(sm, false);
  }
}
