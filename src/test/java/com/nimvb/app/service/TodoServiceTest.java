package com.nimvb.app.service;

import com.nimvb.app.database.exception.KeyAlreadyExistsException;
import com.nimvb.app.database.exception.KeyNotFoundException;
import com.nimvb.app.database.model.Board;
import com.nimvb.app.database.model.Todo;
import com.nimvb.app.repository.BoardRepository;
import com.nimvb.app.repository.ItemRepository;
import com.nimvb.app.repository.TodoRepository;
import com.nimvb.app.service.exception.EntityNotFoundException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private BoardRepository boardRepository;
    @Mock
    private TodoRepository todoRepository;
    @Mock
    private ItemRepository itemRepository;
    private TodoService todoService;

    @BeforeEach
    void init() {
        todoService = new TodoServiceImpl(boardRepository, todoRepository, itemRepository);
    }

    @Test
    void Should_CreateANewTodo_When_BasicInformationIsProvided() {
        String boardId = UUID.randomUUID().toString();
        Integer todoId = 1;
        var todo = new Todo() {{
            setId(todoId);
            setName("t1");
        }};
        var board = new Board() {{
            setId(boardId);
            setName("b1");
            setColor("b1c1");
        }};

        Mockito.when(boardRepository.fetchById(ArgumentMatchers.any())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if (id == null) {
                throw new IllegalArgumentException();
            }
            if (id.equals(board.getId())) {
                return Optional.of(board);
            }
            return Optional.empty();
        });
        Mockito.when(boardRepository.save(ArgumentMatchers.any())).thenAnswer(invocation -> {
            Board source = (Board) invocation.getArgument(0);
            board.setId(source.getId());
            board.setName(source.getName());
            board.setColor(source.getColor());
            board.setTodos(source.getTodos());
            return new Board() {{
                setId(source.getId());
                setName(source.getName());
                setColor(source.getColor());
                setTodos(source.getTodos());
            }};
        });
        Mockito.when(todoRepository.persist(ArgumentMatchers.any())).thenAnswer(invocation -> {
            Todo source = (Todo) invocation.getArgument(0);
            if (source == null)
                throw new IllegalArgumentException();
            todo.setId(source.getId());
            todo.setName(source.getName());
            todo.setItems(source.getItems());
            todo.setCreationTimestamp(source.getCreationTimestamp());
            return todo;
        });

        final Todo target = todoService.create(board.getId(), todo.getName());

        Assertions.assertThat(target).isNotNull();
        Assertions.assertThat(board).isNotNull();
        Assertions.assertThat(board.getTodos()).hasSize(1);
        Assertions.assertThat(board.getTodos().get(0)).isEqualTo(target);
        Assertions.assertThat(board.getTodos().get(0)).isNotSameAs(target);
    }

    @Test
    void Should_ThrowAnException_When_TodoIsCreatedForTheBoardWhichIsNotExists() {
        String boardId = UUID.randomUUID().toString();
        Integer todoId = 1;
        var todo = new Todo() {{
            setId(todoId);
            setName("t1");
        }};
        var board = new Board() {{
            setId(boardId);
            setName("b1");
            setColor("b1c1");
        }};

        Mockito.when(boardRepository.fetchById(ArgumentMatchers.any())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if (id == null) {
                throw new IllegalArgumentException();
            }
            if (id.equals(board.getId())) {
                return Optional.of(board);
            }
            return Optional.empty();
        });

        Assertions.assertThatThrownBy(() -> {
            todoService.create(UUID.randomUUID().toString(), todo.getName());
        }).isInstanceOf(EntityNotFoundException.class);
        Assertions.assertThat(board).isNotNull();
        Assertions.assertThat(board.getTodos()).hasSize(0);
        Mockito.verify(boardRepository, Mockito.times(1)).fetchById(ArgumentMatchers.any());
    }

    @Test
    void Should_ThrownAnException_When_TheCorrespondentBoardIsRemovedBeforePersistingIt() {
        String boardId = UUID.randomUUID().toString();
        Integer todoId = 1;
        var todo = new Todo() {{
            setId(todoId);
            setName("t1");
        }};
        final AtomicReference<Board> boardAtomicReference = new AtomicReference<>(new Board() {{
            setId(boardId);
            setName("b1");
            setColor("b1c1");
        }});
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        final CountDownLatch boardIsRemovedLock = new CountDownLatch(1);
        final CountDownLatch boardIsFetchedLock = new CountDownLatch(1);
        Mockito.when(boardRepository.fetchById(ArgumentMatchers.any())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            Optional<Board> result = Optional.empty();
            if (id == null) {
                throw new IllegalArgumentException();
            }
            if (boardAtomicReference.get() != null && id.equals(boardAtomicReference.get().getId())) {
                result = Optional.of(boardAtomicReference.get());
            }
            boardIsFetchedLock.countDown();
            return result;
        });
        Mockito
                .doAnswer(invocation -> {
                    boardIsFetchedLock.await();
                    String id = invocation.getArgument(0);
                    if (id == null) {
                        throw new IllegalArgumentException();
                    }

                    if (boardAtomicReference.get() != null && id.equals(boardAtomicReference.get().getId())) {
                        boardAtomicReference.set(null);
                    }
                    boardIsRemovedLock.countDown();
                    return null;
                })
                .when(boardRepository)
                .deleteById(ArgumentMatchers.any());
        Mockito.when(boardRepository.save(ArgumentMatchers.any())).thenAnswer(invocation -> {
            boardIsRemovedLock.await();
            Board source = (Board) invocation.getArgument(0);
            boardAtomicReference.getAndUpdate(board -> {
                board.setId(source.getId());
                board.setName(source.getName());
                board.setColor(source.getColor());
                board.setTodos(source.getTodos());
                return board;
            });
            return new Board() {{
                setId(source.getId());
                setName(source.getName());
                setColor(source.getColor());
                setTodos(source.getTodos());
            }};
        });

        final CompletableFuture<Todo> todoCreationTask = CompletableFuture.supplyAsync(() -> todoService.create(boardAtomicReference.get().getId(), todo.getName()), executorService);
        final CompletableFuture<Void> boardDeletionTask = CompletableFuture.runAsync(() -> {
            boardRepository.deleteById(boardId);
            boardIsRemovedLock.countDown();
        });

        CompletableFuture.allOf(todoCreationTask, boardDeletionTask);


        Assertions.assertThat(todoCreationTask).isCompletedExceptionally();
        Assertions.assertThat(boardDeletionTask).isCompleted();
        Assertions.assertThat(boardAtomicReference.get()).isNull();
        Assertions.assertThat(boardIsFetchedLock.getCount()).isEqualTo(0);
        Assertions.assertThat(boardIsRemovedLock.getCount()).isEqualTo(0);

        Mockito.verify(boardRepository, Mockito.times(1)).fetchById(ArgumentMatchers.any());
        Mockito.verify(boardRepository, Mockito.times(1)).deleteById(ArgumentMatchers.any());
        Mockito.verify(boardRepository, Mockito.times(1)).save(ArgumentMatchers.any());

    }

    @Test
    void Should_ThrownAnException_When_BoardIdOrNameIsNullOrEmpty() {
        Assertions.assertThatThrownBy(() -> {
            todoService.delete(null, 1);
        }).isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> {
            todoService.delete("null", null);
        }).isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> {
            todoService.delete(null, null);
        }).isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> {
            todoService.delete("null", 1);
        }).isNotInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void Should_ReturnTheTodo_When_IdOfTheExistingTodoIsProvided() {
        var creationTime = Instant.now();
        var todo = new Todo() {{
            setId(1);
            setName("t1");
            setCreationTimestamp(creationTime.toEpochMilli());
        }};
        Mockito.when(todoRepository.findById(ArgumentMatchers.any())).thenAnswer(invocation -> {
            Integer id = (Integer) invocation.getArgument(0);
            if (id == null)
                throw new IllegalArgumentException();
            Optional<Todo> result = Optional.empty();
            if (id.equals(todo.getId())) {
                result = Optional.of(new Todo() {{
                    setId(todo.getId());
                    setName(todo.getName());
                    setCreationTimestamp(todo.getCreationTimestamp());
                    setItems(todo.getItems());
                }});
            }
            return result;
        });

        final Todo target = todoService.find(1);

        Assertions.assertThat(target).isNotNull();
        Assertions.assertThat(target).isEqualTo(todo);
        Assertions.assertThat(target).isNotSameAs(todo);
        Mockito.verify(todoRepository, Mockito.times(1)).findById(ArgumentMatchers.any());
    }

    @Test
    void Should_ThrownAnException_When_IdOfTheExistingTodoIsNotProvided() {
        var creationTime = Instant.now();
        var todo = new Todo() {{
            setId(1);
            setName("t1");
            setCreationTimestamp(creationTime.toEpochMilli());
        }};
        Mockito.when(todoRepository.findById(ArgumentMatchers.any())).thenAnswer(invocation -> {
            Integer id = (Integer) invocation.getArgument(0);
            if (id == null)
                throw new IllegalArgumentException();
            Optional<Todo> result = Optional.empty();
            if (id.equals(todo.getId())) {
                result = Optional.of(new Todo() {{
                    setId(todo.getId());
                    setName(todo.getName());
                    setCreationTimestamp(todo.getCreationTimestamp());
                    setItems(todo.getItems());
                }});
            }
            return result;
        });

        Assertions.assertThatThrownBy(() -> {
            todoService.find(2);
        }).isInstanceOf(EntityNotFoundException.class);


        Mockito.verify(todoRepository, Mockito.times(1)).findById(ArgumentMatchers.any());
    }


    @Test
    void Should_ThrownAnException_When_TodoIdIsNULL() {
        var creationTime = Instant.now();
        var todo = new Todo() {{
            setId(1);
            setName("t1");
            setCreationTimestamp(creationTime.toEpochMilli());
        }};

        Assertions.assertThatThrownBy(() -> {
            todoService.find(null);
        }).isInstanceOf(IllegalArgumentException.class);


        Mockito.verify(todoRepository, Mockito.times(0)).findById(ArgumentMatchers.any());
    }

    @Test
    void Should_ReturnAllTodos_When_Executed() {
        final Instant genesisTime = Instant.now();
        final List<Todo> todos = Arrays.asList(new Todo() {{
            setId(1);
            setName("t1");
            setCreationTimestamp(genesisTime.toEpochMilli());
        }}, new Todo() {{
            setId(2);
            setName("t2");
            setCreationTimestamp(genesisTime.plusSeconds(5).toEpochMilli());
        }}, new Todo() {{
            setId(3);
            setName("t3");
            setCreationTimestamp(genesisTime.plusSeconds(10).toEpochMilli());
        }});

        Mockito.when(todoRepository.findAll()).thenAnswer(invocation -> todos.stream().map(todo -> new Todo() {{
            setId(todo.getId());
            setName(todo.getName());
            setItems(todo.getItems());
            setCreationTimestamp(todo.getCreationTimestamp());
        }}).collect(Collectors.toList()));

        final Collection<Todo> result = todoService.all();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(todos.size());
        Assertions.assertThat(result).isEqualTo(todos);
        Assertions.assertThat(result).isNotSameAs(todos);
    }

    @Test
    void Should_ReturnAllTodosOfTheBoard_When_TheBoardIdIsProvided() {
        var creationTimestamp = Instant.now();
        var boardId = UUID.randomUUID().toString();
        final List<Todo> todos = List.of(new Todo() {{
            setId(1);
            setName("t1");
            setCreationTimestamp(creationTimestamp.toEpochMilli());
        }});
        var board = new Board() {{
            setId(boardId);
            setName("b1");
            setColor("b1c1");
            setTodos(todos);
        }};

        Mockito.when(boardRepository.fetchById(ArgumentMatchers.any())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException();
            }
            Optional<Board> result = Optional.empty();
            if (id.equals(board.getId())) {
                result = Optional.of(board);
            }
            return result;
        });

        final Collection<Todo> result = todoService.all(boardId);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(todos.size());
        Assertions.assertThat(result).isEqualTo(todos);
        Assertions.assertThat(result).isNotSameAs(todos);
        Assertions.assertThat(result).allMatch(todo -> todos.stream().anyMatch(t -> t.equals(todo)));
        Assertions.assertThat(result).noneMatch(todo -> todos.stream().anyMatch(t -> t == todo));

        Mockito.verify(boardRepository, Mockito.times(1)).fetchById(ArgumentMatchers.any());
    }

    @Test
    void Should_ThrowAnException_When_TheProvidedBoardIdIsNotExists() {
        var creationTimestamp = Instant.now();
        var boardId = UUID.randomUUID().toString();
        final List<Todo> todos = List.of(new Todo() {{
            setId(1);
            setName("t1");
            setCreationTimestamp(creationTimestamp.toEpochMilli());
        }});
        var board = new Board() {{
            setId(boardId);
            setName("b1");
            setColor("b1c1");
            setTodos(todos);
        }};

        Mockito.when(boardRepository.fetchById(ArgumentMatchers.any())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException();
            }
            Optional<Board> result = Optional.empty();
            if (id.equals(board.getId())) {
                result = Optional.of(board);
            }
            return result;
        });

        Assertions.assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> {
            final Collection<Todo> result = todoService.all(UUID.randomUUID().toString());
        });


        Mockito.verify(boardRepository, Mockito.times(1)).fetchById(ArgumentMatchers.any());
    }

    @Test
    void Should_ThrowAnException_When_BoardIdIsNULLOrEmpty() {
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            todoService.all(null);
        });
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            todoService.all("");
        });
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            todoService.all(" ");
        });
    }

    @Test
    void Should_DeleteTheTodoOfTheBoard_When_CorrespondentBoardIdAndTodoIdIsProvided() {
        var boardId = UUID.randomUUID().toString();
        var todoId = 1;
        var timestamp = Instant.now();
        final List<Todo> todos = new ArrayList<>() {{
            add(new Todo() {{
                setId(todoId + 1);
                setName("t2");
                setCreationTimestamp(timestamp.plusSeconds(5).toEpochMilli());
            }});
            add(new Todo() {{
                setId(todoId);
                setName("t1");
                setCreationTimestamp(timestamp.toEpochMilli());
            }});
        }};
        var board = new Board() {{
            setId(boardId);
            setName("b1");
            setColor("b1c1");
            setTodos(todos);
        }};

        Mockito.when(boardRepository.fetchById(ArgumentMatchers.any())).thenAnswer(invocation -> {
            final String id = invocation.getArgument(0);
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException();
            }
            Optional<Board> target = Optional.empty();
            if (id.equals(board.getId())) {
                target = Optional.of(board);
            }
            return target;
        });

        Mockito.when(boardRepository.persist(ArgumentMatchers.any())).thenAnswer(invocation -> {
            Board source = invocation.getArgument(0);
            if (source == null) {
                throw new IllegalArgumentException();
            }
            if (source.getId() != null) {
                if (!source.getId().equals(board.getId())) {
                    throw new KeyNotFoundException();
                }
                board.setName(source.getName());
                board.setColor(source.getColor());
                board.setTodos(source.getTodos());
                return board;
            }
            throw new KeyAlreadyExistsException();
        });

        Mockito.when(todoRepository.fetchById(ArgumentMatchers.any())).thenAnswer(invocation -> {
            final Integer id = invocation.getArgument(0);
            if (id == null) {
                throw new IllegalArgumentException();
            }
            Optional<Todo> result = Optional.empty();
            final Optional<Todo> target = todos.stream().filter(todo -> todo.getId().equals(id)).findFirst();
            if (target.isPresent()) {
                result = target;
            }
            return result;
        });

        Mockito.doAnswer(invocation -> {
            final Integer id = invocation.getArgument(0);
            if (id == null) {
                throw new IllegalArgumentException();
            }
            Optional<Todo> result = Optional.empty();
            final Optional<Todo> target = todos.stream().filter(todo -> todo.getId().equals(id)).findFirst();
            target.ifPresent(todos::remove);
            return null;
        }).when(todoRepository).deleteById(ArgumentMatchers.any());


        todoService.delete(boardId, todoId);

        Assertions.assertThat(todos).isNotNull();
        Assertions.assertThat(todos).hasSize(1);
        Assertions.assertThat(todos.get(0).getId()).isEqualTo(todoId + 1);
        Assertions.assertThat(todos.get(0).getCreationTimestamp()).isEqualTo(timestamp.plusSeconds(5).toEpochMilli());
        Assertions.assertThat(board).isNotNull();
        Assertions.assertThat(board.getTodos()).hasSize(1);
        Assertions.assertThat(board.getTodos()).isEqualTo(todos);
        Assertions.assertThat(board.getTodos()).isSameAs(todos);

    }
}