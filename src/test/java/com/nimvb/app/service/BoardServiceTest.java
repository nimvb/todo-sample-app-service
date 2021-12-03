package com.nimvb.app.service;

import com.nimvb.app.database.model.Board;
import com.nimvb.app.repository.BoardRepository;
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {


    @Mock
    private BoardRepository boardRepository;
    @Mock
    private TodoRepository todoRepository;
    private BoardService boardService;

    @BeforeEach
    void init() {
        boardService = new BoardServiceImpl(boardRepository, todoRepository);
    }

    @Test
    void Should_CreateABoard_When_BasicInformationIsProvided() {
        String uuid = UUID.randomUUID().toString();
        String boardName = "b1";
        String color = "b1c1";
        var board = new Board() {{
            setId(uuid);
            setName(boardName);
            setColor(color);
        }};
        Mockito
                .when(boardRepository.save(ArgumentMatchers.any(Board.class)))
                .thenReturn(new Board() {{
                    setId(board.getId());
                    setName(board.getName());
                    setColor(board.getColor());
                    setTodos(board.getTodos());
                }});
        final Board target = boardService.create(boardName, color);
        Assertions.assertThat(target).isEqualTo(board);
        Assertions.assertThat(target).isNotSameAs(board);
        Mockito.verify(boardRepository, Mockito.times(1)).save(Mockito.any());
    }

    @Test
    void Should_ThrowException_When_ProvidedBasicInformationIsNULL() {
        Assertions.assertThatThrownBy(() -> {
            boardService.create(null, null);
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    void Should_FindTheBoard_When_RelatedIdIsProvided() {
        String id = UUID.randomUUID().toString();
        String name = "b1";
        String color = "b1c1";
        final Board board = new Board() {{
            setId(id);
            setName(name);
            setColor(color);
        }};
        Mockito.when(boardRepository.findById(ArgumentMatchers.any(String.class))).thenAnswer(invocation -> {
            final String targetId = invocation.getArgument(0);
            if (targetId == null) {
                throw new IllegalArgumentException();
            }
            if (targetId.equals(id)) {
                return Optional.of(new Board() {{
                    setId(board.getId());
                    setName(board.getName());
                    setColor(board.getColor());
                    setTodos(board.getTodos());
                }});
            }
            return Optional.empty();
        });

        final Board target = boardService.find(id);

        Assertions.assertThat(target).isNotNull();
        Assertions.assertThat(target).isEqualTo(board);
        Assertions.assertThat(target).isNotSameAs(board);
    }


    @Test
    void Should_ThrowAnException_When_RelatedIdIsNotExisted() {
        String id = UUID.randomUUID().toString();
        String name = "b1";
        String color = "b1c1";
        final Board board = new Board() {{
            setId(id);
            setName(name);
            setColor(color);
        }};
        Mockito.when(boardRepository.findById(ArgumentMatchers.any(String.class))).thenAnswer(invocation -> {
            final String targetId = invocation.getArgument(0);
            if (targetId == null) {
                throw new IllegalArgumentException();
            }
            if (targetId.equals(id)) {
                return Optional.of(new Board() {{
                    setId(board.getId());
                    setName(board.getName());
                    setColor(board.getColor());
                    setTodos(board.getTodos());
                }});
            }
            return Optional.empty();
        });

        Assertions.assertThatThrownBy(() -> {
            boardService.find(UUID.randomUUID().toString());
        }).isInstanceOf(EntityNotFoundException.class);

        Mockito.verify(boardRepository, Mockito.times(1)).findById(ArgumentMatchers.any());
    }

    @Test
    void Should_ThrowAnException_When_RelatedIdIsNULLOrEmpty() {
        Assertions.assertThatThrownBy(() -> {
            boardService.find(null);
        }).isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> {
            boardService.find("");
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void Should_FindAllBoards() {
        final List<Board> db = Stream.of(
                new Board() {{
                    setId(UUID.randomUUID().toString());
                    setName("b1");
                    setColor("b1c1");
                }},
                new Board() {{
                    setId(UUID.randomUUID().toString());
                    setName("b2");
                    setColor("b2c2");
                }}
        ).collect(Collectors.toList());
        Mockito.when(boardRepository.findAll()).thenAnswer(invocation -> db.stream().map(board -> new Board() {{
            setId(board.getId());
            setName(board.getName());
            setColor(board.getColor());
            setTodos(board.getTodos());
        }}).collect(Collectors.toList()));
        final Collection<Board> boards = boardService.all();
        Assertions.assertThat(boards).isNotNull();
        Assertions.assertThat(boards).hasSize(db.size());
        Assertions.assertThat(boards).isEqualTo(db);
        Assertions.assertThat(boards).isNotSameAs(db);
    }

    @Test
    void Should_ReturnEmptyList_When_ThereIsNoBoardExists() {
        Mockito.when(boardRepository.findAll()).thenReturn(Collections.emptyList());
        final Collection<Board> boards = boardService.all();
        Assertions.assertThat(boards).isNotNull();
        Assertions.assertThat(boards).hasSize(0);
    }

    @Test
    void Should_DeleteTheBoard_When_IdOfTheExistingBoardIsProvided() {
        final List<String> ids = Stream.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()).collect(Collectors.toList());
        final List<Board> boards = ids.stream().map(s -> {
            return new Board() {{
                setId(s);
                setName("n-" + s);
                setColor("c-" + s);
            }};
        }).collect(Collectors.toList());
        Mockito
                .when(boardRepository.findById(ArgumentMatchers.any()))
                        .thenAnswer(invocation -> {
                            final String id = invocation.getArgument(0);
                            if(id == null){
                                throw new IllegalArgumentException();
                            }
                            final Optional<Board> target = boards.stream().filter(board -> board.getId().equals(id)).findFirst();
                            if(target.isPresent()){
                                return Optional.of(new Board(){{
                                    setId(target.get().getId());
                                    setName(target.get().getName());
                                    setColor(target.get().getColor());
                                    setTodos(target.get().getTodos());
                                }});
                            }
                            return Optional.empty();
                        });
        Mockito
                .doAnswer(invocation -> {
                    final String id = invocation.getArgument(0);
                    if (id == null)
                        throw new IllegalArgumentException();
                    final Optional<Board> target = boards.stream().filter(board -> board.getId().equals(id)).findFirst();
                    target.ifPresent(boards::remove);
                    return null;
                })
                .when(boardRepository)
                .deleteById(ArgumentMatchers.any());

        boardService.delete(ids.get(0));

        Assertions.assertThat(boards).isNotNull();
        Assertions.assertThat(boards).hasSize(1);
        Assertions.assertThat(boards).allMatch(board -> board.getId().equals(ids.get(1)));

        Mockito.verify(boardRepository,Mockito.times(1)).findById(ArgumentMatchers.any());
        Mockito.verify(boardRepository,Mockito.times(1)).deleteById(ArgumentMatchers.any());
    }


    @Test
    void Should_NotDeleteTheBoardAndThrowException_When_IdOfTheExistingBoardsIsNotEqualToProvidedId() {
        final List<String> ids = Stream.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()).collect(Collectors.toList());
        final List<Board> boards = ids.stream().map(s -> {
            return new Board() {{
                setId(s);
                setName("n-" + s);
                setColor("c-" + s);
            }};
        }).collect(Collectors.toList());
        Mockito
                .lenient()
                .doAnswer(invocation -> {
                    final String id = invocation.getArgument(0);
                    if (id == null)
                        throw new IllegalArgumentException();
                    final Optional<Board> target = boards.stream().filter(board -> board.getId().equals(id)).findFirst();
                    target.ifPresent(boards::remove);
                    return null;
                })
                .when(boardRepository)
                .deleteById(ArgumentMatchers.any());

        Assertions.assertThatThrownBy(() -> {
            boardService.delete(UUID.randomUUID().toString());
        }).isInstanceOf(EntityNotFoundException.class);

        Assertions.assertThat(boards).isNotNull();
        Assertions.assertThat(boards).hasSize(boards.size());

        Mockito.verify(boardRepository,Mockito.times(0)).deleteById(ArgumentMatchers.any());
    }

    @Test
    void Should_ThrowAnException_When_IdOfTheTargetToBeDeletedIsNULLOrEmpty() {
        Assertions.assertThatThrownBy(() -> {
            boardService.delete(null);
        }).isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> {
            boardService.delete("");
        }).isInstanceOf(IllegalArgumentException.class);
    }
}