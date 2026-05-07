package com.xapps.media.xmusic.widget;

import android.graphics.Paint;
import android.text.StaticLayout;
import android.text.TextPaint;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Locale;

public final class LyricsGlyphHelper {

    public static final class GlyphUnit {
        public int start;
        public int end;
        public float x;
        public float width;
        public float center;
    }

    public static GlyphUnit[] buildGlyphs(String text, StaticLayout layout, float[] lineStarts, TextPaint paint) {
        if (text == null || text.isEmpty() || layout == null) return new GlyphUnit[0];

        ArrayList<GlyphUnit> list = new ArrayList<>();

        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.getDefault());
        iterator.setText(text);

        int start = iterator.first();
        int end = iterator.next();

        while (end != BreakIterator.DONE) {
            if (start < end) {
                GlyphUnit g = new GlyphUnit();
                g.start = start;
                g.end = end;

                int lineIndex = layout.getLineForOffset(start);

                float localStart = layout.getPrimaryHorizontal(start);
                float localEnd;

                if (end <= text.length()) {
                    localEnd = layout.getPrimaryHorizontal(end);
                } else {
                    localEnd = localStart + paint.measureText(text, start, end);
                }

                g.x = lineStarts[lineIndex] + localStart;
                g.width = Math.max(1f, localEnd - localStart);
                g.center = g.x + (g.width * 0.5f);

                list.add(g);
            }

            start = end;
            end = iterator.next();
        }

        return list.toArray(new GlyphUnit[0]);
    }

    public static float dp(float value, float density) {
        return value * density;
    }

    public static void applyGlow(Paint paint, float radius, int color) {
        paint.setShadowLayer(radius, 0f, 0f, color);
    }

    public static void clearGlow(Paint paint) {
        paint.clearShadowLayer();
    }
}