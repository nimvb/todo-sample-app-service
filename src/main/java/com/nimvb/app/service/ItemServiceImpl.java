package com.nimvb.app.service;

import com.nimvb.app.database.model.Item;
import com.nimvb.app.database.model.Todo;
import com.nimvb.app.repository.ItemRepository;
import com.nimvb.app.repository.TodoRepository;
import com.nimvb.app.service.exception.EntityNotFoundException;
import com.nimvb.app.service.exception.InvalidTimestampException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService{
    private final TodoRepository todoRepository;
    private final ItemRepository itemRepository;
    @Override
    public Item create(@NonNull Integer todoId, @NonNull String title, @NonNull String description, @NonNull Instant deadline) {
        /*
          1. find the related todo
          2. create a new item and persist it
          3. add the created item to the corresponding todo
          4. persist the todo
         */
        final Instant start = Instant.now();
        if(deadline.isBefore(start)){
            throw new InvalidTimestampException();
        }
        final Todo todo = todoRepository.fetchById(todoId).orElseThrow(EntityNotFoundException::new);
        final Item item = itemRepository.persist(new Item() {{
            setTitle(title);
            setDescription(description);
            setCreationTimestamp(start.toEpochMilli());
            setDeadlineTimestamp(deadline.toEpochMilli());
            setCompleted(false);
        }});
        todo.getItems().add(item);
        return new Item() {{
            setId(item.getId());
            setTitle(item.getTitle());
            setDescription(item.getDescription());
            setCreationTimestamp(item.getCreationTimestamp());
            setDeadlineTimestamp(item.getDeadlineTimestamp());
        }};
    }

    @Override
    public Item find(Integer id) throws EntityNotFoundException {
        return itemRepository.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    @Override
    public Collection<Item> all() {
        return itemRepository.findAll();
    }

    @Override
    public void delete(Integer todoId, Integer id) {
        /*
         * 1. fetch the parent(todo);
         * 2. fetch the item
         * 3. remove the item from the parent
         * 4. persist the parent(todo)
         * 5. delete the item
         */
        final Todo todo = todoRepository.fetchById(todoId).orElseThrow(EntityNotFoundException::new);
        final Item item = itemRepository.fetchById(id).orElseThrow(EntityNotFoundException::new);
        todo.getItems().remove(item);
        todoRepository.persist(todo);
        itemRepository.deleteById(item.getId());
    }
}
