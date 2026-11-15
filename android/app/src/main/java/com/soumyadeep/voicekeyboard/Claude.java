package com.soumyadeep.voicekeyboard;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Sends dictated text to the Anthropic API and returns the repaired text. */
final class Claude {

    static final String DEFAULT_MODEL = "claude-haiku-5-5";

    private Claude() {
    }

    static String prompt(String raw, String target) {
        String lang = (target == null || target.isEmpty())
                ? "Write the result in the same language and script the speaker used. If they mix languages, keep their mix."
                : "Write the result in " + target + ", in its normal script, whatever language was spoken.";
        return "You repair text that came from speech-to-text dictation. The speaker may talk in any language, "
                + "may stutter, repeat words, use fillers, restart sentences, trail off, or be misheard by the recognizer.\n\n"
                + "Rewrite the text so it says clearly what the speaker meant:\n"
                + "- remove fillers, stutters, repeats and false starts\n"
                + "- when they correct themselves, keep only the corrected version\n"
                + "- replace words that were clearly misheard with the word that fits the context\n"
                + "- add punctuation and capitals\n"
                + "- keep their meaning, tone and every fact; do not add new ideas, do not answer or comment on the text\n"
                + "- " + lang + "\n\n"
                + "Reply with the repaired text only, no quotes and no explanation.\n\n"
                + "Dictated text:\n<<<\n" + raw + "\n>>>";
    }

    static String fix(String key, String model, String raw, String target) throws Exception {
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt(raw, target));
        JSONArray messages = new JSONArray();
        messages.put(message);
        JSONObject body = new JSONObject();
        body.put("model", (model == null || model.trim().isEmpty()) ? DEFAULT_MODEL : model.trim());
        body.put("max_tokens", 2048);
        body.put("messages", messages);

        HttpURLConnection c = (HttpURLConnection) new URL("https://api.anthropic.com/v1/messages").openConnection();
        try {
            c.setRequestMethod("POST");
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setDoOutput(true);
            c.setRequestProperty("content-type", "application/json");
            c.setRequestProperty("x-api-key", key);
            c.setRequestProperty("anthropic-version", "2023-06-01");
            OutputStream os = c.getOutputStream();
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            os.close();

            int code = c.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
            String text = in == null ? "" : read(in);
            if (code < 200 || code >= 300) {
                throw new Exception("HTTP " + code + " " + text);
            }
            JSONArray content = new JSONObject(text).getJSONArray("content");
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < content.length(); i++) {
                JSONObject block = content.getJSONObject(i);
                if ("text".equals(block.optString("type"))) {
                    out.append(block.optString("text"));
                }
            }
            return out.toString().trim();
        } finally {
            c.disconnect();
        }
    }

    private static String read(InputStream in) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) > 0) {
            buf.write(chunk, 0, n);
        }
        in.close();
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }
}
