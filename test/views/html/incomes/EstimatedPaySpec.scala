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

package views.html.incomes

import org.scalatest.mock.MockitoSugar
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class EstimatedPaySpec extends TaiViewSpec with MockitoSugar {

  "Estimated pay view without bonusOverTime" should {
    behave like pageWithBackLink
    behave like pageWithCombinedHeader(
      messages("tai.estimatedPay.preHeading"),
      messages("tai.estimatedPay.title"))
  }
  "Estimated pay view with bonusOverTime" should {
    val testView: Html = views.html.incomes.estimatedPay(None,None,id,true,None,None,false,None)
    doc(testView) must haveBackLink
  }

  val id = 1

  override def view: Html = views.html.incomes.estimatedPay(None,None,id,false,None,None,false,None)
}
