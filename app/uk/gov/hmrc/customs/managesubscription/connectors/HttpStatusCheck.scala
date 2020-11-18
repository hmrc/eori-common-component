package uk.gov.hmrc.customs.managesubscription.connectors

object HttpStatusCheck {

  def is2xxSuccessfull: Int => Boolean = status => status >= 200 && status < 299
}
