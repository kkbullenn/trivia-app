package com.triviaapp.util;

import org.json.JSONObject;

import java.util.Map;

/**
 * Utility helpers for projecting question content fields into JSON payloads.
 *
 * @author Jerry Xing
 */
public final class QuestionJsonUtils {

    private static final String[] QUESTION_CONTENT_KEYS = {
            "question_text",
            "answers_option",
            "answers_key",
            "points",
            "youtube_url"
    };

    private QuestionJsonUtils() {
    }

    public static void putQuestionContent(JSONObject target, Map<String, String> questionData) {
        if (target == null || questionData == null) {
            return;
        }
        for (String key : QUESTION_CONTENT_KEYS) {
            target.put(key, questionData.get(key));
        }
    }
}
