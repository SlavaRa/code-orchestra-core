/*
 * Copyright 2003-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.mps.workbench.tools;

import com.intellij.openapi.actionSystem.AnActionEvent;
import jetbrains.mps.ide.findusages.view.icons.Icons;
import jetbrains.mps.workbench.action.BaseAction;

import java.util.Map;

public class CloseAction extends BaseAction {
  private BaseTool myTool;

  public CloseAction(BaseTool tool) {
    super("Close", "Close tool", Icons.CLOSE_ICON);
    myTool = tool;
  }

  @Override
  protected boolean isEnabledInASView() {
    return true;
  }

  protected void doExecute(AnActionEvent e, Map<String, Object> _params) {
    myTool.makeUnavailableLater();
  }
}
