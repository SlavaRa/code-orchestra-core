package jetbrains.mps.baseLanguage.dates.runtime;

/*Generated by MPS */

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

public class InlineDateFormatter {
  public InlineDateFormatter() {
  }

  public DateTimeFormatter createFormatter() {
    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
    return builder.toFormatter();
  }
}
