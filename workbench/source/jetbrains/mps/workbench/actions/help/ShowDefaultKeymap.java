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
package jetbrains.mps.workbench.actions.help;

import jetbrains.mps.util.annotation.CodeOrchestraPatch;

public class ShowDefaultKeymap extends ShowSiteAction {
  public ShowDefaultKeymap() {
    super("Default Keymap Reference");

  }

  @CodeOrchestraPatch
  protected String getSiteURL() {
    return "http://codeOrchestra-media.s3.amazonaws.com/tutorials/03_codenavigation/CodeOrchestra-Cheatlist.pdf"; // RE-2646
  }
}