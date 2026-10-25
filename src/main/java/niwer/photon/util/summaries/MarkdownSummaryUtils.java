package niwer.photon.util.summaries;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;

public final class MarkdownSummaryUtils {

    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b\\p{L}+\\b");

    private static final Set<String> STOPWORDS_FR = Set.of(
            "le", "la", "les", "un", "une", "des", "du", "de", "d", "et", "ou", "mais", "dans",
            "en", "sur", "à", "pour", "avec", "est", "sont", "était", "ce", "cette", "qui", "que",
            "par", "au", "aux", "ne", "pas", "plus", "se", "sa", "son", "ses", "leur", "leurs"
    );

    private static final Set<String> STOPWORDS_EN = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
            "is", "are", "was", "were", "it", "this", "that", "by", "as", "from", "be", "have",
            "has", "had", "they", "we", "he", "she", "which", "who", "will", "would", "can"
    );

    private MarkdownSummaryUtils() {}

    private record SentenceCandidate(int originalIndex, String text, Set<String> tokens, double bonusMultiplier) {}
    private record ScoredSentence(SentenceCandidate candidate, double score) {}

    /**
     * Summarizes the given Markdown content into a specified number of sentences, taking into account the language (French or English) and applying a TextRank algorithm with additional heuristics for Markdown structure.
     * 
     * @param markdownContent
     * @param maxSentences
     * @return
     */
    public static String summarize(String markdownContent, int maxSentences) {
        if (markdownContent == null || markdownContent.isBlank() || maxSentences <= 0) {
            return "";
        }

        /* Detect the language (FR or EN) */
        String lang = detectLanguage(markdownContent);
        Locale locale = "fr".equals(lang) ? Locale.FRENCH : Locale.ENGLISH;
        Set<String> stopWords = "fr".equals(lang) ? STOPWORDS_FR : STOPWORDS_EN;

        /* Extract the text and Markdown metadata */
        List<SentenceCandidate> candidates = extractCandidates(markdownContent, locale, stopWords);
        if (candidates.size() <= maxSentences) {
            return candidates.stream().map(SentenceCandidate::text).collect(Collectors.joining(" "));
        }

        /* Calculate the similarity matrix (TextRank) */
        int n = candidates.size();
        double[][] similarityMatrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double sim = calculateSimilarity(candidates.get(i).tokens(), candidates.get(j).tokens());
                similarityMatrix[i][j] = sim;
                similarityMatrix[j][i] = sim;
            }
        }

        /* PageRank algorithm (20 iterations, damping 0.85) */
        double[] scores = new double[n];
        Arrays.fill(scores, 1.0);
        double damping = 0.85;

        for (int iter = 0; iter < 20; iter++) {
            double[] nextScores = new double[n];
            for (int i = 0; i < n; i++) {
                double sum = 0.0;
                for (int j = 0; j < n; j++) {
                    if (i != j && similarityMatrix[i][j] > 0) {
                        double totalOut = 0.0;
                        for (int k = 0; k < n; k++) totalOut += similarityMatrix[j][k];
                        if (totalOut > 0) {
                            sum += (similarityMatrix[i][j] / totalOut) * scores[j];
                        }
                    }
                }
                nextScores[i] = (1 - damping) + damping * sum;
            }
            scores = nextScores;
        }

        /* Apply the final scores to the sentence candidates */
        List<ScoredSentence> scored = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double finalScore = scores[i] * candidates.get(i).bonusMultiplier();
            scored.add(new ScoredSentence(candidates.get(i), finalScore));
        }

        /* Select the best sentences and sort them chronologically */
        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredSentence::score).reversed())
                .limit(maxSentences)
                .map(ScoredSentence::candidate)
                .sorted(Comparator.comparingInt(SentenceCandidate::originalIndex))
                .map(SentenceCandidate::text)
                .collect(Collectors.joining(" "));
    }

    /**
     * Detects the language of the given text based on the frequency of French and English stopwords.
     * If the count of French stopwords is greater than or equal to the count of English stopwords, it returns "fr" (French); otherwise, it returns "en" (English).
     * 
     * @param text
     * @return
     */
    private static String detectLanguage(String text) {
        int frCount = 0;
        int enCount = 0;
        var matcher = WORD_PATTERN.matcher(text.toLowerCase());

        while (matcher.find()) {
            String word = matcher.group();
            if (STOPWORDS_FR.contains(word)) frCount++;
            if (STOPWORDS_EN.contains(word)) enCount++;
        }
        return frCount >= enCount ? "fr" : "en";
    }
    
    private static List<SentenceCandidate> extractCandidates(String markdown, Locale locale, Set<String> stopWords) {
        Node document = MARKDOWN_PARSER.parse(markdown);
        List<SentenceCandidate> candidates = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getSentenceInstance(locale);
        int index = 0;

        Node node = document.getFirstChild();
        boolean isLeadParagraph = true;

        while (node != null) {
            if (node instanceof Paragraph) {
                String plainText = extractPlainText(node).trim();
                boolean hasBold = hasDescendantOfType(node, StrongEmphasis.class);

                /* Markdown heuristics: reward the beginnings of sections and bold text */
                double bonus = 1.0;
                if (isLeadParagraph) bonus *= 1.4;
                if (hasBold) bonus *= 1.25;

                isLeadParagraph = false;

                if (!plainText.isBlank()) {
                    iterator.setText(plainText);
                    int start = iterator.first();
                    for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                        String sText = plainText.substring(start, end).trim();
                        /* Filter micro-sentences */
                        if (sText.length() > 25) {
                            Set<String> tokens = tokenize(sText, stopWords);
                            candidates.add(new SentenceCandidate(index++, sText, tokens, bonus));
                        }
                    }
                }
            } else if (node instanceof Heading) {
                isLeadParagraph = true; // The entire paragraph following a heading is considered a lead paragraph
            }
            node = node.getNext();
        }
        return candidates;
    }

    private static double calculateSimilarity(Set<String> s1, Set<String> s2) {
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        long matches = s1.stream().filter(s2::contains).count();
        if (matches == 0) return 0.0;
        return (double) matches / (Math.log(s1.size() + 1) + Math.log(s2.size() + 1));
    }

    private static Set<String> tokenize(String text, Set<String> stopWords) {
        Set<String> tokens = new HashSet<>();
        var matcher = WORD_PATTERN.matcher(text.toLowerCase());
        while (matcher.find()) {
            String word = matcher.group();
            if (!stopWords.contains(word) && word.length() > 2) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    private static String extractPlainText(Node node) {
        StringBuilder sb = new StringBuilder();
        node.accept(new AbstractVisitor() {
            @Override
            public void visit(Text text) {
                sb.append(text.getLiteral());
            }
            @Override
            public void visit(SoftLineBreak softLineBreak) {
                sb.append(" ");
            }
            @Override
            public void visit(HardLineBreak hardLineBreak) {
                sb.append(" ");
            }
            @Override
            public void visit(Code code) {
                sb.append(code.getLiteral());
            }
        });
        return sb.toString();
    }

    private static boolean hasDescendantOfType(Node parent, Class<? extends Node> targetClass) {
        final boolean[] exists = {false};
        parent.accept(new AbstractVisitor() {
            @Override
            protected void visitChildren(Node node) {
                if (targetClass.isInstance(node)) {
                    exists[0] = true;
                    return;
                }
                super.visitChildren(node);
            }
        });
        return exists[0];
    }
}