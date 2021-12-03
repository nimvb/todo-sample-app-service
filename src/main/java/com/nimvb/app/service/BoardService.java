package com.nimvb.app.service;

import com.nimvb.app.database.model.Board;
import com.nimvb.app.service.exception.EntityNotFoundException;
import lombok.NonNull;

import java.util.Collection;

public interface BoardService {

    /**
     * Create a new board
     *
     * @param name name of the board
     * @param color color of the board
     * @return The created board which is wrapped
     * @throws IllegalArgumentException if <code>name</code> or <code>color</code> is null or empty
     */
    Board create(String name, String color) throws IllegalArgumentException;

    /**
     * Find the board which has an id equal to the <code>id</code>
     *
     * @param id the id of the target board
     * @return The target board which is wrapped or cloned
     * @throws EntityNotFoundException if the target board with the <code>id</code> is not found
     * @throws IllegalArgumentException if the <code>id</code> is null or empty
     */
    Board find(String id) throws EntityNotFoundException,IllegalArgumentException;

    /**
     * Collection of all boards which are wrapped or cloned
     *
     * @return a collection contains all the boards
     */
    Collection<Board> all();

    /**
     * Delete the entity
     *
     * @param id the id of the target entity
     * @throws IllegalArgumentException if the <code>id</code> is null or empty
     * @throws EntityNotFoundException if the board which has corresponding <code>id</code> is not found;
     */
    void delete(String id) throws IllegalArgumentException,EntityNotFoundException;

}
