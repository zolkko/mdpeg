import com.mdpeg.{ListBlockParser, Markdown, OrderedList, UnorderedList}
import org.parboiled2.{ErrorFormatter, ParseError, Parser, ParserInput}
import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Failure, Success, Try}

class ListParserSpec extends FlatSpec with Matchers {
  class ListParserTestSpec(val input: ParserInput) extends Parser with ListBlockParser {}
  object PrettyPrintListParser {
    def apply(parser : ListParserTestSpec) : Unit= {
      val result= parser.bulletListItem.run()
      result match {
        case Failure(error) =>
          error match {
            case e : ParseError => println(parser.formatError(e, new ErrorFormatter(showTraces = true)))
            case _ => println(error)
          }
        case Success(value) => println(value)
      }
    }
  }

  def hasSucceededUnordered(parserResult: Try[UnorderedList]): Boolean = !hasFailedUnordered(parserResult)
  def hasFailedUnordered(parserResult: Try[UnorderedList]): Boolean = parserResult match {
    case Failure(_) => true
    case Success(_) => false
  }
  def hasFailedOrdered(parsed: Try[OrderedList]) = parsed match {
    case Failure(_) => true
    case Success(_) => false
  }

  it should "parse unordered list's bullets '-*+'" in {
    for (ch <- Vector("-","*","+")) {
      val term = s"""$ch """.stripMargin
      new ListParserTestSpec(term).bullet.run().get
    }
  }

  it should "parse ordered list's enumerator 1..999" in {
    for (d <- 1 to 999) {
      val term = s"""$d. """.stripMargin
      new ListParserTestSpec(term).enumerator.run().get
    }
  }

  it should "parse tight bullet list" in {
    val firstItem = "First item"
    val secondItem = "First item"
    val term =
      s"""- $firstItem
         |- $secondItem
       """.stripMargin
    val expectedFirst =
      s"""$firstItem
         |""".stripMargin
    val expectedSecond =
      s"""$secondItem
         |""".stripMargin
    val parsed = new ListParserTestSpec(term).bulletListTight.run()
    parsed.get shouldEqual UnorderedList(Vector(Vector(Markdown(expectedFirst), Markdown(expectedSecond))))
  }

  it should "fail on sparse bullet list while parsing it as tight" in {
    val term =
      s"""- First item
         |
         |- Second item
       """.stripMargin
    val parsed: Try[UnorderedList] = new ListParserTestSpec(term).bulletListTight.run()
    hasFailedUnordered(parsed) shouldEqual true
  }

  it should "parse sparse bullet list" in {
    val firstItem = "First item"
    val secondItem = "First item"
    val term =
      s"""- $firstItem
         |
         |- $secondItem
         |
       """.stripMargin
    val parsed = new ListParserTestSpec(term).bulletListSparse.run()

    val expectedFirst =
      s"""$firstItem
         |""".stripMargin
    val expectedSecond =
      s"""$secondItem
         |""".stripMargin
    parsed.get shouldEqual UnorderedList(Vector(Vector(Markdown(expectedFirst), Markdown(expectedSecond))))
  }

  it should "parse tight ordered list" in {
    val firstItem = "First item"
    val secondItem = "First item"
    val term =
      s"""1. $firstItem
         |2. $secondItem
       """.stripMargin
    val expectedFirst =
      s"""$firstItem
         |""".stripMargin
    val expectedSecond =
      s"""$secondItem
         |""".stripMargin
    val parsed = new ListParserTestSpec(term).orderedListTight.run()
    parsed.get shouldEqual OrderedList(Vector(Vector(Markdown(expectedFirst), Markdown(expectedSecond))))
  }

  it should "parse sparse ordered list" in {
    val firstItem = "First item"
    val secondItem = "First item"
    val term =
      s"""1. $firstItem
         |
         |2. $secondItem
         |
       """.stripMargin
    val parsed = new ListParserTestSpec(term).orderedListSparse.run()
    val expectedFirst =
      s"""$firstItem
         |""".stripMargin
    val expectdSecond =
      s"""$secondItem
         |""".stripMargin
    parsed.get shouldEqual OrderedList(Vector(Vector(Markdown(expectedFirst), Markdown(expectdSecond))))
  }

  it should "fail sparse ordered list while parsing it as list" in {
    val term =
      s"""1. First item
         |
         |2. Second item
       """.stripMargin
    val parsed: Try[OrderedList] = new ListParserTestSpec(term).orderedListTight.run()
    hasFailedOrdered(parsed) shouldEqual true
  }
}
