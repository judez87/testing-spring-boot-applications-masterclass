package de.rieckpil.courses.book.management;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class OpenLibraryApiClientTest {

  private MockWebServer mockWebServer;
  private OpenLibraryApiClient cut;

  private static final String ISBN = "9780596004651";

  private static String VALID_RESPONSE;

  static {
    try {
      VALID_RESPONSE = new String(OpenLibraryApiClientTest.class
        .getClassLoader()
        .getResourceAsStream("stubs/openlibrary/success-" + ISBN + ".json")
        .readAllBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @BeforeEach
  public void setUp() throws IOException {

    HttpClient httpClient =
      HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2_000)
        .doOnConnected(
          connection ->
            connection
              .addHandlerLast(new ReadTimeoutHandler(2))
              .addHandlerLast(new WriteTimeoutHandler(2)));

    this.mockWebServer = new MockWebServer();
    this.mockWebServer.start();

    this.cut = new OpenLibraryApiClient(
      WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .baseUrl(mockWebServer.url("/").toString())
        .build()
    );
  }

  @Test
  void notNull() {
    assertNotNull(cut);
    assertNotNull(mockWebServer);
  }

  @Test
  void shouldReturnBookWhenResultIsSuccess() throws InterruptedException {
    MockResponse mockResponse = new MockResponse()
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setBody(VALID_RESPONSE);

    mockWebServer.enqueue(mockResponse);

    Book book = cut.fetchMetadataForBook(ISBN);
    assertNotNull(book);

    assertEquals("9780596004651", book.getIsbn());
    assertEquals("Head first Java", book.getTitle());
    assertEquals("https://covers.openlibrary.org/b/id/388761-S.jpg", book.getThumbnailUrl());
    assertEquals("Kathy Sierra", book.getAuthor());
    assertEquals("Java (Computer program language)", book.getGenre());
    assertEquals("O'Reilly", book.getPublisher());
    assertEquals(619, book.getPages());

    RecordedRequest recordedRequest = this.mockWebServer.takeRequest();
    assertEquals("/api/books?jscmd=data&format=json&bibkeys=" + ISBN, recordedRequest.getPath());
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

    mockWebServer.enqueue(new MockResponse()
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setResponseCode(200)
      .setBody(response));

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
  }

  @Test
  void shouldPropagateExceptionWhenRemoteSystemIsDown() {
    assertThrows(RuntimeException.class, () -> {

      mockWebServer.enqueue(new MockResponse()
        .setResponseCode(500)
        .setBody("System is down :("));

      cut.fetchMetadataForBook(ISBN);
    });
  }

  @Test
  void shouldRetryWhenRemoteSystemIsSlowOrFailing() {

    mockWebServer.enqueue(new MockResponse()
      .setResponseCode(500)
      .setBody("System is down :("));

    mockWebServer.enqueue(new MockResponse()
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setResponseCode(200)
      .setBody(VALID_RESPONSE)
      .setBodyDelay(2, TimeUnit.SECONDS));

    mockWebServer.enqueue(new MockResponse()
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setResponseCode(200)
      .setBody(VALID_RESPONSE));

    Book book = cut.fetchMetadataForBook(ISBN);
    assertNotNull(book);
    assertEquals("9780596004651", book.getIsbn());
    assertNull(book.getId());
  }
}
