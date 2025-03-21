package de.rieckpil.courses.book.management;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookSynchronizationListenerTest {

  private static final String VALID_ISBN = "1234567891234";

  @Mock
  private BookRepository mockedBookRepository;

  @Mock
  private OpenLibraryApiClient mockedOpenLibraryApiClient;

  @InjectMocks
  private BookSynchronizationListener cut;

  @Captor
  private ArgumentCaptor<Book> bookArgumentCaptor;

  @Test
  void shouldRejectBookWhenIsbnIsMalformed() {
    cut.consumeBookUpdates(new BookSynchronization("12345"));

    verifyNoInteractions(mockedBookRepository, mockedOpenLibraryApiClient);
  }

  @Test
  void shouldNotOverrideWhenBookAlreadyExists() {
    when(mockedBookRepository.findByIsbn(VALID_ISBN)).thenReturn(new Book());

    cut.consumeBookUpdates(new BookSynchronization(VALID_ISBN));

    verifyNoInteractions(mockedOpenLibraryApiClient);
    verify(mockedBookRepository, never()).save(any(Book.class));
  }

  @Test
  void shouldThrowExceptionWhenProcessingFails() {
    when(mockedBookRepository.findByIsbn(VALID_ISBN)).thenReturn(null);
    when(mockedOpenLibraryApiClient.fetchMetadataForBook(VALID_ISBN)).thenThrow(new RuntimeException("Network timeout"));

    BookSynchronization bookSynchronization = new BookSynchronization(VALID_ISBN);

    assertThrows(RuntimeException.class, () -> cut.consumeBookUpdates(bookSynchronization));
  }

  @Test
  void shouldStoreBookWhenNewAndCorrectIsbn() {
    Book requestedBook = new Book();
    requestedBook.setTitle("Test Book");
    requestedBook.setIsbn(VALID_ISBN);

    when(mockedBookRepository.findByIsbn(VALID_ISBN)).thenReturn(null);
    when(mockedOpenLibraryApiClient.fetchMetadataForBook(VALID_ISBN)).thenReturn(requestedBook);
    when(mockedBookRepository.save(ArgumentMatchers.any())).thenAnswer(invocation -> {
      Book methodArgument = invocation.getArgument(0, Book.class);
      methodArgument.setId(1L);
      return methodArgument;
    });

    cut.consumeBookUpdates(new BookSynchronization(VALID_ISBN));
    verify(mockedBookRepository, times(1)).save(requestedBook);
    verify(mockedBookRepository).save(bookArgumentCaptor.capture());
    assertEquals("Test Book", bookArgumentCaptor.getValue().getTitle());
    assertEquals(VALID_ISBN, bookArgumentCaptor.getValue().getIsbn());
  }
}
