package neoproxy.neolinkmc.gui;

/**
 * NeoLinkMC LAN 配置界面共用的比例布局几何。
 *
 * <p>该布局沿用 vanilla/LSP LAN 界面的双列视觉语言，但每一行都归属于同一个内容块。
 * 空间足够时内容块居中；logical GUI 高度较小时，间距会整体压缩，因此窗口尺寸变化和
 * GUI scale 变化不会让底部控件与中部行发生碰撞。</p>
 */
public final class LanScreenLayout {
    public static final int MIN_BUTTON_WIDTH = 150;
    public static final int MIN_BUTTON_HEIGHT = 20;
    public static final int MIN_INPUT_WIDTH = 147;

    private static final int MAX_BUTTON_WIDTH = 260;
    private static final int MAX_BUTTON_HEIGHT = 32;
    private static final int MIN_SCREEN_MARGIN = 8;
    private static final int TEXT_LINE_HEIGHT = 10;
    private static final int BUTTON_TEXT_PADDING = 24;

    private LanScreenLayout() {
    }

    public static Positions calculate(int screenWidth, int screenHeight, int minimumTextWidth) {
        int horizontalMargin = clamp(screenWidth / 32, MIN_SCREEN_MARGIN, 32);
        int columnGap = clamp(screenWidth / 64, 8, 28);
        int buttonWidth = clamp(
                Math.max(MIN_BUTTON_WIDTH, minimumTextWidth + BUTTON_TEXT_PADDING),
                MIN_BUTTON_WIDTH,
                MAX_BUTTON_WIDTH
        );

        int availableWidth = Math.max(MIN_BUTTON_WIDTH * 2 + columnGap, screenWidth - horizontalMargin * 2);
        int maximumColumnWidth = Math.max(MIN_BUTTON_WIDTH, (availableWidth - columnGap) / 2);
        buttonWidth = Math.min(buttonWidth, maximumColumnWidth);

        int buttonHeight = clamp(screenHeight / 26, MIN_BUTTON_HEIGHT, MAX_BUTTON_HEIGHT);
        int inputWidth = Math.max(MIN_INPUT_WIDTH, buttonWidth - 3);
        int totalWidth = buttonWidth * 2 + columnGap;
        int leftColumnX = screenWidth / 2 - totalWidth / 2;
        int rightColumnX = leftColumnX + buttonWidth + columnGap;

        VerticalGaps gaps = VerticalGaps.preferred(screenHeight, buttonHeight);
        int fixedHeight = TEXT_LINE_HEIGHT * 3 + buttonHeight * 5;
        int availableHeight = Math.max(fixedHeight, screenHeight - MIN_SCREEN_MARGIN * 2);
        int gapHeight = gaps.total();

        if (fixedHeight + gapHeight > availableHeight) {
            double scale = Math.max(0.0D, (availableHeight - fixedHeight) / (double) gapHeight);
            gaps = gaps.scale(scale);
        }

        int contentHeight = fixedHeight + gaps.total();
        int top = Math.max(MIN_SCREEN_MARGIN, (screenHeight - contentHeight) / 2);

        int titleY = top;
        int topRowY = titleY + TEXT_LINE_HEIGHT + gaps.titleToTopRow;
        int playerSettingsY = topRowY + buttonHeight + gaps.topRowToPlayerTitle;
        int firstOptionRowY = playerSettingsY + TEXT_LINE_HEIGHT + gaps.playerTitleToFirstRow;
        int secondOptionRowY = firstOptionRowY + buttonHeight + gaps.optionRowGap;
        int inputLabelY = secondOptionRowY + buttonHeight + gaps.optionsToInputLabel;
        int inputRowY = inputLabelY + TEXT_LINE_HEIGHT + gaps.inputLabelToInputRow;
        int bottomRowY = inputRowY + buttonHeight + gaps.inputRowToBottomRow;

        return new Positions(
                screenWidth / 2,
                leftColumnX,
                rightColumnX,
                buttonWidth,
                buttonHeight,
                inputWidth,
                titleY,
                topRowY,
                playerSettingsY,
                firstOptionRowY,
                secondOptionRowY,
                inputLabelY,
                inputRowY,
                bottomRowY
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record VerticalGaps(
            int titleToTopRow,
            int topRowToPlayerTitle,
            int playerTitleToFirstRow,
            int optionRowGap,
            int optionsToInputLabel,
            int inputLabelToInputRow,
            int inputRowToBottomRow
    ) {
        static VerticalGaps preferred(int screenHeight, int buttonHeight) {
            return new VerticalGaps(
                    clamp(screenHeight / 23, 14, 26),
                    clamp(screenHeight / 10, 36, 76),
                    clamp(screenHeight / 18, 22, 42),
                    clamp(buttonHeight / 4, 4, 8),
                    clamp(screenHeight / 14, 24, 56),
                    clamp(buttonHeight / 2, 8, 14),
                    clamp(screenHeight / 28, 12, 24)
            );
        }

        VerticalGaps scale(double scale) {
            return new VerticalGaps(
                    scaled(titleToTopRow, scale),
                    scaled(topRowToPlayerTitle, scale),
                    scaled(playerTitleToFirstRow, scale),
                    scaled(optionRowGap, scale),
                    scaled(optionsToInputLabel, scale),
                    scaled(inputLabelToInputRow, scale),
                    scaled(inputRowToBottomRow, scale)
            );
        }

        int total() {
            return titleToTopRow
                    + topRowToPlayerTitle
                    + playerTitleToFirstRow
                    + optionRowGap
                    + optionsToInputLabel
                    + inputLabelToInputRow
                    + inputRowToBottomRow;
        }

        private static int scaled(int value, double scale) {
            return Math.max(2, (int) Math.round(value * scale));
        }
    }

    public record Positions(
            int centerX,
            int leftColumnX,
            int rightColumnX,
            int buttonWidth,
            int buttonHeight,
            int inputWidth,
            int titleY,
            int topRowY,
            int playerSettingsY,
            int firstOptionRowY,
            int secondOptionRowY,
            int inputLabelY,
            int inputRowY,
            int bottomRowY
    ) {
    }
}
