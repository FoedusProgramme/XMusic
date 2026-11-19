package com.xapps.media.xmusic.lyrics;

import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.util.Log;
import com.xapps.media.xmusic.models.LyricLine;
import com.xapps.media.xmusic.models.LyricSyllable;
import com.xapps.media.xmusic.models.LyricWord;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ESLParser {

    private static final String TAG = "ESLParserDBG";

    private static final Pattern OFFSET_PATTERN = Pattern.compile("\\[offset:([+-]?\\d+)\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern METADATA_IGNORE_PATTERN = Pattern.compile("^\\[(by|ar|ti|al|au|length|re):.*\\]$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ESL_WORD_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]([^\\[\r\n]*)");

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static long currentRequestId = 0;

    public interface LyricsListener {
        void onParsed(LyricsResult result);
    }

    public record LyricsResult(List<LyricLine> lines, boolean isSynced) {
    }

    /**
     * Asynchronously parses ESLyric formatted string.
     */
    public static void parse(String lyricsString, LyricsListener listener) {
        final long requestId = ++currentRequestId;
        EXECUTOR.execute(() -> {
            List<LyricLine> lines = parseString(lyricsString);
            boolean isSynced = false;
            for (LyricLine line : lines) {
                if (line.time > 0) {
                    isSynced = true;
                    break;
                }
            }
            final LyricsResult result = new LyricsResult(lines, isSynced);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (listener != null && requestId == currentRequestId) {
                    listener.onParsed(result);
                }
            });
        });
    }

    /**
     * Synchronously parses an ESLyric formatted string into a List of LyricLine objects.
     */
    public static List<LyricLine> parseString(String lyricsString) {
        if (lyricsString == null || lyricsString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try (InputStream is = new ByteArrayInputStream(lyricsString.getBytes(StandardCharsets.UTF_8))) {
            return parseStream(is);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing ESLyric string", e);
            return new ArrayList<>();
        }
    }

    /**
     * Synchronously parses an ESLyric InputStream into a List of LyricLine objects.
     */
    public static List<LyricLine> parseStream(InputStream inputStream) {
        List<LyricLine> result = new ArrayList<>();
        long globalOffset = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String rawLine;
            while ((rawLine = br.readLine()) != null) {
                String line = rawLine.trim();
                if (line.isEmpty() || METADATA_IGNORE_PATTERN.matcher(line).matches()) continue;

                Matcher om = OFFSET_PATTERN.matcher(line);
                if (om.find()) {
                    globalOffset = Long.parseLong(om.group(1));
                    continue;
                }

                LyricLine parsedLine = processLine(line, globalOffset);
                if (parsedLine != null) {
                    result.add(parsedLine);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading ESLyric stream", e);
        }

        if (!result.isEmpty()) {
            sortAndFinalizeLines(result);
        }

        return result;
    }

    /**
     * Processes a single ESLyric line containing inline [mm:ss.xxx] timestamps.
     */
    public static LyricLine processLine(String lineContent, long globalOffset) {
        if (lineContent == null || lineContent.trim().isEmpty()) return null;

        String t = lineContent.trim();
        int vocalType = 1;
        boolean isBackground = false;

        if (t.startsWith("bg:")) {
            isBackground = true;
            t = t.substring(3).trim();
        } else if (t.startsWith("[bg:") && t.endsWith("]")) {
            isBackground = true;
            t = t.substring(4, t.length() - 1).trim();
        }

        String lower = t.toLowerCase();
        if (lower.startsWith("v1:")) {
            t = t.substring(3).trim();
        } else if (lower.startsWith("v2:")) {
            vocalType = 2;
            t = t.substring(3).trim();
        }

        Matcher wm = ESL_WORD_PATTERN.matcher(t);
        List<Integer> timestamps = new ArrayList<>();
        List<String> fragments = new ArrayList<>();
        long explicitEnd = -1;

        while (wm.find()) {
            int ts = (int) (parseTimestamp(wm.group(1), wm.group(2), wm.group(3)) + globalOffset);
            String fragment = wm.group(4);

            if (fragment == null || fragment.isEmpty()) {
                explicitEnd = ts;
                continue;
            }

            timestamps.add(ts);
            fragments.add(fragment);
        }

        if (timestamps.isEmpty()) {
            return null;
        }

        long lineStartTime = timestamps.get(0);
        boolean isNonSpace = isNonSpaceLanguage(t);

        StringBuilder rawTextBuilder = new StringBuilder();
        List<LyricWord> words = new ArrayList<>();
        LyricWord currentWord = null;
        int cursor = 0;
        boolean lastHadTrailingSpace = false;

        for (int i = 0; i < fragments.size(); i++) {
            String frag = fragments.get(i);
            int cut = frag.length();
            while (cut > 0 && frag.charAt(cut - 1) == ' ') cut--;
            String core = frag.substring(0, cut);
            String trailing = frag.substring(cut);

            if (currentWord == null || lastHadTrailingSpace || isNonSpace) {
                currentWord = new LyricWord(cursor);
                words.add(currentWord);
            }

            LyricSyllable syl = new LyricSyllable(timestamps.get(i), core, cursor - currentWord.startIndex);
            if (i + 1 < timestamps.size()) {
                syl.endTime = timestamps.get(i + 1);
            } else {
                syl.endTime = (explicitEnd > 0) ? (int) explicitEnd : syl.startTime + 1000;
            }

            currentWord.syllables.add(syl);
            rawTextBuilder.append(core);
            cursor += core.length();

            if (!trailing.isEmpty()) {
                rawTextBuilder.append(trailing);
                cursor += trailing.length();
                lastHadTrailingSpace = true;
            } else {
                lastHadTrailingSpace = false;
            }
        }

        LyricLine line = new LyricLine((int) lineStartTime, new SpannableString(rawTextBuilder.toString()), words);
        line.vocalType = vocalType;
        line.isBackground = isBackground;
        return line;
    }

    /**
     * Checks whether a text string looks like ESLyric format (has multiple inline square bracket timestamps on a single line).
     */
    public static boolean isESLyric(String content) {
        if (content == null || content.isEmpty()) return false;
        int newlineIdx = content.indexOf('\n');
        String sampleLine = (newlineIdx != -1) ? content.substring(0, newlineIdx) : content;
        Matcher m = ESL_WORD_PATTERN.matcher(sampleLine);
        int count = 0;
        while (m.find()) {
            count++;
            if (count >= 2) return true;
        }
        return false;
    }

    private static long parseTimestamp(String min, String sec, String msStr) {
        int m = Integer.parseInt(min);
        int s = Integer.parseInt(sec);
        int ms = Integer.parseInt(msStr);
        if (msStr.length() == 2) {
            ms *= 10;
        } else if (msStr.length() == 1) {
            ms *= 100;
        }
        return (m * 60L + s) * 1000L + ms;
    }

    private static boolean isNonSpaceLanguage(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || block == Character.UnicodeBlock.HIRAGANA || block == Character.UnicodeBlock.KATAKANA || block == Character.UnicodeBlock.THAI) {
                return true;
            }
        }
        return false;
    }

    private static void sortAndFinalizeLines(List<LyricLine> lines) {
        Comparator<LyricLine> lineComparator = (l1, l2) -> {
            int t = Integer.compare(l1.time, l2.time);
            if (t != 0) return t;
            if (!l1.isLinkedBg && l2.isLinkedBg) return -1;
            if (l1.isLinkedBg && !l2.isLinkedBg) return 1;
            if (!l1.isRomaji && l2.isRomaji) return -1;
            if (l1.isRomaji && !l2.isRomaji) return 1;
            return 0;
        };
        lines.sort(lineComparator);

        for (int i = 1; i < lines.size(); i++) {
            LyricLine prev = lines.get(i - 1);
            LyricLine current = lines.get(i);
            if (!current.isWaitingDots && !prev.isWaitingDots && current.time == prev.time && current.vocalType == prev.vocalType) {
                if (!current.isBackground && !prev.isBackground && !current.isLinkedBg && !prev.isLinkedBg) {
                    current.isRomaji = true;
                }
            }
        }

        finalizeSyllableTimings(lines);
        lines.sort(lineComparator);
    }

    private static void finalizeSyllableTimings(List<LyricLine> lines) {
        final int GAP_THRESHOLD_MS = 600;
        final int INSTRUMENTAL_THRESHOLD_MS = 5000;
        List<LyricLine> injectedLines = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            LyricLine line = lines.get(i);
            if (line.words == null || line.words.isEmpty() || line.isWaitingDots) continue;

            for (int w = 0; w < line.words.size(); w++) {
                LyricWord word = line.words.get(w);
                for (int s = 0; s < word.syllables.size(); s++) {
                    LyricSyllable current = word.syllables.get(s);
                    LyricSyllable next = null;
                    if (s + 1 < word.syllables.size()) {
                        next = word.syllables.get(s + 1);
                    } else if (w + 1 < line.words.size() && line.words.get(w + 1).syllables != null && !line.words.get(w + 1).syllables.isEmpty()) {
                        next = line.words.get(w + 1).syllables.get(0);
                    }

                    if (next != null && !line.isSimpleLRC) {
                        int originalEnd = current.endTime;
                        int gap = next.startTime - originalEnd;
                        if (gap > 0 && gap <= GAP_THRESHOLD_MS) {
                            int halfGap = gap / 2;
                            current.endTime = originalEnd + halfGap;
                            next.startTime = next.startTime - (gap - halfGap);
                            current.nextStartTime = next.startTime;
                        } else {
                            current.endTime = Math.max(originalEnd, current.startTime);
                            current.nextStartTime = next.startTime;
                        }
                    } else {
                        current.endTime = Math.max(current.endTime, current.startTime);
                        current.nextStartTime = current.endTime;
                    }
                }
            }
        }

        long maxEndTimeSoFar = 0;
        LyricLine firstMain = null;
        for (LyricLine l : lines) {
            if (!l.isRomaji && !l.isLinkedBg && !l.isBackground && !l.isWaitingDots) {
                firstMain = l;
                break;
            }
        }

        if (firstMain != null && !firstMain.isSimpleLRC && firstMain.time >= INSTRUMENTAL_THRESHOLD_MS) {
            LyricLine breakLine = new LyricLine(0, new SpannableString(""), new ArrayList<>());
            breakLine.endTime = firstMain.time;
            breakLine.isWaitingDots = true;
            injectedLines.add(breakLine);
        }

        for (int i = 0; i < lines.size(); i++) {
            LyricLine current = lines.get(i);
            if (current.isRomaji || current.isWaitingDots) continue;
            long currentEnd = current.time;
            if (current.words != null && !current.words.isEmpty()) {
                LyricWord lastWord = current.words.get(current.words.size() - 1);
                if (lastWord.syllables != null && !lastWord.syllables.isEmpty()) {
                    currentEnd = lastWord.syllables.get(lastWord.syllables.size() - 1).endTime;
                }
            }
            maxEndTimeSoFar = Math.max(maxEndTimeSoFar, currentEnd);

            LyricLine next = null;
            for (int j = i + 1; j < lines.size(); j++) {
                LyricLine candidate = lines.get(j);
                if (!candidate.isRomaji && !candidate.isWaitingDots && candidate.time > current.time) {
                    next = candidate;
                    break;
                }
            }

            if (next != null && !current.isSimpleLRC && !next.isSimpleLRC) {
                if (next.time - maxEndTimeSoFar >= INSTRUMENTAL_THRESHOLD_MS) {
                    boolean exists = false;
                    for (LyricLine inj : injectedLines) {
                        if (inj.time == maxEndTimeSoFar && inj.endTime == next.time) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        LyricLine breakLine = new LyricLine((int) maxEndTimeSoFar, new SpannableString(""), new ArrayList<>());
                        breakLine.endTime = next.time;
                        breakLine.isWaitingDots = true;
                        injectedLines.add(breakLine);
                    }
                }
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            LyricLine current = lines.get(i);
            if (current.isWaitingDots) continue;
            long nextDistinctTime = -1;
            for (int j = i + 1; j < lines.size(); j++) {
                LyricLine candidate = lines.get(j);
                if (candidate.time > current.time && !candidate.isRomaji && !candidate.isLinkedBg && !candidate.isBackground) {
                    nextDistinctTime = candidate.time;
                    break;
                }
            }

            long naturalEnd = current.time;
            if (current.words != null && !current.words.isEmpty()) {
                LyricWord lastWord = current.words.get(current.words.size() - 1);
                if (lastWord.syllables != null && !lastWord.syllables.isEmpty()) {
                    naturalEnd = lastWord.syllables.get(lastWord.syllables.size() - 1).endTime;
                }
            }

            if (current.isSimpleLRC) {
                current.endTime = (nextDistinctTime > -1) ? (int) nextDistinctTime : current.time + 5000;
            } else {
                current.endTime = (int) naturalEnd;
            }
        }

        if (!injectedLines.isEmpty()) {
            lines.addAll(injectedLines);
        }
    }
}
