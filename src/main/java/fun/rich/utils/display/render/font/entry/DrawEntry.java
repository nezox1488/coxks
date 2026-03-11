package fun.rich.utils.display.render.font.entry;

import fun.rich.utils.display.render.font.glyph.Glyph;

public record DrawEntry(float atX, float atY, int color, Glyph toDraw) {
}
