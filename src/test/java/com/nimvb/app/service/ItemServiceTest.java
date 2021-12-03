package com.nimvb.app.service;

import com.nimvb.app.repository.BoardRepository;
import com.nimvb.app.repository.ItemRepository;
import com.nimvb.app.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {


    @Mock
    private TodoRepository todoRepository;
    @Mock
    private BoardRepository boardRepository;
    @Mock
    private ItemRepository itemRepository;

    private TodoService todoService;

    @BeforeEach
    void init() {
        todoService = new TodoServiceImpl(boardRepository,todoRepository,itemRepository);
    }
    @Test
    void create() {

    }
}