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
package codeOrchestra.actionscript.run.compiler.PlayerComboBox;

import codeOrchestra.actionscript.modulemaker.view.FlexSDKPlayerVersionAnalyzer;
import com.intellij.backport12.openapi.roots.ui.util.CompositeAppearance;
import com.intellij.backport12.openapi.roots.ui.util.SimpleTextCellAppearance;
import com.intellij.backport12.ui.ColoredListCellRendererWrapper;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.backport12.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JList;
import javax.swing.UIManager;

/**
 * @author Eugene Zhuravlev
 * @since May 18, 2005
 */
public class PlayerListRenderer extends ColoredListCellRendererWrapper {
  @Override
  public void doCustomize(JList list, Object value, int index, boolean selected, boolean hasFocus) {
    if (value == null) {
      createAppearanceForInvalidPlayer("<No player>").customize(this);
    } else if (value instanceof String) {
      createAppearanceForPlayer((String)value, selected).customize(this);
    }
    else {
      final String str = value.toString();
      if (str != null) {
        append(str, selected ? SimpleTextAttributes.SELECTED_SIMPLE_CELL_ATTRIBUTES : SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES);
      }
    }
  }

  @NotNull
  public CompositeAppearance createAppearanceForPlayer(@Nullable final String player, final boolean selected) {
    //String name = "Flex SDK Player";
    String name = player;
    String version = player;

    CompositeAppearance appearance = new CompositeAppearance();
    appearance.setIcon(null);
    SimpleTextAttributes attributes = getTextAttributes(FlexSDKPlayerVersionAnalyzer.playerVersionAvailable(player), selected);
    CompositeAppearance.DequeEnd ending = appearance.getEnding();
    ending.addText(name, attributes);

    if (!version.equals(name)) {
      SimpleTextAttributes textAttributes = SimpleTextAttributes.GRAY_ATTRIBUTES;
      ending.addComment(player, textAttributes);
    }

    return ending.getAppearance();
  }

  @NotNull
  public SimpleTextCellAppearance createAppearanceForInvalidPlayer(@NotNull final String text) {
    return SimpleTextCellAppearance.invalid(text, null);
  }

  private static SimpleTextAttributes getTextAttributes(final boolean valid, final boolean selected) {
    if (!valid) {
      return SimpleTextAttributes.ERROR_ATTRIBUTES;
    }
    else if (selected && !((SystemInfo.isWindows && StringUtil.compareVersionNumbers(SystemInfo.OS_VERSION, "6.0") >= 0)
      && UIManager.getLookAndFeel().getName().contains("Windows"))) {
      return SimpleTextAttributes.SELECTED_SIMPLE_CELL_ATTRIBUTES;
    }
    else {
      return SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES;
    }
  }
}
