package utils;

public final class Colors {

    private Colors() {
    }

    public static String parse(String text) {
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < text.length(); ) {
            if (i + 7 < text.length()
                    && text.charAt(i) == '{'
                    && text.charAt(i + 7) == '}') {
                String hex = text.substring(i + 1, i + 7);

                try {
                    int rgb = Integer.parseInt(hex, 16);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    out.append("\u001B[38;2;")
                            .append(r).append(";")
                            .append(g).append(";")
                            .append(b).append("m");

                    i += 8;
                    continue;
                } catch (Exception ignored) {
                }
            }

            out.append(text.charAt(i));
            i++;
        }

        out.append("\u001B[0m");
        return out.toString();
    }
}
