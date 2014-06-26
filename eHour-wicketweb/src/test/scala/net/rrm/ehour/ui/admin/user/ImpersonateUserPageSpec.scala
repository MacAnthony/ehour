package net.rrm.ehour.ui.admin.user

import java.util
import java.util.Calendar

import com.google.common.collect.Lists
import net.rrm.ehour.AbstractSpringWebAppSpec
import net.rrm.ehour.domain.{User, UserObjectMother}
import net.rrm.ehour.timesheet.dto.WeekOverview
import net.rrm.ehour.timesheet.service.{IOverviewTimesheet, IPersistTimesheet}
import net.rrm.ehour.ui.common.panel.entryselector.EntrySelectedEvent
import net.rrm.ehour.ui.common.session.EhourWebSession
import net.rrm.ehour.user.service.UserService
import org.apache.wicket.ajax.AjaxRequestTarget
import org.apache.wicket.event.Broadcast
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter

class ImpersonateUserPageSpec extends AbstractSpringWebAppSpec with BeforeAndAfter {
  val PathToImpersonateLink = "frame:frame_body:border:border_body:content:impersonateLink"

  "Impersonate User Page" should {
    val service = mockService[UserService]
    val overviewTimesheet = mockService[IOverviewTimesheet]

    mockService[IPersistTimesheet]

    before {
      reset(service, overviewTimesheet)
      when(service.getActiveUsers).thenReturn(util.Arrays.asList(new User("thies", "thies")))
    }

    "render" in {
      tester.startPage(classOf[ImpersonateUserPage])

      tester.assertNoErrorMessage()
      tester.assertNoInfoMessage()
    }

    "handle entryselectedevent by showing that user in the content panel" in {
      val user = UserObjectMother.createUser
      when(service.getUser(1)).thenReturn(user)

      tester.startPage(classOf[ImpersonateUserPage])

      val target = mock[AjaxRequestTarget]

      val page = tester.getLastRenderedPage
      page.send(page, Broadcast.DEPTH, EntrySelectedEvent(1, target))

      tester.assertNoErrorMessage()
      tester.assertNoInfoMessage()

      verify(service).getUser(1)
      verify(target).add(any())
    }

    "impersonate user" in {
      prep(service, overviewTimesheet)

      tester.startPage(classOf[ImpersonateUserPage])

      val target = mock[AjaxRequestTarget]

      val page = tester.getLastRenderedPage
      page.send(page, Broadcast.DEPTH, EntrySelectedEvent(1, target))

      tester.clickLink(PathToImpersonateLink)

      EhourWebSession.getSession shouldBe 'impersonating
    }

    "cannot impersonate user when user is already impersonating" in {
      prep(service, overviewTimesheet)

      tester.startPage(classOf[ImpersonateUserPage])

      val target = mock[AjaxRequestTarget]

      val page = tester.getLastRenderedPage
      page.send(page, Broadcast.DEPTH, EntrySelectedEvent(1, target))

      tester.clickLink(PathToImpersonateLink)

      tester.startPage(classOf[ImpersonateUserPage])
      val page2 = tester.getLastRenderedPage
      page2.send(page2, Broadcast.DEPTH, EntrySelectedEvent(1, target))

      tester.getLastRenderedPage.get(PathToImpersonateLink) should be(null)
    }
  }

  def prep(service: UserService, overviewTimesheet: IOverviewTimesheet) {
    val user = UserObjectMother.createUser

    when(overviewTimesheet.getWeekOverview(any[User], any[Calendar])).thenReturn(new WeekOverview(Lists.newArrayList(), Lists.newArrayList()))

    when(service.getUser(1)).thenReturn(user)
  }
}