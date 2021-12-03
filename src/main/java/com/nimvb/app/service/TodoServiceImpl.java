package com.nimvb.app.service;

import com.nimvb.app.database.model.Board;
import com.nimvb.app.database.model.Item;
import com.nimvb.app.database.model.Todo;
import com.nimvb.app.repository.BoardRepository;
import com.nimvb.app.repository.ItemRepository;
import com.nimvb.app.repository.TodoRepository;
import com.nimvb.app.service.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Collection;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService{

    private final BoardRepository boardRepository;
    private final TodoRepository todoRepository;
    private final ItemRepository itemRepository;
    @Override
    public Todo create(String boardId, String name) {
        Assert.hasText(boardId,"board id is null or empty");
        Assert.hasText(name,"name is null or empty");
        final long creationTimestamp = Instant.now().toEpochMilli();
        var todo = new Todo(){{
            setName(name);
            setCreationTimestamp(creationTimestamp);
        }};
        var persistedTodo= todoRepository.persist(todo);
        final Board board = boardRepository.fetchById(boardId).orElseThrow(EntityNotFoundException::new);
        board.getTodos().add(persistedTodo);
        final Board result = boardRepository.save(board);
        return new Todo(){{
            setId(persistedTodo.getId());
            setName(persistedTodo.getName());
            setCreationTimestamp(persistedTodo.getCreationTimestamp());
            setItems(persistedTodo.getItems());
        }};
    }

    @Override
    public Todo find(Integer id) throws EntityNotFoundException {
        Assert.notNull(id,"id is null");
        return todoRepository.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    @Override
    public Collection<Todo> all() {
        return todoRepository.findAll();
    }

    @Override
    public Collection<Todo> all(String boardId) throws IllegalArgumentException, EntityNotFoundException {
        Assert.hasText(boardId,"board id is null or empty");
        final Board board = boardRepository.fetchById(boardId).orElseThrow(EntityNotFoundException::new);
        return board.getTodos().stream().map(todo -> {
            return new Todo(){{
                setId(todo.getId());
                setName(todo.getName());
                setItems(todo.getItems());
                setCreationTimestamp(todo.getCreationTimestamp());
            }};
        }).collect(Collectors.toList());
    }

    @Override
    public void delete(String boardId, Integer id) {
        /*
          Steps:
           1. find the target todo
           2. find the related board
           3. remove the todo from the related board
           4. find the corresponding items of the todo and remove the respectively from the item repository
           6. remove the items from the todo
           5. persist th board
           6. remove the item
         */
        Assert.hasText(boardId,"board id is null or empty");
        Assert.notNull(id,"todo id is null");
        final Todo todo = todoRepository.fetchById(id).orElseThrow(EntityNotFoundException::new);
        final Board board = boardRepository.fetchById(boardId).orElseThrow(EntityNotFoundException::new);
        board.getTodos().remove(todo);
        for (Item item : todo.getItems()) {
            itemRepository.deleteById(item.getId());
        }
        todo.getItems().clear();
        boardRepository.persist(board);
        todoRepository.deleteById(id);
    }
}
