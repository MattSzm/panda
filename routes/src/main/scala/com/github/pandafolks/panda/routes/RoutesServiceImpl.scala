package com.github.pandafolks.panda.routes

import cats.effect.Resource
import com.github.pandafolks.panda.routes.dto.{MapperRecordDto, RoutesResourceDto}
import com.github.pandafolks.panda.routes.mappers.{Mapper, MappingContent, Prefix}
import com.github.pandafolks.panda.utils.PersistenceError
import monix.connect.mongodb.client.CollectionOperator
import monix.eval.Task

final class RoutesServiceImpl(private val mapperDao: MapperDao, private val prefixesDao: PrefixesDao)(
  private val c: Resource[Task, (CollectionOperator[Mapper], CollectionOperator[Prefix])]) extends RoutesService {

  override def saveRoutes(routesResourceDto: RoutesResourceDto): Task[(List[Either[PersistenceError, String]], List[Either[PersistenceError, String]])] =
    c.use {
      case (mapperOperator, prefixesOperator) =>
        Task.parZip2(
          Task.parTraverse(routesResourceDto.mappers.getOrElse(Map.empty).toList) { entry =>
            mapperDao.saveMapper(entry._1, entry._2)(mapperOperator)
          },
          Task.parTraverse(routesResourceDto.prefixes.getOrElse(Map.empty).toList) { entry =>
            prefixesDao.savePrefix(entry._1, entry._2)(prefixesOperator)
          }
        )
    }

  override def findAll(): Task[RoutesResourceDto] = c.use {
    case (mapperOperator, prefixesOperator) =>
      Task.parMap2(
        mapperDao.findAll(mapperOperator)
          .map(mapper => (mapper.route, mapper.httpMethod, MappingContent.toMappingDto(mapper.mappingContent)))
          .foldLeftL(Map.empty[String, MapperRecordDto])((prevState, p) => prevState + (p._1 -> MapperRecordDto(p._3, Some(p._2)))),

        prefixesDao.findAll(prefixesOperator)
          .map(prefix => (prefix.groupName, prefix.value))
          .foldLeftL(Map.empty[String, String])((prevState, p) => prevState + p)
      )((mappers, prefixes) => RoutesResourceDto(mappers = Some(mappers), prefixes = Some(prefixes)))
  }
}