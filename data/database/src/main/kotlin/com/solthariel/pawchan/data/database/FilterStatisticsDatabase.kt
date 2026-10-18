package com.solthariel.pawchan.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.solthariel.pawchan.domain.filter.ContentFilterBoardStatistics
import com.solthariel.pawchan.domain.filter.ContentFilterHit
import com.solthariel.pawchan.domain.filter.ContentFilterStatistics

/** Separate database: no change or destructive migration to workspace/Room storage. */
internal class FilterStatisticsDatabase(
    context: Context,
) : SQLiteOpenHelper(context, "content-filter-statistics.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE hits (rule TEXT NOT NULL, site TEXT NOT NULL, board TEXT NOT NULL, " +
                "thread INTEGER NOT NULL, post INTEGER NOT NULL, op INTEGER NOT NULL, time INTEGER NOT NULL, " +
                "PRIMARY KEY(rule,site,board,thread,post))",
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        error("Unsupported statistics migration: $oldVersion -> $newVersion")
    }
}

internal fun SQLiteDatabase.insertFilterHits(hits: List<ContentFilterHit>): Boolean {
    var inserted = false
    val now = System.currentTimeMillis()
    beginTransaction()
    try {
        hits.forEach { hit ->
            val values =
                ContentValues().apply {
                    put("rule", hit.ruleId)
                    put("site", hit.thread.board.site.value)
                    put("board", hit.thread.board.board.value)
                    put("thread", hit.thread.thread.value)
                    put("post", hit.postId)
                    put("op", if (hit.originalPost) 1 else 0)
                    put("time", now)
                }
            if (insertWithOnConflict("hits", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1L) inserted = true
        }
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
    return inserted
}

internal fun SQLiteDatabase.filterStatistics(): ContentFilterStatistics {
    val rules = linkedMapOf<String, Long>()
    rawQuery("SELECT rule, COUNT(*) AS hit_count FROM hits GROUP BY rule", null).use { cursor ->
        while (cursor.moveToNext()) {
            rules[cursor.getString(cursor.getColumnIndexOrThrow("rule"))] =
                cursor.getLong(cursor.getColumnIndexOrThrow("hit_count"))
        }
    }
    val boards = mutableListOf<ContentFilterBoardStatistics>()
    rawQuery(
        "SELECT site, board, SUM(op) AS op_hits, SUM(1-op) AS reply_hits, MAX(time) AS last_hit " +
            "FROM hits GROUP BY site, board ORDER BY COUNT(*) DESC",
        null,
    ).use { cursor ->
        while (cursor.moveToNext()) {
            boards.add(
                ContentFilterBoardStatistics(
                    cursor.getString(cursor.getColumnIndexOrThrow("site")),
                    cursor.getString(cursor.getColumnIndexOrThrow("board")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("op_hits")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("reply_hits")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("last_hit")),
                ),
            )
        }
    }
    return ContentFilterStatistics(rules, boards)
}
