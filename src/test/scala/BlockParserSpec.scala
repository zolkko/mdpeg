package org.mdpeg
import org.mdpeg._
import org.mdpeg.ast._
import org.mdpeg.parsers.BlockParser
import org.parboiled2.{ErrorFormatter, ParseError}
import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Failure, Success}
import scala.compat.Platform.EOL

class BlockParserSpec extends FlatSpec with Matchers {
  it should "parse '_' Horizontal rule" in {
    val term =
      """   ____
        |
        |""".stripMargin
    val parsed = new BlockParser(term).horizontalRule.run().get
    parsed shouldEqual HorizontalRuleBlock
  }

  it should "parse '-' sparse Horizontal rule" in {
    val term =
      """   - - - -
        |
        |""".stripMargin
    val parsed = new BlockParser(term).horizontalRule.run().get
    parsed shouldEqual HorizontalRuleBlock
  }

  it should "parse '*'-many Horizontal rule" in {
    val term =
      """   ***************
        |
        |""".stripMargin
    val parsed = new BlockParser(term).horizontalRule.run().get
    parsed shouldEqual HorizontalRuleBlock
  }

  it should "parse '>' Block Quote" in {
    val term =
      s"""> ${TestData.blockQuoteLineOne}
        |> ${TestData.blockQuoteLineTwo}
        |> ${TestData.blockQuoteLineThree}
        |""".stripMargin

    val expectedText = TestData.blockQuote
    val parsed = new BlockParser(term).blockQuote.run().get
    parsed shouldEqual
      BlockQuote(Vector(
        Markdown(RawMarkdownContent("This is quote")),
        Markdown(RawMarkdownContent("and should span several")),
        Markdown(RawMarkdownContent("yet another line for the block"))))
  }

  it should "parse Paragraph" in {
    val term =
      s"""${TestData.paragraphOne}
        |
        |""".stripMargin
    val parser =new BlockParser(term)
    parser.InputLine.run().get shouldEqual Vector(ExpectedTestResults.paragraphOne)
  }

  it should "parse Plain text" in {
    val term = TestData.plainText
    val parsed = new BlockParser(term).plain.run().get
    parsed shouldEqual ExpectedTestResults.plainText
  }

  it should "parse ATX Heading" in {
    val term =
      s""" ${TestData.headingOne}
        |
      """.stripMargin
    val ts = (1 to 6).map(i => (i, "#" * i + term))
    for (t <- ts) {
      val parsed = new BlockParser(t._2).heading.run().get
      parsed shouldEqual HeadingBlock(t._1,Vector(Text("Heading"), Space, Text("One")))
    }
  }

  it should "parse a Verbatim" in {
    val term =
      s"""```
         |${TestData.codeBlock}
         |```""".stripMargin
    val parsed = new BlockParser(term).verbatim.run().get
    parsed shouldEqual Verbatim(TestData.codeBlock, None)
  }

  it should "parse a Verbatim with long spaces" in {
    val term =
      s"""```
         |${TestData.codeBlock2}
         |```""".stripMargin
    val parsed = new BlockParser(term).verbatim.run().get
    parsed shouldEqual Verbatim(TestData.codeBlock2, None)
  }

  it should "parse a JSON like Verbatim" in {
    val term =
      s"""```
         |${TestData.codeBlock3}
         |```""".stripMargin
    val parsed = new BlockParser(term).verbatim.run().get
    parsed shouldEqual Verbatim(TestData.codeBlock3, None)
  }

  it should "parse a Verbatim with blank lines, comments, spaces" in {
    val term =
      s"""```
         |${TestData.codeBlock4}
         |```""".stripMargin
    val parsed = new BlockParser(term).verbatim.run().get
    parsed shouldEqual Verbatim(TestData.codeBlock4, None)
  }

  it should "parse a Verbatim along with syntax name" in {
    val term =
      s"""```pseudo language
         |${TestData.codeBlock2}
         |```""".stripMargin
    val parsed = new BlockParser(term).verbatim.run().get
    parsed shouldEqual Verbatim(TestData.codeBlock2, Some("pseudo language"))
  }

  it should "parse a Verbatim along with syntax surrounded by whitespaces" in {
    val syntaxName = " \t pseudo language \t "
    val term =
      s"""```$syntaxName
         |${TestData.codeBlock2}
         |```""".stripMargin
    val parsed = new BlockParser(term).verbatim.run().get
    parsed shouldEqual Verbatim(TestData.codeBlock2, Some("pseudo language"))
  }

  it should "parse a reference with title" in {
    val term = s"""[arbitrary case-insensitive reference text]: https://www.mozilla.org 'this is title'""".stripMargin
    new BlockParser(term).reference.run().get shouldEqual
      ReferenceBlock(Vector(Text("arbitrary"), Space, Text("case-insensitive"), Space, Text("reference"), Space, Text("text")),Src("https://www.mozilla.org",Some("this is title")))
  }

  it should "parse a reference without title" in {
    val term = s"""[arbitrary case-insensitive 123]: https://www.mozilla.org""".stripMargin
    new BlockParser(term).reference.run().get shouldEqual
      ReferenceBlock(Vector(Text("arbitrary"), Space, Text("case-insensitive"), Space, Text("123")),Src("https://www.mozilla.org",None))
  }

  it should "parse several consequent multiline tables split by different intervals" in {
    val term =
      s"""${TestData.complexTable}
         |${TestData.complexTable}
         |
         |${TestData.complexTable}
         |
         |
         |${TestData.complexTable}
         |""".stripMargin
    val parsed = new BlockParser(term).InputLine.run().get
    parsed shouldEqual Vector(
      ExpectedTestResults.complexTable,
      ExpectedTestResults.complexTable,
      ExpectedTestResults.complexTable,
      ExpectedTestResults.complexTable)
  }

