package de.rieckpil.courses.book.management;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.HttpServerErrorException;
import wiremock.org.hamcrest.Matchers;

import static org.junit.jupiter.api.Assertions.*;

@RestClientTest(OpenLibraryRestTemplateApiClient.class)
class OpenLibraryRestTemplateApiClientTest {

  @Autowired private OpenLibraryRestTemplateApiClient cut;

  @Autowired private MockRestServiceServer mockRestServiceServer;

  private static final String ISBN = "9780596004651";

  @Test
  void shouldInjectBeans() {
    assertNotNull(cut);
    assertNotNull(mockRestServiceServer);
  }

  @Test
  void shouldReturnBookWhenResultIsSuccess() {
    this.mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("/api/books?jscmd=data&format=json&bibkeys="+ISBN))
      .andRespond(MockRestResponseCreators.withSuccess(
        new ClassPathResource("/stubs/openlibrary/success-" + ISBN + ".json"),
        MediaType.APPLICATION_JSON)
      );

    Book book = cut.fetchMetadataForBook(ISBN);
    assertNotNull(book);

    assertEquals("9780596004651", book.getIsbn());
    assertEquals("Head first Java", book.getTitle());
    assertEquals("https://covers.openlibrary.org/b/id/388761-S.jpg", book.getThumbnailUrl());
    assertEquals("Kathy Sierra", book.getAuthor());
    assertEquals("Java (Computer program language)", book.getGenre());
    assertEquals("O'Reilly", book.getPublisher());
    assertEquals(619, book.getPages());

    assertNull(book.getId());
  }

  @Test
  void shouldReturnBookWhenResultIsSuccessButLackingAllInformation() {
    String response = """
      {
        "9780596004651": {
          "publishers": [
            {
            "name": "O'Reilly"
            }
          ],
          "title": "Head first Java",
          "authors": [
            {
              "url": "https://openlibrary.org/authors/OL18362A/Kathy_Sierra",
              "name": "Kathy Sierra"
            }
          ],
          "number_of_pages": 42,
          "cover": {
            "small": "https://covers.openlibrary.org/b/id/388761-S.jpg",
            "large": "https://covers.openlibrary.org/b/id/388761-L.jpg",
            "medium": "https://covers.openlibrary.org/b/id/388761-M.jpg"
          }
        }
      }
      """;

    this.mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("/api/books?jscmd=data&format=json&bibkeys="+ISBN))
      .andRespond(MockRestResponseCreators.withSuccess(
        response, MediaType.APPLICATION_JSON
      ));

    Book book = cut.fetchMetadataForBook(ISBN);
    assertNotNull(book);

    assertEquals("9780596004651", book.getIsbn());
    assertEquals("Head first Java", book.getTitle());
    assertEquals("https://covers.openlibrary.org/b/id/388761-S.jpg", book.getThumbnailUrl());
    assertEquals("Kathy Sierra", book.getAuthor());
    assertEquals("n.A", book.getGenre());
    assertEquals("n.A", book.getDescription());
    assertEquals("O'Reilly", book.getPublisher());
    assertEquals(42, book.getPages());

    assertNull(book.getId());

    // verify that all mock interactions were performed
    this.mockRestServiceServer.verify();
  }

  @Test
  void shouldPropagateExceptionWhenRemoteSystemIsDown() {

    assertThrows(HttpServerErrorException.class, () -> {
      this.mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("/api/books?jscmd=data&format=json&bibkeys="+ISBN))
        .andRespond(MockRestResponseCreators.withServerError());

      cut.fetchMetadataForBook(ISBN);
    });
  }

  @Test
  void shouldContainCorrectHeadersWhenRemoteSystemIsInvoked() {
    this.mockRestServiceServer
      .expect(MockRestRequestMatchers.requestTo("/api/books?jscmd=data&format=json&bibkeys="+ISBN))
      .andExpect(MockRestRequestMatchers.header("X-Custom-Auth", "Duke42"))
      .andExpect(MockRestRequestMatchers.header("X-Customer-Id", "42"))
      .andRespond(MockRestResponseCreators.withSuccess(
        new ClassPathResource("/stubs/openlibrary/success-" + ISBN + ".json"),
        MediaType.APPLICATION_JSON)
      );

    Book book = cut.fetchMetadataForBook(ISBN);
    assertNotNull(book);
  }
}
