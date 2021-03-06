package org.mdpeg.parsers

import org.mdpeg.ast.HorizontalRuleBlock
import org.parboiled2._

private[mdpeg] trait PrimitiveRules {
  this: Parser =>

  import CharPredicate._

  def horizontalRule: Rule1[HorizontalRuleBlock.type] = {
    def h = (ch: String) => rule(ch ~ sps ~ ch ~ sps ~ ch ~ (sps ~ ch).* ~ sps ~ nl ~ blankLine.+)

    rule(nonIndentSpace ~ capture(h("-") | h("*") | h("_")) ~> ((_: String) => HorizontalRuleBlock))
  }

  def textChar: Rule0 = rule(escapedChar | (!(specialChar | sp) ~ !nl ~ ANY))

  def escapedChar: Rule0 = rule(CharPredicate('\\') ~ ANY)

  def specialChar: Rule0 = rule(anyOf("*_`&[]<!\\"))

  def indentedLine: Rule0 = rule(indent ~ anyLine)

  def anyLine: Rule0 = rule((!nl ~ !EOI ~ ANY).+ ~ nl.?)

  def anyChar: Rule0 = rule(inlineChar | mathChar | specialCharEx)

  def mathChar: Rule0 = rule(anyOf("=/\\*-+^%!<>[]{}"))

  def specialCharEx: Rule0 = rule(anyOf("@#$\"“"))

  def indent: Rule0 = rule("\t" | "    ")

  def nonIndentSpace: Rule0 = rule("   " | "  " | " " | "")

  def inlineChar: Rule0 = rule(atomic(AlphaNum | sp | punctuationChar | anyOf("_\"{}()'^%@#$")))

  def blankLine: Rule0 = rule(sps ~ nl)

  def punctuationChar: Rule0 = rule(anyOf(":;,.?!-’“”—")) // ToDo think how to handle backtick '`' so that it is not confused with verbatim block
  def spnl: Rule0 = rule(sps ~ ((nl ~ sp).? | ""))

  def nl: Rule0 = rule('\r' ~ '\n' | '\n' | '\r')

  def sps: Rule0 = rule(sp.*)

  def sp: Rule0 = rule(" " | " " | "\t") //NB! first 2 rules are DIFFERENT do not simplify this rule; first one is '0xC2 0xA0' aka non-breaking space
  def backTick: Rule0 = rule("`")
}
