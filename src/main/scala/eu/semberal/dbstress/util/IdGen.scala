package eu.semberal.dbstress.util

import org.apache.commons.lang3.RandomStringUtils

object IdGen {
  val IdPlaceholder = "@@gen_query_id@@"

  private val IdLength = 4

  private def gen() = RandomStringUtils.randomAlphabetic(IdLength)

  val genScenarioId, genConnectionId, genStatementId = gen _

}
