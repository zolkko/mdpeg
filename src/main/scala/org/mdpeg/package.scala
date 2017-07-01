package org

package object mdpeg {
  type MultilineTableColumn = Vector[MultilineTableCell]
  type MultilineTableRow = Vector[MultilineTableCell]

  type InlineContent = Seq[Inline]
  /**
    * Flattens a vector of strings into one string, replaces all cr/crlf with spaces
    * @param xs a vector of strings
    * @return a flat string composed of original list
    * */
  def flattenString(xs:Vector[String]): String = xs.mkString(" ").replace("\r\n","").replace("\r", "").replace("\n", "")

  /**
    * Transposes a list. Doesn't fail on lists of different sizes (unlike built-in function)
    * @param xs list to transpose
    * @tparam A element type
    * @return transposed list
    */
  def transposeAnyShape[A](xs: List[List[A]]): List[List[A]] =
    xs.filter(_.nonEmpty) match {
      case Nil => Nil
      case ys: List[List[A]] => (ys map (_ head)) :: transposeAnyShape(ys map (_ tail))
  }

  /**
    * Trims the end of the string
    * @param s string to trim
    * @return end-trimmed string
    */
  def trimEnd(s: String): String = s.
    reverseIterator.
    dropWhile(s => s == ' ').
    toSeq.
    reverseIterator.
    mkString

  /**
    * Trims the end of the string, removes trailing cr/crlf
    * @param s string to trim
    * @return end-trimmed string with no cr/crlf
    */
  def trimEndWithEnding(s: String): String = s.
    reverseIterator.
    dropWhile(s => s == ' ' || s == '\r' || s == '\n').
    toSeq.
    reverseIterator.
    mkString

  def listSplit(pos: List[Int], str: String): List[String] = {
    val (rest, result) = pos.foldRight(z = (str, List[String]())) {
      case (curr, (s, res)) =>
        val (rest, split) = s.splitAt(curr)
        (rest, split :: res)
    }
    rest :: result
  }

  implicit def toPipe[A](a: A): Pipe[A] = Pipe(a)
  implicit def splitToTuple(str: String) = new StringSplitToTuple(str)

  class StringSplitToTuple(s: String) {
    def splitToTuple(pattern: String): (String, String) = {
      s.split(pattern) match {
        case Array(str1, str2) => (str1, str2)
        case Array(str1) => (str1, "")
        case otherwise => sys.error("Split array contains too many elements")
      }
    }
  }
}