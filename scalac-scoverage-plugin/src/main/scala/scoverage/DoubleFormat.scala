package scoverage

import java.text.{DecimalFormat, DecimalFormatSymbols}
import java.util.Locale

object DoubleFormat {
  private[this] val twoFractionDigitsFormat: DecimalFormat = {
    val fmt = new DecimalFormat()
    fmt.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US))
    fmt.setMinimumIntegerDigits(1)
    fmt.setMinimumFractionDigits(2)
    fmt.setMaximumFractionDigits(2)
    fmt.setGroupingUsed(false)
    fmt
  }

  def twoFractionDigits(d: Double) = twoFractionDigitsFormat.format(d)

}
