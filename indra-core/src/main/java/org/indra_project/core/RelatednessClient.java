package org.indra_project.core;

import org.indra_project.core.exception.RelatednessError;
import org.indra_project.core.lang.IndraAnalyzer;
import org.indra_project.core.lang.Langs;
import org.indra_project.common.client.AnalyzedPair;
import org.indra_project.common.client.ScoredTextPair;
import org.indra_project.common.client.TextPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//TODO: JavaDoc
public abstract class RelatednessClient {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected abstract List<ScoredTextPair> compute(List<AnalyzedPair> pairs);

    protected abstract Params getParams();

    private AnalyzedPair doAnalyze(IndraAnalyzer analyzer, TextPair p) {
        try {
            return analyzer.analyze(p);
        } catch (IOException e) {
            logger.error("Error analyzing {}", p, e);
        }
        return null;
    }

    private List<ScoredTextPair> doCompute(List<TextPair> pairs)  {
        logger.debug("Analyzing {} pairs", pairs.size());
        try {
            IndraAnalyzer analyzer = Langs.newInstanceAnalyzer(getParams().language, getParams().useStemming());
            List<AnalyzedPair> analyzedPairs = new ArrayList<>();
            pairs.forEach(p -> {
                AnalyzedPair analyzedPair = doAnalyze(analyzer, p);
                if (analyzedPair != null)
                    analyzedPairs.add(doAnalyze(analyzer, p));
            });

            logger.debug("Computing relatedness..");
            List<ScoredTextPair> r = compute(analyzedPairs);
            logger.debug("Done.");
            return r;
        }
        catch(Exception e) {
            throw new RelatednessError(e);
        }
    }

    public final RelatednessResult getRelatedness(List<TextPair> pairs) {
        return new RelatednessResult(doCompute(pairs));
    }
}