package eu.modernmt.api.actions.translation;

import eu.modernmt.api.actions.util.ContextUtils;
import eu.modernmt.api.framework.HttpMethod;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;
import eu.modernmt.api.framework.actions.ObjectAction;
import eu.modernmt.api.framework.routing.Route;
import eu.modernmt.api.model.TranslationResponse;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.decoder.DecoderException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Priority;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.xml.format.InputFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * Created by davide on 17/12/15.
 */
@Route(aliases = "translate", method = HttpMethod.GET)
public class Translate extends ObjectAction<TranslationResponse> {


    public static final int MAX_QUERY_LENGTH = 5000;

    @Override
    protected TranslationResponse execute(RESTRequest req, Parameters _params) throws ContextAnalyzerException, PersistenceException, DecoderException, ProcessingException {
        Params params = (Params) _params;

        TranslationResponse result = new TranslationResponse(params.priority);
        result.verbose = params.verbose;

        if (params.contextString != null) {
            result.context = ModernMT.translation.getContextVector(params.user, params.direction, params.contextString, params.contextLimit);
        } else {
            result.context = params.context;
        }
        result.terminology = params.terminology;

        result.translation = ModernMT.translation.get(params.user, params.direction, params.format, params.query, result.context, result.terminology, params.nbest, params.priority, params.timeout);

        if (result.context != null)
            ContextUtils.resolve(result.context);

        if (result.terminology != null)
            ContextUtils.resolve(result.terminology);

        return result;
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
    }

    public static class Params extends Parameters {
        private final Logger logger = LogManager.getLogger(getClass());

        public final InputFormat.Type format;
        public final UUID user;
        public final LanguageDirection direction;
        public final String query;
        public final ContextVector context;
        public final String contextString;
        public final ContextVector terminology;
        public final int contextLimit;
        public final int nbest;
        public final Priority priority;
        public final boolean verbose;
        public final long timeout;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);

            format = getEnum("if", InputFormat.Type.class, null);
            user = getUUID("user", null);

            query = getString("q", true);
            if (query.length() > MAX_QUERY_LENGTH)
                throw new ParameterParsingException("q", query.substring(0, 10) + "...",
                        "max query length of " + MAX_QUERY_LENGTH + " exceeded");

            LanguageDirection engineDirection = ModernMT.getNode().getEngine().getLanguageIndex().asSingleLanguagePair();
            direction = engineDirection != null ?
                    getLanguagePair("source", "target", engineDirection) :
                    getLanguagePair("source", "target");

            contextLimit = getInt("context_limit", 10);
            nbest = getInt("nbest", 0);

            priority = getEnum("priority", Priority.class, Priority.NORMAL);
            verbose = getBoolean("verbose", false);
            timeout = getLong("timeout", 0L);

            String terminologyWeights = getString("terminology_vector", false, null);

            if (terminologyWeights != null) {
                terminology = ContextUtils.parseParameter("terminology_vector", terminologyWeights);
            } else {
                terminology = null;
            }
            logger.debug("Params terminology:" + terminology);

            String  contextWeights = getString("context_vector", false, null);

            if (contextWeights != null) {
                context = ContextUtils.parseParameter("context_vector", contextWeights);
                contextString = null;
            } else {
                context = null;
                contextString = getString("context", false, null);
            }
            logger.debug("Params context:" + context);
            logger.debug("Params contextString:" + contextString);



        }
    }
}