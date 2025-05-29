package de.rieckpil.courses.book.review;

import de.rieckpil.courses.book.management.Book;
import de.rieckpil.courses.book.management.BookRepository;
import de.rieckpil.courses.book.management.User;
import de.rieckpil.courses.book.management.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock private ReviewVerifier mockedReviewVerifier;

  @Mock private UserService userService;

  @Mock private BookRepository bookRepository;

  @Mock private ReviewRepository reviewRepository;

  @InjectMocks private ReviewService cut;

  private static final String EMAIL = "duke@spring.io";
  private static final String USERNAME = "duke";
  private static final String ISBN = "42";

  @Test
  void shouldNotBeNull() {
    assertNotNull(mockedReviewVerifier);
    assertNotNull(userService);
    assertNotNull(bookRepository);
    assertNotNull(reviewRepository);
    assertNotNull(cut);
  }

  @Test
  @DisplayName("Write english sentence")
  void shouldThrowExceptionWhenReviewedBookIsNotExisting() {
    when(bookRepository.findByIsbn(ISBN)).thenReturn(null);

    assertThrows(IllegalArgumentException.class,
      () -> cut.createBookReview(ISBN, null, USERNAME, EMAIL)
    );
  }

  @Test
  void shouldRejectReviewWhenReviewQualityIsBad() {
    BookReviewRequest request = new BookReviewRequest("Some Title", "Bad Content!", 1);

    when(bookRepository.findByIsbn(ISBN)).thenReturn(new Book());
    when(mockedReviewVerifier.doesMeetQualityStandards(request.getReviewContent())).thenReturn(false);

    assertThrows(BadReviewQualityException.class,
      () -> cut.createBookReview(ISBN, request, USERNAME, EMAIL));
    verify(reviewRepository, never()).save(any());
  }

  @Test
  void shouldStoreReviewWhenReviewQualityIsGoodAndBookIsPresent() {
    BookReviewRequest request = new BookReviewRequest("Some Title", "Good Content!", 5);

    when(bookRepository.findByIsbn(ISBN)).thenReturn(new Book());
    when(mockedReviewVerifier.doesMeetQualityStandards(request.getReviewContent())).thenReturn(true);
    when(userService.getOrCreateUser(USERNAME, EMAIL)).thenReturn(new User());
    when(reviewRepository.save(any())).thenAnswer(invocation -> {
      Review argument = invocation.getArgument(0, Review.class);
      argument.setId(42L);
      return argument;
    });

    Long bookId = cut.createBookReview(ISBN, request, USERNAME, EMAIL);
    assertEquals(42L, bookId);
    verify(reviewRepository, times(1)).save(any());

  }
}
