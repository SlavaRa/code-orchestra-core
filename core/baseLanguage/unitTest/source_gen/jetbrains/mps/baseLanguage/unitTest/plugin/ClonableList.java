package jetbrains.mps.baseLanguage.unitTest.plugin;

/*Generated by MPS */

import java.util.AbstractList;
import java.util.List;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

public class ClonableList<T> extends AbstractList<T> implements Cloneable {
  private List<T> myData;

  public ClonableList() {
    this(ListSequence.fromList(new ArrayList<T>()));
  }

  public ClonableList(List<T> inner) {
    this.myData = inner;
  }

  public ClonableList(@NotNull T value) {
    this(ListSequence.fromListAndArray(new ArrayList<T>(), value));
  }

  public T get(int index) {
    return ListSequence.fromList(this.myData).getElement(index);
  }

  public int size() {
    return ListSequence.fromList(this.myData).count();
  }

  public T remove(int index) {
    return ListSequence.fromList(this.myData).removeElementAt(index);
  }

  public void add(int index, @NotNull T object) {
    ListSequence.fromList(this.myData).insertElement(index, object);
  }

  public T set(int index, @NotNull T object) {
    return ListSequence.fromList(this.myData).setElement(index, object);
  }

  protected ClonableList<T> clone() throws CloneNotSupportedException {
    ClonableList<T> result = ((ClonableList<T>) super.clone());
    result.myData = ListSequence.fromListWithValues(new ArrayList<T>(), this.myData);
    return result;
  }
}
