package com.nimvb.app.service;

import com.nimvb.app.database.model.Board;
import com.nimvb.app.repository.BoardRepository;
import com.nimvb.app.repository.TodoRepository;
import com.nimvb.app.service.exception.EntityNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService{
    private final BoardRepository repository;
    private final TodoRepository todoRepository;

    @Override
    public Board create(String name, String color) {
        Assert.hasText(name,"name is empty");
        Assert.hasText(color,"color is empty");
        var board = new Board(){{
            setName(name);
            setColor(color);
        }};
        return repository.save(board);
    }

    @Override
    public Board find(String id) throws EntityNotFoundException,IllegalArgumentException {
        Assert.hasText(id,"id is empty");
        return repository.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    @Override
    public Collection<Board> all() {
        return repository.findAll();
    }

    @Override
    public void delete(String id) {
        Assert.hasText(id,"id is empty");
        repository.findById(id).orElseThrow(EntityNotFoundException::new);
        repository.deleteById(id);
    }
}