  it should "parse a compound document" in {
    val term = TestData.compoundMD
    val parser = new BlockParser(term)
    val parsed = parser.InputLine.run()

    parsed.get shouldEqual Vector(
      ExpectedTestResults.headingOne,
      ExpectedTestResults.headingTwo,
      ExpectedTestResults.paragraphOne,
      ExpectedTestResults.paragraphTwo,
      HorizontalRuleBlock,
      HorizontalRuleBlock,
      ExpectedTestResults.blockQuote,
      HorizontalRuleBlock,
      Verbatim(TestData.codeBlock4, None),
      ExpectedTestResults.plainTextCompound,
      ExpectedTestResults.texBlock1,
      ExpectedTestResults.unorderedList,
      ExpectedTestResults.orderedList,
      ExpectedTestResults.complexTable,
      ExpectedTestResults.referenceType1,
      ExpectedTestResults.referenceType2
    )
  }

  it should "parse a paragraph followed by a list as a paragraph and a list" in {
    val term =
      """hello from the other side
        |second line from the other side
        |
        |* sub 1
        |* sub 2
        |* sub 3
        |* sub 4""".stripMargin
    val parser = new BlockParser(term)
    val parsed = parser.InputLine.run()
    parsed.get shouldEqual Vector(
      Paragraph(Vector(
        Text("hello"), Space, Text("from"), Space, Text("the"), Space, Text("other"), Space, Text("side"), Space,
        Text("second"), Space, Text("line"), Space, Text("from"), Space, Text("the"), Space, Text("other"), Space,
        Text("side"))),
      UnorderedList(Vector(Markdown(RawMarkdownContent("sub 1")), Markdown(RawMarkdownContent("sub 2")), Markdown(RawMarkdownContent("sub 3")), Markdown(RawMarkdownContent("sub 4")))))

  }

  it should "parse a paragraph followed by an unordered list as plain followed by an unordered list" in {
    val term =
      """hello from the other side
        |second line from the other side
        |
        |* sub 1
        |  some sub text
        |* sub 2
        |  also some
        |  sub text
        |  here
        |* sub 3
        |* sub 4""".stripMargin
    val parser = new BlockParser(term)
    parser.InputLine.run().get shouldEqual
      Vector(
        Paragraph(Vector(Text("hello"), Space, Text("from"), Space, Text("the"), Space, Text("other"), Space, Text("side"),
                     Space, Text("second"), Space, Text("line"), Space, Text("from"), Space, Text("the"), Space,
                     Text("other"), Space, Text("side"))),
        UnorderedList(Vector(
          Markdown(RawMarkdownContent("""sub 1
                     |  some sub text""".stripMargin)),
          Markdown(RawMarkdownContent("""sub 2
                     |  also some
                     |  sub text
                     |  here""".stripMargin)),
          Markdown(RawMarkdownContent("sub 3")),
          Markdown(RawMarkdownContent("sub 4"))
        )))
  }

  it should "parse a paragraph followed by an ordered list as plain followed by an ordered list" in {
    val term =
      """hello from the other side
        |second line from the other side
        |
        |1. sub 1
        |2. sub 2""".stripMargin
    val parser = new BlockParser(term)
    parser.InputLine.run().get shouldEqual
      Vector(
        Paragraph(Vector(
          Text("hello"), Space, Text("from"), Space, Text("the"), Space, Text("other"), Space, Text("side"), Space,
          Text("second"), Space, Text("line"), Space, Text("from"), Space, Text("the"),
          Space, Text("other"), Space, Text("side"))),
        OrderedList(Vector(
          Markdown(RawMarkdownContent("sub 1")),
          Markdown(RawMarkdownContent("sub 2")))
        ))
  }

  it should "parse a table without caption followed by a text" in {
    val term =
      """--------------------------------------------------------------------------------
        |Term                  Description
        |----------------      ------------------------------------------------
        |cell 1                cell 2
        |--------------------------------------------------------------------------------
        |
        |kio""".stripMargin
    val parser = new BlockParser(term)
    parser.InputLine.run().get shouldEqual
      Vector(MultilineTableBlock(Vector(25.0f, 75.0f),
        None,
        Some(Vector(MultilineTableCell(Vector(Markdown(RawMarkdownContent("Term")))), MultilineTableCell(Vector(Markdown(RawMarkdownContent("Description")))))),
        Vector(Vector(MultilineTableCell(Vector(Markdown(RawMarkdownContent("cell 1"))))),
          Vector(MultilineTableCell(Vector(Markdown(RawMarkdownContent("cell 2"))))))),
        Plain(Vector(Text("kio"))))
  }

  it should "parse a TeX block" in {
    val term =
      """$$$
        |\frac{1+sin(x)}{y}
        |
        |
        |$$ \begin{array}{l}
        |x = k \cdot a \cdot \left(a + b\right) \\
        |y = k \cdot b \cdot \left(a + b\right) \\
        |z = k \cdot a \cdot b,
        |\end{array} $$
        |
        |
        |$$$""".stripMargin
    val parser = new BlockParser(term)
    parser.InputLine.run().get shouldEqual Vector(
      TexBlock(TexContent(
        """\frac{1+sin(x)}{y}
          |
          |
          |$$ \begin{array}{l}
          |x = k \cdot a \cdot \left(a + b\right) \\
          |y = k \cdot b \cdot \left(a + b\right) \\
          |z = k \cdot a \cdot b,
          |\end{array} $$
          |
          |""".stripMargin))
    )
  }
}