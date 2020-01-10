package eu.modernmt.processing;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Word;
import eu.modernmt.model.XMLTag;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class PreprocessorTest {

    private static final LanguageDirection language = new LanguageDirection(Language.ENGLISH, Language.ITALIAN);

    private static Sentence process(String text) throws ProcessingException {
        Preprocessor preprocessor = null;

        try {
            preprocessor = new Preprocessor();
            return preprocessor.process(language, text);
        } catch (IOException e) {
            throw new ProcessingException(e);
        } finally {
            IOUtils.closeQuietly(preprocessor);
        }
    }
    private static Sentence process(String text, LanguageDirection language) throws ProcessingException {
        Preprocessor preprocessor = null;

        try {
            preprocessor = new Preprocessor();
            return preprocessor.process(language, text);
        } catch (IOException e) {
            throw new ProcessingException(e);
        } finally {
            IOUtils.closeQuietly(preprocessor);
        }
    }

    @Test
    public void testCommonSentence() throws ProcessingException {
        String text = "Hello world!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
        assertFalse(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("Hello", "Hello", null," "),
                new Word("world", "world", " ",null),
                new Word("!", "!",  null,null),
        }, sentence.getWords());
    }

    @Test
    public void testInitialTagWithSpace() throws ProcessingException {
        String text = "<a> Hello world!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("Hello", "Hello", " "," "),
                new Word("world", "world", " ", null),
                new Word("!", "!", null,null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<a>", null, " ", 0)
        }, sentence.getTags());
    }

    @Test
    public void testStrippedSentenceWithSpaceAfterTag() throws ProcessingException {
        String text = "Hello<a> world!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("Hello", "Hello", null,null),
                new Word("world", "world", " ", null),
                new Word("!", "!", null, null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<a>", null, " ", 1)
        }, sentence.getTags());
    }

    @Test
    public void testStrippedSentenceWithSpacesBetweenTags() throws ProcessingException {
        String text = "Hello<a> <b>world!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("Hello", "Hello", null, null),
                new Word("world", "world", null, null),
                new Word("!", "!", null, null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<a>", null, " ", 1),
                XMLTag.fromText("<b>", " ", null, 1)
        }, sentence.getTags());
    }

    @Test
    public void testRequiredSpaceTrue() throws ProcessingException {
        String text = "Hello<a>guys";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello guys", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("Hello", "Hello", null, null),
                new Word("guys", "guys", null, null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<a>", null, null, 1)
        }, sentence.getTags());
    }

    @Test
    public void testRequiredSpaceFalse() throws ProcessingException {
        String text = "Hello<a>!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello!", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("Hello", "Hello", null, null),
                new Word("!", "!", null, null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<a>", null, null, 1)
        }, sentence.getTags());
    }

    @Test
    public void testRequiredSpaceFalseWithRightSpace() throws ProcessingException {
        String text = "Hello<a> !";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello!", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("Hello", "Hello", null, null),
                new Word("!", "!", " ", null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<a>", null, " ", 1)
        }, sentence.getTags());
    }

    @Test
    public void testRequiredSpaceFalseWithLeftSpace() throws ProcessingException {
        String text = "Hello <a>!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello!", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("Hello", "Hello", null, " "),
                new Word("!", "!", null, null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<a>", " ", null, 1)
        }, sentence.getTags());
    }

    @Test
    public void testPartiallyCased() throws ProcessingException {
        String text = "I LOVE New York";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("I LOVE New York", sentence.toString(false, false));
        assertEquals("I LOVE New York", sentence.toString(false, true));
        assertFalse(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("I", "I", null, " "),
                new Word("LOVE", "LOVE", " ", " "),
                new Word("New", "New", " ", " "),
                new Word("York", "York", " ", null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{}, sentence.getTags());
    }

    @Test
    public void testAllCased() throws ProcessingException {
        String text = "I LOVE NEW YORK";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("I LOVE NEW YORK", sentence.toString(false, false));
        assertEquals("I love new york", sentence.toString(false, true));
        assertFalse(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("I", "I", null, " "),
                new Word("LOVE", "love", " ", " "),
                new Word("NEW", "new", " ", " "),
                new Word("YORK", "york", " ", null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{}, sentence.getTags());
    }


    @Test
    @Ignore
    public void testAllCasedOpennlp() throws ProcessingException {
        String text = "VAGTSKIFTE I STATSMINISTERIET STYRKER METTE FREDERIKSEN";
        Sentence sentence = process(text, new LanguageDirection(Language.DANISH, Language.DANISH));

        assertEquals(text, sentence.toString(true, false));
        assertEquals("VAGTSKIFTE I STATSMINISTERIET STYRKER METTE FREDERIKSEN", sentence.toString(false, false));
        assertEquals("Vagtskifte i statsministeriet styrker mette frederiksen", sentence.toString(false, true));
        assertFalse(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("VAGTSKIFTE", "Vagtskifte", null, " "),
                new Word("I", "i", " ", " "),
                new Word("STATSMINISTERIET", "statsministeriet", " ", " "),
                new Word("STYRKER", "styrker", " ", " "),
                new Word("METTE", "mette", " ", " "),
                new Word("FREDERIKSEN", "frederiksen", " ", null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{}, sentence.getTags());
    }

    @Test
    @Ignore
    public void testMixedCasedOpennlp() throws ProcessingException {
        String text = "VAGTSKIFTE i Statsministeriet STYRKER mette Frederiksen";
        Sentence sentence = process(text, new LanguageDirection(Language.DANISH, Language.DANISH));

        assertEquals(text, sentence.toString(true, false));
        assertEquals("VAGTSKIFTE i Statsministeriet STYRKER mette Frederiksen", sentence.toString(false, false));
        assertEquals("VAGTSKIFTE i Statsministeriet STYRKER mette Frederiksen", sentence.toString(false, true));
        assertFalse(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("VAGTSKIFTE", "VAGTSKIFTE", null, " "),
                new Word("i", "i", " ", " "),
                new Word("Statsministeriet", "Statsministeriet", " ", " "),
                new Word("STYRKER", "STYRKER", " ", " "),
                new Word("mette", "mette", " ", " "),
                new Word("Frederiksen", "Frederiksen", " ", null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{}, sentence.getTags());
    }
}
