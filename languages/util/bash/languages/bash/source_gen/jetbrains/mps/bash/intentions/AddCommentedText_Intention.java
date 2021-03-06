package jetbrains.mps.bash.intentions;

/*Generated by MPS */

import jetbrains.mps.intentions.BaseIntention;
import jetbrains.mps.intentions.Intention;
import jetbrains.mps.smodel.SNode;
import jetbrains.mps.nodeEditor.EditorContext;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SNodeOperations;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SLinkOperations;
import jetbrains.mps.smodel.action.SNodeFactoryOperations;

public class AddCommentedText_Intention extends BaseIntention implements Intention {
  public AddCommentedText_Intention() {
  }

  public String getConcept() {
    return "jetbrains.mps.bash.structure.CommandList";
  }

  public boolean isParameterized() {
    return false;
  }

  public boolean isErrorIntention() {
    return false;
  }

  public boolean isAvailableInChildNodes() {
    return true;
  }

  public String getDescription(final SNode node, final EditorContext editorContext) {
    return "Add Comment";
  }

  public boolean isApplicable(final SNode node, final EditorContext editorContext) {
    if (!(this.isApplicableToNode(node, editorContext))) {
      return false;
    }
    return true;
  }

  public boolean isApplicableToNode(final SNode node, final EditorContext editorContext) {
    SNode selectedNode = editorContext.getSelectedNode();
    SNode commandlist = SNodeOperations.getAncestor(selectedNode, "jetbrains.mps.bash.structure.CommandList", false, false);
    if (commandlist != node) {
      return false;
    }
    return (SLinkOperations.getTarget(node, "comment", true) == null);
  }

  public void execute(final SNode node, final EditorContext editorContext) {
    SNodeFactoryOperations.setNewChild(node, "comment", "jetbrains.mps.bash.structure.CommentedText");
    editorContext.select(SLinkOperations.getTarget(node, "comment", true));
  }

  public String getLocationString() {
    return "jetbrains.mps.bash.intentions";
  }
}
