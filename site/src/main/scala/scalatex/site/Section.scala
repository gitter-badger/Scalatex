package scalatex.site

import scala.collection.mutable
import scalatags.Text.all
import scalatags.Text.all._
import scalatags.text.Builder
import Styles.css

object Section{
  case class Proxy(func: Seq[Frag] => Frag){
    def apply(body: Frag*) = func(body)
  }
  trait Header{
    def header(anchor: Frag, name: String, subname: String): ConcreteHtmlTag[String]
    def content(frag: Frag): Frag
  }
  object Header{
    def apply(h: (Frag, String, String) => ConcreteHtmlTag[String], c: Frag => Frag = f => f) = {
      new Header {
        def header(anchor: Frag, name: String, subname: String) = h(anchor, name, subname)
        def content(frag: all.Frag) = c(frag)
      }
    }
    implicit def TagToHeaderStrategy(t: ConcreteHtmlTag[String]): Header =
      Header((frag, name, subname) => t(frag, name))
  }


}

/**
 * Lets you instantiate an object used to delimit secitons of your document.
 *
 * This lets you determine a sequence of headers used
 */
trait Section{

  import Section._
  type Header = Section.Header
  val Header = Section.Header
  var structure = Tree[String]("root", mutable.Buffer.empty)
  var depth = 0
  val headers: Seq[Header] = Seq(h1, h2, h3, h4, h5, h6)

  val usedRefs = mutable.Set.empty[String]

  def ref(s: String, txt: String = "") = {
    usedRefs += s
    a(if (txt == "") s else txt, href:=s"#${munge(s)}")
  }

  def headerSeq = {
    def rec(t: Tree[String]): Iterator[String] = {
      Iterator(t.value) ++ t.children.flatMap(rec)
    }
    rec(structure).toVector
  }

  def munge(name: String): String = name.replace(" ", "")

  def headingAnchor(name: String) = a(
    css.headerLink,
    href:=s"#${munge(name)}",
    i(cls:="fa fa-link"),
    position.absolute,
    right:=0
  )


  def apply(header: String, subHeader: String = "") = {
    depth += 1
    val newNode = Tree[String](header, mutable.Buffer.empty)
    structure.children.append(newNode)
    val prev = structure
    structure = newNode
    Proxy{body =>
      val hs = headers(depth - 1)
      val munged = munge(header)

      val res = Seq[Frag](
        hs.header(headingAnchor(munged), header, subHeader)(
          id:=munged,
          display.block,
          css.hoverContainer,
          css.headerTag
        ),
        hs.content(body)
      )
      depth -= 1
      structure = prev
      res
    }
  }
}