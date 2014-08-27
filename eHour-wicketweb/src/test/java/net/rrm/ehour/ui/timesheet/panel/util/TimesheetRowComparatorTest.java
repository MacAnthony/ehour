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

package net.rrm.ehour.ui.timesheet.panel.util;

import static org.junit.Assert.assertEquals;
import net.rrm.ehour.config.EhourConfigStub;
import net.rrm.ehour.domain.Activity;
import net.rrm.ehour.domain.ActivityMother;
import net.rrm.ehour.ui.timesheet.dto.TimesheetRow;
import net.rrm.ehour.ui.timesheet.util.TimesheetRowComparator;

import org.junit.Test;

/**
 * @author thies
 *
 */
public class TimesheetRowComparatorTest
{
    @Test
    public void testCompare()
    {
        Activity a1 = ActivityMother.createActivity(1);
        a1.getProject().setName("A");
        TimesheetRow rowA = new TimesheetRow(new EhourConfigStub());
        rowA.setActivity(a1);

        Activity a2 = ActivityMother.createActivity(1);
        a2.getProject().setName("B");
        TimesheetRow rowB = new TimesheetRow(new EhourConfigStub());
        rowB.setActivity(a2);

        assertEquals(-1, TimesheetRowComparator.INSTANCE.compare(rowA, rowB));
    }

}
