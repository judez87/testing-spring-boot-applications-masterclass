package de.rieckpil.courses.book.review;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static de.rieckpil.courses.book.review.RandomReviewParameterResolverExtension.RandomReview;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(RandomReviewParameterResolverExtension.class)
class ReviewVerifierTest {

  private ReviewVerifier reviewVerifier;

  @BeforeEach
  void setup() {
    reviewVerifier = new ReviewVerifier();
  }

  @Test
  void shouldFailWhenReviewContainsSwearWord() {
    String review = "This book is shit, and I think the test should fail";

    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertFalse(result, "ReviewVerifier did not detect swear word");
  }

  @Test
  @DisplayName("Should fail when review contains 'lorem ipsum'")
  void testLoremIpsum() {
    String review = "This book is excelent, and Lorem ipsum dolor sit amet, consectetur adipiscing";

    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertFalse(result, "ReviewVerifier did not detect lorem ipsum");
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/badReview.csv")
  void shouldFailWhenReviewIsOfBadQuality(String review) {
    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertFalse(result, "ReviewVerifier didn't detect bad quality");
  }

  @RepeatedTest(5)
  void shouldFailWhenRandomReviewQualityIsBad(@RandomReview String review) {
    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertFalse(result, "ReviewVerifier didn't detect bad quality with random review: " + review);
  }

  @Test
  void shouldPassWhenReviewIsGood() {
    String review = "This book is excelent, and I think the test should pass!";

    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertTrue(result, "ReviewVerifier didn't approved the good quality review");
  }

  @Test
  void shouldPassWhenReviewIsGoodHamcrest() {
    String review = "This book is excelent, and I think the test should pass!";

    boolean result = reviewVerifier.doesMeetQualityStandards(review);

    MatcherAssert.assertThat(
      "ReviewVerifier didn't approved the good quality review",
      result,
      equalTo(true)
    );

  }

  @Test
  void shouldPassWhenReviewIsGoodAssertJ() {
    String review = "This book is excelent, and I think the test should pass!";

    boolean result = reviewVerifier.doesMeetQualityStandards(review);

    Assertions.assertThat(result)
      .withFailMessage("ReviewVerifier didn't approved the good quality review")
      .isTrue();
  }
}
