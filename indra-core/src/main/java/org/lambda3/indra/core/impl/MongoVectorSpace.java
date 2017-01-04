package org.lambda3.indra.core.impl;

import com.google.common.base.Preconditions;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.Binary;
import org.lambda3.indra.core.VectorPair;
import org.lambda3.indra.core.VectorSpace;
import org.lambda3.indra.core.utils.VectorsUtils;
import org.lambda3.indra.common.client.AnalyzedPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

class MongoVectorSpace implements VectorSpace {
    private static String termFieldName = "term";
    private static String vectorFieldName = "vector";
    private static String termsCollName = "terms";

    private Map<String, Map<Integer, Double>> vectorsCache = new ConcurrentHashMap<>();
    private Logger logger = LoggerFactory.getLogger(getClass());
    private MongoClient mongoClient;
    private final String dbName;

    MongoVectorSpace(MongoClientURI mURI, String dbName) {
        logger.info("Creating new vector space");
        this.mongoClient = new MongoClient(mURI);
        this.dbName = dbName;
    }

    @Override
    public VectorPair getVector(AnalyzedPair pair) {
        return getVector(pair, Integer.MAX_VALUE);
    }

    @Override
    public VectorPair getVector(AnalyzedPair pair, int limit) {
        Preconditions.checkNotNull(pair);

        collectVectors(new HashSet<String>(){{
            addAll(pair.getT1());
            addAll(pair.getT2());
        }}, limit);

        VectorPair vectorPair = new VectorPair();

        vectorPair.v1 = composeVectors(pair.getT1());

        if (vectorPair.v1 == null) {
            logger.warn("Vector for {} not found", pair.getTextPair().t1);
        }
        else {
            vectorPair.v2 = composeVectors(pair.getT2());
            if (vectorPair.v2 == null) {
                logger.warn("Vector for {} not found", pair.getTextPair().t2);
            }
        }
        return vectorPair;
    }

    @Override
    public Map<AnalyzedPair, VectorPair> getVectors(List<AnalyzedPair> pairs, int limit) {
        Preconditions.checkNotNull(pairs);
        Map<AnalyzedPair, VectorPair> res = new ConcurrentHashMap<>();

        Set<String> allTerms = new HashSet<>();
        for (AnalyzedPair p : pairs) {
            allTerms.addAll(p.getT1());
            allTerms.addAll(p.getT2());
        }

        collectVectors(allTerms, limit);

        for (AnalyzedPair p : pairs) {
            VectorPair vectorPair = new VectorPair();
            vectorPair.v1 = composeVectors(p.getT1());
            vectorPair.v2 = composeVectors(p.getT2());
            res.put(p, vectorPair);
        }

        return res;
    }

    private MongoCollection<Document> getColl() {
        return this.mongoClient.getDatabase(dbName).getCollection(termsCollName);
    }

    private void collectVectors(Set<String> terms, int limit) {
        Set<String> toFetch = terms.stream()
                .filter(t -> !this.vectorsCache.containsKey(t))
                .collect(Collectors.toSet());

        logger.debug("Cache has {} vectors, need to fetch more {}",
                terms.size() - toFetch.size(), toFetch.size());

        if (!toFetch.isEmpty()) {
            logger.info("Collecting {} term vectors", toFetch.size());
            FindIterable<Document> docs = getColl().find(Filters.in(termFieldName, toFetch));
            if (docs != null) {
                docs.batchSize(toFetch.size());
                for (Document doc : docs) {
                    this.vectorsCache.put(doc.getString(termFieldName), unmarshall(doc, limit));
                }
            }
        }
    }

    //TODO: Decouple this to use different methods of vector composition
    private Map<Integer, Double> composeVectors(List<String> terms) {
        logger.trace("Composing {} vectors", terms.size());
        List<Map<Integer, Double>> vectors = getFromCache(new HashSet<>(terms));
        if (!vectors.isEmpty()) {
            return VectorsUtils.add(vectors);
        }

        return null;
    }

    private List<Map<Integer, Double>> getFromCache(Set<String> terms) {
        List<Map<Integer, Double>> termVectors = new ArrayList<>();
        terms.stream().
                filter(t -> this.vectorsCache.containsKey(t)).
                forEach((t) -> termVectors.add(this.vectorsCache.get(t)));
        return termVectors;
    }

    private Map<Integer, Double> unmarshall(Document doc, int limit) {
        final Binary binary = doc.get(vectorFieldName, Binary.class);
        final byte[] b = binary.getData();
        final Map<Integer, Double> vector = new HashMap<>();

        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b))) {
            int key;
            double score;
            int size = Math.min(dis.readInt(), limit);

            for (int i = 0; i < size; i++) {
                key = dis.readInt();
                score = dis.readFloat(); //TODO: models were inserted as float. In the next generation version, insert by double.
                vector.put(key, score);
            }

            return vector;
        }
        catch(IOException e) {
            logger.error("Fail to read vector data.", e);
        }
        return null;
    }
}