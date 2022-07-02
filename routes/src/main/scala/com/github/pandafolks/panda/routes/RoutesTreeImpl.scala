package com.github.pandafolks.panda.routes

import RoutesTree.{Fixed, Node, Wildcard}
import com.github.pandafolks.panda.routes.dto.RoutesMappingInitDto
import org.http4s.Uri
import org.http4s.Uri.Path

final class RoutesTreeImpl(private val root: Node) extends RoutesTree {

  override def getRoot: Node = root.copy()

  override def specifyGroup(path: Path): Option[(GroupInfo, Map[String, String])] = {
    // Searching priority:
    //  - Fixed
    //  - Wildcard
    //  - Pocket
    def rc(curr: Node, segments: List[Uri.Path.Segment]): Option[(GroupInfo, Map[String, String])] = {
      if (segments.isEmpty) curr.groupInfo.map((_, Map.empty[String, String]))
      else curr.children.find(node => node.value match {
        case Fixed(expression) if expression == segments.head.encoded => true
        case _: Fixed => false
        case _: Wildcard => true
      })
        .flatMap(foundNode => rc(foundNode, segments.tail)
          .map(res => foundNode.value match {
            case Wildcard(expression) => (res._1, res._2 + (expression -> segments.head.encoded))
            case _ => res
          })
        )
        .orElse(curr.groupInfo.filter(_.isPocket).map((_, Map.empty[String, String])))
    }

    rc(root, path.segments.toList)
  }
}

object RoutesTreeImpl {

  def unifyPrefixesAndConstruct(data: RoutesMappingInitDto, httpMethod: HttpMethod = HttpMethod.Get): RoutesTree =
    construct(data.withUnifiedPrefixes, httpMethod)

  def construct(data: RoutesMappingInitDto, httpMethod: HttpMethod = HttpMethod.Get): RoutesTree =
    new RoutesTreeImpl(
      data.get(httpMethod).iterator
        .foldLeft(Node(RoutesTree.Wildcard(), List.empty))((root, entry) =>
          insert(
            root = root,
            path = entry._1,
            info = GroupInfo(
              Group(entry._2),
              Path.unsafeFromString(data.prefixes.getOrElse(entry._2, ""))
            )
          )
        )
    )

  private def insert(root: Node, path: String, info: GroupInfo): Node = {

    def rc(parts: List[RoutesTree.SegmentType], currentNode: Node): Node =
      parts.headOption match {
        case None => currentNode.copy(groupInfo = Some(info))
        case Some(RoutesTree.Pocket) => currentNode.copy(groupInfo = Some(info.copy(isPocket = true)))
        case Some(head: RoutesTree.Value) =>
          val affectedNode = currentNode.children.find(_.value == head).getOrElse(Node(head, List.empty))
          currentNode.copy(
            children = (rc(parts.tail, affectedNode) :: currentNode.children.filterNot(_.value == head)).sorted
          )
      }

    def splitIntoParts(path: String)(mapFunc: String => RoutesTree.SegmentType): List[RoutesTree.SegmentType] =
      path.split("/").filterNot(_ == "").map(mapFunc).toList

    val mainParts: List[RoutesTree.SegmentType] = splitIntoParts(path) {
      case "**" => RoutesTree.Pocket
      case s"{{$entry}}" => RoutesTree.Wildcard(entry)
      case entry => RoutesTree.Fixed(entry)
    }

    rc(mainParts, root)
  }
}
