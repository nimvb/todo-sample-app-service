package com.nimvb.app.service;

import com.nimvb.app.database.model.Item;
import com.nimvb.app.service.exception.EntityNotFoundException;

import java.time.Instant;
import java.util.Collection;

public interface ItemService {

    Item create(Integer todoId, String title, String description, Instant deadline);

    Item find(Integer id) throws EntityNotFoundException;

    Collection<Item> all();

    void delete(Integer todoId,Integer id);
}
