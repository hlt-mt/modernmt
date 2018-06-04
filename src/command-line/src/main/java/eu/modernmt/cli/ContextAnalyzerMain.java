package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.lucene.LuceneAnalyzer;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Memory;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.training.MultilingualCorpusMask;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by davide on 17/12/15.
 */
public class ContextAnalyzerMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option index = Option.builder("i").longOpt("index-path").hasArg().required().build();
            Option corpora = Option.builder("c").longOpt("corpora").hasArgs().required().build();
            Option source = Option.builder("s").longOpt("source").hasArg().required().build();
            Option target = Option.builder("t").longOpt("target").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(index);
            cliOptions.addOption(corpora);
            cliOptions.addOption(source);
            cliOptions.addOption(target);
        }

        public final File indexPath;
        public final File[] corporaRoots;
        public final Language sourceLanguage;
        public final Language targetLanguage;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            sourceLanguage = Language.fromString(cli.getOptionValue('s'));
            targetLanguage = Language.fromString(cli.getOptionValue('t'));
            indexPath = new File(cli.getOptionValue('i'));

            String[] roots = cli.getOptionValues('c');
            corporaRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                corporaRoots[i] = new File(roots[i]);
        }

    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);

        Args args = new Args(_args);
        LanguagePair language = new LanguagePair(args.sourceLanguage, args.targetLanguage);

        ArrayList<MultilingualCorpus> corpora = new ArrayList<>();
        Corpora.list(null, false, corpora, args.sourceLanguage, args.targetLanguage, args.corporaRoots);

        HashMap<Memory, MultilingualCorpus> memory2corpus = new HashMap<>();
        for (MultilingualCorpus corpus : corpora) {
            long id = Long.parseLong(corpus.getName());

            Memory memory = new Memory(id);
            memory2corpus.put(memory, new MultilingualCorpusMask(language, corpus));
        }

        LanguagePair direction = new LanguagePair(args.sourceLanguage, args.targetLanguage);

        ContextAnalyzer contextAnalyzer = null;
        try {
            contextAnalyzer = new LuceneAnalyzer(Collections.singleton(direction), args.indexPath, eu.modernmt.context.lucene.storage.Options.prepareForBulkLoad());
            contextAnalyzer.add(memory2corpus);
        } finally {
            IOUtils.closeQuietly(contextAnalyzer);
        }
    }

}
