/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.rrm.ehour.ui.timesheet.panel;

import com.google.common.collect.Lists;
import net.rrm.ehour.data.DateRange;
import net.rrm.ehour.domain.*;
import net.rrm.ehour.project.status.ProjectAssignmentStatus;
import net.rrm.ehour.timesheet.dto.WeekOverview;
import net.rrm.ehour.timesheet.service.IOverviewTimesheet;
import net.rrm.ehour.timesheet.service.IPersistTimesheet;
import net.rrm.ehour.ui.common.BaseSpringWebAppTester;
import net.rrm.ehour.ui.common.session.EhourWebSession;
import net.rrm.ehour.ui.timesheet.dto.Timesheet;
import net.rrm.ehour.user.service.UserService;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.FormTester;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class TimesheetPanelTest extends BaseSpringWebAppTester {
    private static final String TIMESHEET_PATH = "panel:timesheetFrame:timesheetFrame_body:timesheetForm";
    private static final String DAY1_PATH = "blueFrame:blueFrame_body:customers:0:rows:0:day1";
    private static final String DAY1_FULL_PATH = TIMESHEET_PATH + ":" + DAY1_PATH;
    private static final User USER = new User(1);

    private IPersistTimesheet persistTimesheet;
    private IOverviewTimesheet overviewTimesheet;
    private UserService userService;

    @Before
    public void setup() {
        getConfig().setCompleteDayHours(8l);
        EhourWebSession.getSession().reloadConfig();

        persistTimesheet = createMock(IPersistTimesheet.class);
        getMockContext().putBean(persistTimesheet);

        overviewTimesheet = createMock(IOverviewTimesheet.class);
        getMockContext().putBean(overviewTimesheet);

        userService = createMock(UserService.class);
        getMockContext().putBean("userService", userService);
    }

    @Test
    public void addDayComment() {
        startAndReplayWithDefaultWeekOverview();

        final String comment = "commentaar";

        ModalWindow window = openCommentWindow(DAY1_FULL_PATH);
        assertTrue(window.isShown());

        FormTester timesheetFormTester = tester.newFormTester(TIMESHEET_PATH);
        timesheetFormTester.setValue(DAY1_PATH + ":dayWin:content:comment", comment);

        tester.executeAjaxEvent(DAY1_FULL_PATH + ":dayWin:content:comment", "onchange");
        tester.executeAjaxEvent(DAY1_FULL_PATH + ":dayWin:content:submit", "onclick");

        Timesheet timesheet = (Timesheet) tester.getComponentFromLastRenderedPage("panel").getDefaultModelObject();
        assertEquals(comment, timesheet.getTimesheetEntries().get(0).getComment());

        tester.assertNoErrorMessage();
    }

    private ModalWindow openCommentWindow(String path) {
        ModalWindow window = (ModalWindow) tester.getComponentFromLastRenderedPage(path + ":dayWin");
        tester.executeAjaxEvent(path + ":dayLink", "onclick");
        return window;
    }

    @Test
    public void addDayCommentCancelled() {
        startAndReplayWithDefaultWeekOverview();

        final String comment = "commentaar";

        clickDay1();

        FormTester formTester = tester.newFormTester(TIMESHEET_PATH);
        formTester.setValue(DAY1_PATH + ":dayWin:content:comment", comment);

        tester.executeAjaxEvent(DAY1_FULL_PATH + ":dayWin:content:comment", "onchange");
        tester.executeAjaxEvent(DAY1_FULL_PATH + ":dayWin:content:cancel", "onclick");

        Timesheet timesheet = (Timesheet) tester.getComponentFromLastRenderedPage("panel").getDefaultModelObject();
        assertNull(timesheet.getTimesheetEntries().get(0).getComment());

        tester.assertNoErrorMessage();
    }

    private void clickDay1() {
        tester.executeAjaxEvent(DAY1_FULL_PATH + ":dayLink", "onclick");
    }

    @Test
    public void shouldBookAllHours() {
        startAndReplayWithDefaultWeekOverview();

        tester.executeAjaxEvent(TIMESHEET_PATH + ":blueFrame:blueFrame_body:customers:0:rows:0:bookWholeWeek", "onclick");
        tester.assertNoErrorMessage();

        Label grandTotalLabel = (Label) tester.getComponentFromLastRenderedPage(TIMESHEET_PATH + ":blueFrame:blueFrame_body:grandTotal");
        assertEquals(40f, (Float) grandTotalLabel.getDefaultModelObject(), 0.01f);

        tester.assertNoErrorMessage();
    }

    @Test
    public void updateCounts() {
        startAndReplayWithDefaultWeekOverview();

        FormTester formTester = tester.newFormTester(TIMESHEET_PATH);

        formTester.setValue(DAY1_PATH + ":day", "12");
        tester.executeAjaxEvent(DAY1_FULL_PATH + ":day", "onblur");
        tester.assertNoErrorMessage();
        tester.assertContains(DAY1_PATH + ":day");

        Label grandTotalLabel = (Label) tester.getComponentFromLastRenderedPage(TIMESHEET_PATH + ":blueFrame:blueFrame_body:grandTotal");
        assertEquals(12f, (Float) grandTotalLabel.getDefaultModelObject(), 0.01f);
    }

    @Test
    public void moveToNextWeek() {
        startAndReplayWithDefaultWeekOverview();

        tester.executeAjaxEvent("panel:timesheetFrame:title:nextWeek", "onclick");

        Calendar cal = getWebApp().getSession().getNavCalendar();

        Calendar now = GregorianCalendar.getInstance();
        now.add(Calendar.DAY_OF_YEAR, 1);

        assertTrue(now.getTime().before(cal.getTime()));
    }

    @Test
    public void shouldNotResendUnmodifiedEntries() {
        startAndReplayWithDefaultWeekOverview();

        FormTester formTester = tester.newFormTester(TIMESHEET_PATH);

        formTester.setValue(DAY1_PATH + ":day", "12");
        tester.executeAjaxEvent(DAY1_FULL_PATH + ":day", "onblur");
        tester.assertNoErrorMessage();
        tester.assertContains(DAY1_PATH + ":day");

        //changing another field should not resend the unmodified day1
        formTester.setValue("blueFrame:blueFrame_body:customers:0:rows:0:day2:day", "8");
        tester.executeAjaxEvent(TIMESHEET_PATH + ":blueFrame:blueFrame_body:customers:0:rows:0:day2:day", "onblur");
        tester.assertNoErrorMessage();
        tester.assertContains("blueFrame:blueFrame_body:customers:0:rows:0:day2:day");
    }

    @Test
    public void shouldResetErrorState() {
        startAndReplayWithDefaultWeekOverview();

        FormTester formTester = tester.newFormTester(TIMESHEET_PATH);

        formTester.setValue(DAY1_PATH + ":day", "12");
        tester.executeAjaxEvent(DAY1_FULL_PATH + ":day", "onblur");
        tester.assertNoErrorMessage();
        tester.assertContains(DAY1_PATH + ":day");
        tester.assertContainsNot("color: #ff0000");

        formTester.setValue(DAY1_PATH + ":day", "ff");
        tester.executeAjaxEvent(DAY1_FULL_PATH + ":day", "onblur");
        tester.assertContains(DAY1_PATH + ":day");
        tester.assertContains("color: #ff0000");
        tester.assertErrorMessages("day.IConverter.Float");

        formTester.setValue(DAY1_PATH + ":day", "1");
        tester.executeAjaxEvent(DAY1_FULL_PATH + ":day", "onblur");
        tester.assertContains(DAY1_PATH + ":day");
        tester.assertContainsNot("color: #ff0000");
    }


    @SuppressWarnings("unchecked")
    @Test
    public void shouldSubmitSuccessful() {
        expect(persistTimesheet.persistTimesheetWeek(isA(Collection.class), isA(TimesheetComment.class), isA(DateRange.class)))
                .andReturn(new ArrayList<ProjectAssignmentStatus>());

        startAndReplayWithDefaultWeekOverview();

        tester.executeAjaxEvent(TIMESHEET_PATH + ":commentsFrame:commentsFrame_body:submitButton", "onclick");

        tester.assertNoErrorMessage();
    }

    @Test
    public void shouldDisableInputForLockedDays() {
        Date lockedDay = new LocalDate().plusDays(1).toDate();

        startAndReplayWithLockedDays(Arrays.asList(lockedDay));

        tester.assertComponent(TIMESHEET_PATH + ":blueFrame:blueFrame_body:customers:0:rows:0:day2:day", Label.class);
        tester.assertComponent(TIMESHEET_PATH + ":blueFrame:blueFrame_body:customers:0:rows:0:day3:day", TimesheetTextField.class);

        tester.assertNoErrorMessage();
    }

    @Test
    public void shouldDisableCommentInputForLockedDays() {
        Date lockedDay = new LocalDate().plusDays(1).toDate();

        startAndReplayWithLockedDays(Arrays.asList(lockedDay));

        openCommentWindow(TIMESHEET_PATH + ":blueFrame:blueFrame_body:customers:0:rows:0:day2");

        tester.assertComponent(TIMESHEET_PATH + ":blueFrame:blueFrame_body:customers:0:rows:0:day2:dayWin:content:comment", Label.class);

        tester.assertNoErrorMessage();
    }


    @After
    public void verifyMocks() {
        verify(overviewTimesheet);
        verify(userService);
        verify(persistTimesheet);

    }

    private void startAndReplayWithDefaultWeekOverview() {
        startAndReplayWithLockedDays(Lists.<Date>newArrayList());
    }

    private void startAndReplayWithLockedDays(List<Date> lockedDays) {
        withDefaultWeekOverview(lockedDays);

        replay(overviewTimesheet);
        replay(userService);
        replay(persistTimesheet);

        tester.startComponentInPage(new TimesheetPanel("panel", USER, new GregorianCalendar()));
    }

    private void withDefaultWeekOverview(List<Date> lockedDates) {
        Calendar now = GregorianCalendar.getInstance();
        now.add(Calendar.DAY_OF_WEEK, 7);

        DateRange nextWeekRange = new DateRange(new Date(), now.getTime());

        List<TimesheetEntry> entries = Arrays.asList(TimesheetEntryObjectMother.createTimesheetEntry(1, new Date(), 5));
        List<ProjectAssignment> assignments = Arrays.asList(ProjectAssignmentObjectMother.createProjectAssignment(1));

        WeekOverview overview = new WeekOverview(entries, null, assignments, nextWeekRange, USER, lockedDates);
        expect(overviewTimesheet.getWeekOverview(isA(User.class), isA(Calendar.class))).andReturn(overview);
    }
}
