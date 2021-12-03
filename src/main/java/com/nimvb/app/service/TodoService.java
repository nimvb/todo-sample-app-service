package com.nimvb.app.service;

import com.nimvb.app.database.exception.KeyNotFoundException;
import com.nimvb.app.database.model.Todo;
import com.nimvb.app.service.exception.EntityNotFoundException;

import java.util.Collection;

public interface TodoService {

    /**
     * Create a new todo entity
     *
     * @param boardId the id of the parent board which the newly created todo should be added to
     * @param name the name of the todo
     * @return the created todo entity which is wrapped
     * @throws EntityNotFoundException if the board with <code>boardId</code> is not found
     * @throws KeyNotFoundException if the board with <code>boardId</code> is not found due to the fact that the call to this method is not transactional
     * @throws IllegalArgumentException if <code>boardId</code> or <code>name</code> is null or empty
     */
    Todo create(String boardId,String name) throws IllegalArgumentException, EntityNotFoundException, KeyNotFoundException;

    /**
     * Find the existing todo
     *
     * @param id the id of the target todo
     * @return the target todo which is wrapped
     * @throws EntityNotFoundException if there is no entity which has id equals to <code>id</code>
     * @throws IllegalArgumentException if the <code>id</code> is null
     */
    Todo find(Integer id) throws EntityNotFoundException,IllegalArgumentException;

    /**
     * Collection of all the existing todos
     *
     * @return the collection contains all the existing boards which are wrapped
     */
    Collection<Todo> all();

    /**
     * Collection of all the existing todos related to the board with id of <code>boardId</code>
     *
     * @param boardId id of the existing board
     * @return the collection contains all the existing boards which are wrapped
     * @throws IllegalArgumentException if <code>boardId</code> is empty or null
     * @throws EntityNotFoundException if the board which its id equals to <code>boardId</code> is not found
     */
    Collection<Todo> all(String boardId) throws IllegalArgumentException,EntityNotFoundException;

    /**
     * Delete the existing todo from the corresponding existing board
     *
     * @param boardId the id of the board
     * @param id the id of the todo
     * @throws IllegalArgumentException if the <code>boardId</code> or the <code>id</code> is null or empty
     * @throws EntityNotFoundException if the corresponding board or todo is not found
     */
    void delete(String boardId,Integer id) throws EntityNotFoundException,IllegalArgumentException;
}
