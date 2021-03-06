/*
 * Copyright (c) 2018 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.andstatus.app.data.checker;

import org.andstatus.app.account.MyAccount;
import org.andstatus.app.data.DataUpdater;
import org.andstatus.app.data.DbUtils;
import org.andstatus.app.data.DownloadStatus;
import org.andstatus.app.data.MyQuery;
import org.andstatus.app.database.table.NoteTable;
import org.andstatus.app.net.social.Actor;
import org.andstatus.app.net.social.Audience;
import org.andstatus.app.origin.Origin;
import org.andstatus.app.service.MyServiceManager;
import org.andstatus.app.util.I18n;
import org.andstatus.app.util.MyHtml;
import org.andstatus.app.util.RelativeTime;
import org.andstatus.app.util.TriState;

/**
 * @author yvolk@yurivolkov.com
 */
class CheckAudience extends DataChecker {

    @Override
    long fixInternal(boolean countOnly) {
        return myContext.origins().collection().stream().mapToInt(o -> fixOneOrigin(o, countOnly)).sum();
    }

    private static class FixSummary {
        long rowsCount = 0;
        int toFixCount = 0;
    }

    private int fixOneOrigin(Origin origin, boolean countOnly) {
        MyAccount ma = myContext.accounts().getFirstSucceededForOrigin(origin);
        if (ma.isEmpty()) return 0;

        DataUpdater dataUpdater = new DataUpdater(ma);

        String sql = "SELECT " + NoteTable._ID + ", " +
                NoteTable.INS_DATE + ", " +
                NoteTable.PUBLIC + ", " +
                NoteTable.CONTENT + ", " +
                NoteTable.ORIGIN_ID + ", " +
                NoteTable.AUTHOR_ID + ", " +
                NoteTable.IN_REPLY_TO_ACTOR_ID +
                " FROM " + NoteTable.TABLE_NAME +
                " WHERE " + NoteTable.ORIGIN_ID + "=" + origin.getId() +
                " AND " + NoteTable.NOTE_STATUS + "=" + DownloadStatus.LOADED.save() +
                " ORDER BY " + NoteTable._ID + " DESC" +
                (includeLong ? "" : " LIMIT 0, 1000");
        FixSummary summary = MyQuery.foldLeft(myContext, sql, new FixSummary(), s -> cursor -> {
            s.rowsCount++;
            long noteId = DbUtils.getLong(cursor, NoteTable._ID);
            long insDate = DbUtils.getLong(cursor, NoteTable.INS_DATE);
            TriState isPublic = DbUtils.getTriState(cursor, NoteTable.PUBLIC);
            String content = DbUtils.getString(cursor, NoteTable.CONTENT);
            Actor author = Actor.fromId(origin, DbUtils.getLong(cursor, NoteTable.AUTHOR_ID));
            Actor inReplyToActor = Actor.fromId(origin, DbUtils.getLong(cursor, NoteTable.IN_REPLY_TO_ACTOR_ID));

            Audience audience = Audience.fromNoteId(origin, noteId, isPublic);
            audience.extractActorsFromContent(content, author, inReplyToActor);
            if (!countOnly) {
                audience.getActors().stream().filter(a -> a.actorId == 0).forEach(actor ->
                    dataUpdater.updateObjActor(ma.getActor().update(actor), 0)
                );
            }
            if (audience.save(myContext, origin, noteId, isPublic, countOnly)) {
                s.toFixCount += 1;
            }
            if (logger.loggedMoreSecondsAgoThan(PROGRESS_REPORT_PERIOD_SECONDS)) {
                logger.logProgress(origin.getName() + ": need to fix " + s.toFixCount +
                        " of " + s.rowsCount + " audiences;\n" +
                        RelativeTime.getDifference(myContext.context(), insDate) + ", " +
                        I18n.trimTextAt(MyHtml.fromHtml(content), 120));
                MyServiceManager.setServiceUnavailable();
            }
            return s;
        });

        logger.logProgress(origin.getName() + ": " +
                (summary.toFixCount == 0
                ? "No changes to Audience were needed. " + summary.rowsCount + " notes"
                : (countOnly ? "Need to update " : "Updated") + " Audience for " + summary.toFixCount +
                        " of " + summary.rowsCount + " notes"));
        DbUtils.waitMs(this, 1000);
        return summary.toFixCount;
    }
}
