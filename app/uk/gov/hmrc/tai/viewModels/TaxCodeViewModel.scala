/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.tai.viewModels

import play.api.Play.current
import play.api.i18n.Messages

import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.domain.income.{BasisOperation, TaxCodeIncome, Week1Month1BasisOperation}
import uk.gov.hmrc.tai.viewModels.TaxCodeDescriptor._
import uk.gov.hmrc.tai.model.domain.income.{TaxCodeIncome, Week1Month1BasisOperation}
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.urls.Link

import scala.collection.immutable.ListMap

case class TaxCodeViewModel(title: String,
                            mainHeading: String,
                            ledeMessage: String,
                            taxCodeDetails: Seq[DescriptionListViewModel])

case class TaxCodeDescription(taxCode: String, basisOperation: BasisOperation, scottishTaxRateBands: Map[String, BigDecimal])

object TaxCodeViewModel extends ViewModelHelper {

  def apply(taxCodeIncomes: Seq[TaxCodeIncome], scottishTaxRateBands: Map[String, BigDecimal])(implicit messages: Messages): TaxCodeViewModel = {

    val explanationRules: Seq[TaxCodeDescription => ListMap[String, String]] = Seq(scottishTaxCodeExplanation,
      untaxedTaxCodeExplanation,
      fetchTaxCodeExplanation,
      emergencyTaxCodeExplanation)

    val descriptionListViewModels = taxCodeIncomes.map { taxCodeIncome =>
      val taxCode = taxCodeIncome.taxCodeWithEmergencySuffix
      val taxDescription = TaxCodeDescription(taxCodeIncome.taxCode, taxCodeIncome.basisOperation, scottishTaxRateBands)
      val explanation = explanationRules.foldLeft(ListMap[String, String]())((expl, rule) => expl ++ rule(taxDescription))
      DescriptionListViewModel(Messages("tai.taxCode.subheading", taxCodeIncome.name, taxCode), explanation)
    }

    val taxCodesPrefix = if (taxCodeIncomes.size > 1) Messages("tai.taxCode.multiple.code.title.pt1") else Messages("tai.taxCode.single.code.title.pt1")

    val title = s"$taxCodesPrefix $currentTaxYearRange"
    val mainHeading = s"$taxCodesPrefix $currentTaxYearRangeHtmlNonBreak"
    val ledeMessage = if (taxCodeIncomes.size > 1) Messages("tai.taxCode.multiple.info") else Messages("tai.taxCode.single.info")

    TaxCodeViewModel(title, mainHeading, ledeMessage, descriptionListViewModels)
  }

}

object TaxCodeDescriptor {

  val TaxAmountFactor = 10
  val EmergencyTaxCode = "X"

  def scottishTaxCodeExplanation(implicit messages: Messages) = (taxCodeDescription: TaxCodeDescription) => {
    val scottishRegex = "^S".r
    val taxCode = taxCodeDescription.taxCode
    scottishRegex.findFirstIn(taxCode) match {
      case Some(code) => ListMap(code -> Messages(s"tai.taxCode.$code",
        Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value=Some(Messages("tai.taxCode.scottishIncomeText.link"))).toHtml))
      case _ => ListMap[String, String]()
    }
  }

  def untaxedTaxCodeExplanation(implicit messages: Messages) = (taxCodeDescription: TaxCodeDescription) => {
    val untaxedRegex = "K".r
    val taxCode = taxCodeDescription.taxCode
    untaxedRegex.findFirstIn(taxCode) match {
      case Some(code) =>
        val amount = taxAmount(taxCode)
        val messageAmount = MoneyPounds(amount * TaxAmountFactor, 0).quantity
        ListMap(code -> Messages(s"tai.taxCode.$code"),
          amount.toString -> Messages(s"tai.taxCode.untaxedAmount", messageAmount))
      case _ => ListMap[String, String]()
    }
  }

  def fetchTaxCodeExplanation(implicit messages: Messages) = (taxCodeDescription: TaxCodeDescription) => {
    val codeExplanation = suffixTaxCodeExplanation(messages)(taxCodeDescription)

    if (codeExplanation.isEmpty)
      standAloneTaxCodeExplanation(messages)(taxCodeDescription)
    else
      codeExplanation
  }

  def standAloneTaxCodeExplanation(implicit messages: Messages) = (taxCodeDescription: TaxCodeDescription) => {
    val standAloneRegex = "0T|BR|NT".r
    val scottishStandAloneRegex = "D0|D1|D2|D3|D4|D5|D6|D7|D8".r

    val taxCode = taxCodeDescription.taxCode
    (standAloneRegex.findFirstIn(taxCode), scottishStandAloneRegex.findFirstIn(taxCode)) match {
      case (Some(code), None) => ListMap(code -> Messages(s"tai.taxCode.$code"))
      case (None, Some(code)) => ListMap(code -> Messages(s"tai.taxCode.DX", taxCodeDescription.scottishTaxRateBands.getOrElse(code, BigDecimal(0))))
      case _ => ListMap[String, String]()
    }
  }

  def suffixTaxCodeExplanation(implicit messages: Messages) = (taxCodeDescription: TaxCodeDescription) => {
    val suffixRegex = """L|M|\d[0-9]*N|\d[1-9]T|\d+0T|[1-9]T""".r
    val taxCode = taxCodeDescription.taxCode
    suffixRegex.findFirstIn(taxCode) match {
      case Some(code) =>
        val amount = taxAmount(taxCode)
        val messageAmount = MoneyPounds(amount * TaxAmountFactor, 0).quantity
        ListMap(amount.toString -> Messages(s"tai.taxCode.amount", messageAmount),
          code.last.toString -> Messages(s"tai.taxCode.${code.last.toString}"))
      case _ => ListMap[String, String]()
    }
  }

  def taxAmount(taxCode: String): Int = {
    val digitsRegex = """(\d+)""".r
    val amount = digitsRegex.findAllIn(taxCode).toList.headOption
    amount.map(_.toInt).getOrElse(0)
  }

  def emergencyTaxCodeExplanation(implicit messages: Messages) = (taxCodeDescription: TaxCodeDescription) => {
    taxCodeDescription.basisOperation match {
      case Week1Month1BasisOperation => ListMap(EmergencyTaxCode -> Messages("tai.taxCode.X"))
      case _ => ListMap[String, String]()
    }
  }
}
