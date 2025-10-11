package fun.rich.display.screens.clickgui.components.implement.autobuy.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AuctionUtils {
    public static final Pattern funTimePricePattern = Pattern.compile("\\$(\\d+(?:[\\s,]\\d{3})*(?:\\.\\d{2})?)");

    public static int getPrice(ItemStack stack) {
        var tag = stack.getComponents();
        if (tag == null) return -1;

        String priceStr = null;
        String componentString = tag.toString();
        priceStr = StringUtils.substringBetween(componentString, "literal{ $", "}[style={color=green}]");

        if (priceStr == null || priceStr.isEmpty()) {
            String customName = stack.getName().getString();
            if (customName != null) {
                java.util.regex.Matcher matcher = funTimePricePattern.matcher(customName);
                if (matcher.find()) {
                    priceStr = matcher.group(1);
                }
            }
        }

        if (priceStr == null || priceStr.isEmpty()) return -1;

        try {
            priceStr = priceStr.replaceAll("[\\s,]", "");
            return Integer.parseInt(priceStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String cleanString(String str) {
        if (str == null) return "";
        return str.toLowerCase().trim()
                .replaceAll("§.", "")
                .replaceAll("[^a-zа-яё0-9\\s\\[\\]★]", "")
                .replaceAll("\\s+", " ");
    }

    public static boolean compareItem(ItemStack a, ItemStack b) {
        if (a.getItem() != b.getItem()) return false;

        String aName = a.getName().getString();
        aName = funTimePricePattern.matcher(aName).replaceAll("").trim();
        String bName = b.getName().getString();

        String aNameClean = cleanString(aName);
        String bNameClean = cleanString(bName);

        var aLore = a.get(DataComponentTypes.LORE);
        var bLoreComp = b.get(DataComponentTypes.LORE);
        boolean hasLore = bLoreComp != null && !bLoreComp.lines().isEmpty();

        if (hasLore) {
            List<Text> expectedLore = bLoreComp.lines();

            if (aLore == null || aLore.lines().isEmpty()) {
                if (!aNameClean.contains(bNameClean)) return false;
            } else {
                List<String> auctionLoreStrings = aLore.lines().stream()
                        .map(text -> cleanString(text.getString()))
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                String auctionLoreJoined = String.join(" ", auctionLoreStrings);

                boolean hasOriginalMarker = false;
                for (String line : auctionLoreStrings) {
                    if (line.contains("оригинальный предмет") || line.contains("★")) {
                        hasOriginalMarker = true;
                        break;
                    }
                }

                int matchCount = 0;
                int requiredMatches = 0;

                for (Text expected : expectedLore) {
                    String expectedStr = cleanString(expected.getString());
                    if (expectedStr.isEmpty()) continue;

                    boolean isOriginalMarker = expectedStr.contains("оригинальный предмет") || expectedStr.contains("★");

                    if (isOriginalMarker) {
                        if (!hasOriginalMarker) {
                            return false;
                        }
                        matchCount++;
                        requiredMatches++;
                        continue;
                    }

                    requiredMatches++;

                    boolean found = false;
                    for (String auctionLine : auctionLoreStrings) {
                        if (auctionLine.contains(expectedStr) || expectedStr.contains(auctionLine)) {
                            found = true;
                            break;
                        }
                    }

                    if (!found && auctionLoreJoined.contains(expectedStr)) {
                        found = true;
                    }

                    if (found) {
                        matchCount++;
                    }
                }

                if (matchCount < requiredMatches) {
                    return false;
                }
            }
        } else {
            if (!aNameClean.contains(bNameClean) && !bNameClean.contains(aNameClean)) {
                return false;
            }
        }

        return true;
    }


}