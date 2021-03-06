package jetbrains.mps.ide.actions;

/*Generated by MPS */

import jetbrains.mps.plugins.pluginparts.actions.GeneratedAction;
import javax.swing.Icon;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.actionSystem.AnActionEvent;
import java.util.Map;
import jetbrains.mps.internal.collections.runtime.MapSequence;
import jetbrains.mps.workbench.MPSDataKeys;
import java.util.Set;
import jetbrains.mps.project.IModule;
import jetbrains.mps.internal.collections.runtime.SetSequence;
import java.util.LinkedHashSet;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import jetbrains.mps.project.MPSProject;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;

public class RecompileProject_Action extends GeneratedAction {
  private static final Icon ICON = null;
  protected static Log log = LogFactory.getLog(RecompileProject_Action.class);

  public RecompileProject_Action() {
    super("Recompile Project", "", ICON);
    this.setIsAlwaysVisible(true);
    this.setExecuteOutsideCommand(true);
  }

  public void doUpdate(@NotNull AnActionEvent event, final Map<String, Object> _params) {
    try {
      this.enable(event.getPresentation());
    } catch (Throwable t) {
      if (log.isErrorEnabled()) {
        log.error("User's action doUpdate method failed. Action:" + "RecompileProject", t);
      }
      this.disable(event.getPresentation());
    }
  }

  protected boolean collectActionData(AnActionEvent event, final Map<String, Object> _params) {
    if (!(super.collectActionData(event, _params))) {
      return false;
    }
    MapSequence.fromMap(_params).put("ideaProject", event.getData(MPSDataKeys.PROJECT));
    if (MapSequence.fromMap(_params).get("ideaProject") == null) {
      return false;
    }
    MapSequence.fromMap(_params).put("project", event.getData(MPSDataKeys.MPS_PROJECT));
    if (MapSequence.fromMap(_params).get("project") == null) {
      return false;
    }
    return true;
  }

  public void doExecute(@NotNull final AnActionEvent event, final Map<String, Object> _params) {
    try {
      final Set<IModule> modules = SetSequence.fromSet(new LinkedHashSet<IModule>());
      SetSequence.fromSet(modules).addSequence(ListSequence.fromList(((MPSProject) MapSequence.fromMap(_params).get("project")).getProjectSolutions()));
      SetSequence.fromSet(modules).addSequence(ListSequence.fromList(((MPSProject) MapSequence.fromMap(_params).get("project")).getProjectLanguages()));
      SetSequence.fromSet(modules).addSequence(ListSequence.fromList(((MPSProject) MapSequence.fromMap(_params).get("project")).getProjectDevKits()));
      ProgressManager.getInstance().run(new DefaultMakeTask(((Project) MapSequence.fromMap(_params).get("ideaProject")), "Rebuilding", modules, true));
    } catch (Throwable t) {
      if (log.isErrorEnabled()) {
        log.error("User's action execute method failed. Action:" + "RecompileProject", t);
      }
    }
  }
}
