package com.moments.dao.impl;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.moments.dao.MomentDao;
import com.moments.models.Moment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class MomentDaoImpl implements MomentDao {

    @Autowired
    private Firestore firestore;

    private static final String COLLECTION_NAME = "moments";

    @Override
    public String saveMoment(Moment moment) throws ExecutionException, InterruptedException {

        DocumentReference documentReference = moment.getMomentId() == null || moment.getMomentId().isEmpty()
                ? firestore.collection(COLLECTION_NAME).document()
                : firestore.collection(COLLECTION_NAME).document(moment.getMomentId());

        if (moment.getMomentId() == null || moment.getMomentId().isEmpty()) {
            moment.setMomentId(documentReference.getId());
        }

        ApiFuture<WriteResult> future = documentReference.set(moment);
        future.get();

        return moment.getMomentId();
    }

    @Override
    public Moment getMomentById(String id) throws ExecutionException, InterruptedException {
        DocumentReference documentReference = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            return document.toObject(Moment.class);
        } else {
            throw new RuntimeException("Moment not found with ID: " + id);
        }
    }

    @Override
    public List<Moment> getAllMoments() throws ExecutionException, InterruptedException {
        CollectionReference collection = firestore.collection(COLLECTION_NAME);
        ApiFuture<QuerySnapshot> future = collection.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<Moment> moments = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            moments.add(document.toObject(Moment.class));
        }
        return moments;
    }

    @Override
    public void deleteMoment(String id) throws ExecutionException, InterruptedException {
        DocumentReference documentReference = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<WriteResult> future = documentReference.delete();
        future.get();
    }

    @Override
    public List<Moment> getMomentsFeed(String creatorUserId, String eventId, int offset, int limit) throws ExecutionException, InterruptedException {
        CollectionReference collection = firestore.collection(COLLECTION_NAME);
        Query query = collection.orderBy("creationTime", Query.Direction.DESCENDING);


        if(eventId != null && !eventId.isEmpty()) {
            query = query.whereEqualTo("eventId", eventId);
        }

        // Apply filter if creatorUserId is provided
        if (creatorUserId != null && !creatorUserId.isEmpty()) {
            query = query.whereEqualTo("creatorId", creatorUserId);
        }



        // Fetch all documents matching the query
        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        // Apply pagination (offset and limit)
        List<Moment> moments = new ArrayList<>();
        int startIndex = Math.min(offset, documents.size());
        int endIndex = Math.min(startIndex + limit, documents.size());
        for (int i = startIndex; i < endIndex; i++) {
            moments.add(documents.get(i).toObject(Moment.class));
        }

        return moments;
    }

    @Override
    public int getTotalCount(String creatorUserId, String eventId) throws ExecutionException, InterruptedException {
        CollectionReference collection = firestore.collection(COLLECTION_NAME);
        Query query = collection;

        if(eventId != null && !eventId.isEmpty()) {
            query = query.whereEqualTo("eventId", eventId);
        }

        // Apply filter if creatorUserId is provided
        if (creatorUserId != null && !creatorUserId.isEmpty()) {
            query = query.whereEqualTo("creatorId", creatorUserId);
        }
        // Count all matching documents
        ApiFuture<QuerySnapshot> future = query.get();
        return future.get().size();
    }


}


