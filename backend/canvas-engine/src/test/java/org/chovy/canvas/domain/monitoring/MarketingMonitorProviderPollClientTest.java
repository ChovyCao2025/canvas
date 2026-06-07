package org.chovy.canvas.domain.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketingMonitorProviderPollClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void supportsProviderSourceTypesCaseInsensitively() {
        MarketingMonitorProviderPollClient client = client(new FakeTransport("{}"), credentials());

        assertThat(client.supports("x_recent_search")).isTrue();
        assertThat(client.supports("YOUTUBE_SEARCH")).isTrue();
        assertThat(client.supports("google_business_reviews")).isTrue();
        assertThat(client.supports("TIKTOK_RESEARCH_VIDEO")).isTrue();
        assertThat(client.supports("BRANDWATCH")).isFalse();
    }

    @Test
    void rejectsMissingCredentialRefBeforeHttpCall() {
        FakeTransport transport = new FakeTransport("{}");
        MarketingMonitorProviderPollClient client = client(transport, credentials());

        assertThatThrownBy(() -> client.fetch(request(
                "X_RECENT_SEARCH",
                null,
                null,
                null,
                25,
                Map.of(
                        "query", "brand launch",
                        "credentials", Map.of("mode", "BEARER_ENV")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokenEnv");
        assertThat(transport.requests()).isEmpty();
    }

    @Test
    void fetchesXRecentSearchWithBearerEnvTokenAndMapsNextToken() {
        FakeTransport transport = new FakeTransport("""
                {
                  "data": [
                    {
                      "id": "184",
                      "text": "Brand launch is great",
                      "author_id": "42",
                      "created_at": "2026-06-06T01:02:03.000Z",
                      "lang": "en"
                    }
                  ],
                  "includes": {
                    "users": [
                      {"id": "42", "username": "alice"}
                    ]
                  },
                  "meta": {"next_token": "next-x", "result_count": 1}
                }
                """);
        MarketingMonitorProviderPollClient client = client(transport, credentials("X_TOKEN", "x-token"));

        MarketingMonitorPollResponse response = client.fetch(request(
                "X_RECENT_SEARCH",
                "cursor-before",
                LocalDateTime.of(2026, 6, 6, 0, 0),
                LocalDateTime.of(2026, 6, 6, 2, 0),
                25,
                Map.of(
                        "query", "brand launch",
                        "brandKey", "our-brand",
                        "credentials", Map.of("mode", "BEARER_ENV", "tokenEnv", "X_TOKEN"))));

        MarketingMonitorProviderHttpRequest httpRequest = transport.singleRequest();
        assertThat(httpRequest.method()).isEqualTo("GET");
        assertThat(httpRequest.uri().toString()).contains("https://api.x.com/2/tweets/search/recent");
        assertThat(httpRequest.uri().toString()).contains("query=brand+launch");
        assertThat(httpRequest.uri().toString()).contains("next_token=cursor-before");
        assertThat(httpRequest.uri().toString()).contains("max_results=25");
        assertThat(httpRequest.headers()).containsEntry("Authorization", "Bearer x-token");
        assertThat(response.nextCursor()).isEqualTo("next-x");
        assertThat(response.metadata()).containsEntry("provider", "X_RECENT_SEARCH");
        assertThat(response.metadata().toString()).doesNotContain("x-token");
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.externalItemId()).isEqualTo("x:184");
            assertThat(item.sourceUrl()).isEqualTo("https://x.com/i/web/status/184");
            assertThat(item.authorKey()).isEqualTo("alice");
            assertThat(item.brandKey()).isEqualTo("our-brand");
            assertThat(item.language()).isEqualTo("en");
            assertThat(item.text()).isEqualTo("Brand launch is great");
            assertThat(item.publishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 6, 1, 2, 3));
        });
    }

    @Test
    void fetchesXRecentSearchWithBearerCredentialRef() {
        FakeTransport transport = new FakeTransport("""
                {
                  "data": [
                    {
                      "id": "184",
                      "text": "Brand launch is great",
                      "author_id": "42",
                      "created_at": "2026-06-06T01:02:03.000Z"
                    }
                  ],
                  "meta": {"next_token": "next-x"}
                }
                """);
        MarketingMonitorProviderPollClient client = client(transport,
                tenantCredential("credential:x-prod:access_token", "db-token"));

        client.fetch(request(
                "X_RECENT_SEARCH",
                null,
                null,
                null,
                25,
                Map.of(
                        "query", "brand launch",
                        "credentials", Map.of("mode", "BEARER_REF", "credentialKey", " X-Prod "))));

        MarketingMonitorProviderHttpRequest httpRequest = transport.singleRequest();
        assertThat(httpRequest.headers()).containsEntry("Authorization", "Bearer db-token");
    }

    @Test
    void fetchesYouTubeSearchWithApiKeyEnvAndMapsVideoItems() {
        FakeTransport transport = new FakeTransport("""
                {
                  "nextPageToken": "yt-next",
                  "items": [
                    {
                      "id": {"videoId": "abc123"},
                      "snippet": {
                        "publishedAt": "2026-06-06T02:00:00Z",
                        "channelTitle": "Brand Channel",
                        "title": "Brand demo",
                        "description": "great launch"
                      }
                    }
                  ]
                }
                """);
        MarketingMonitorProviderPollClient client = client(transport, credentials("YT_KEY", "yt-key"));

        MarketingMonitorPollResponse response = client.fetch(request(
                "YOUTUBE_SEARCH",
                "page-1",
                LocalDateTime.of(2026, 6, 5, 0, 0),
                LocalDateTime.of(2026, 6, 6, 0, 0),
                50,
                Map.of(
                        "query", "brand demo",
                        "brandKey", "video-brand",
                        "regionCode", "US",
                        "credentials", Map.of("mode", "API_KEY_ENV", "apiKeyEnv", "YT_KEY"))));

        MarketingMonitorProviderHttpRequest httpRequest = transport.singleRequest();
        assertThat(httpRequest.method()).isEqualTo("GET");
        assertThat(httpRequest.uri().toString()).contains("https://youtube.googleapis.com/youtube/v3/search");
        assertThat(httpRequest.uri().toString()).contains("pageToken=page-1");
        assertThat(httpRequest.uri().toString()).contains("key=yt-key");
        assertThat(httpRequest.uri().toString()).contains("publishedAfter=2026-06-05T00%3A00%3A00Z");
        assertThat(response.nextCursor()).isEqualTo("yt-next");
        assertThat(response.metadata().toString()).doesNotContain("yt-key");
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.externalItemId()).isEqualTo("youtube:abc123");
            assertThat(item.authorKey()).isEqualTo("Brand Channel");
            assertThat(item.brandKey()).isEqualTo("video-brand");
            assertThat(item.text()).contains("Brand demo", "great launch");
            assertThat(item.publishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 6, 2, 0));
        });
    }

    @Test
    void fetchesYouTubeSearchWithApiKeyCredentialRef() {
        FakeTransport transport = new FakeTransport("""
                {
                  "items": [
                    {
                      "id": {"videoId": "abc123"},
                      "snippet": {"title": "Brand demo"}
                    }
                  ]
                }
                """);
        MarketingMonitorProviderPollClient client = client(transport,
                tenantCredential("credential:youtube-prod:api_key", "db-key"));

        client.fetch(request(
                "YOUTUBE_SEARCH",
                null,
                null,
                null,
                50,
                Map.of(
                        "query", "brand demo",
                        "credentials", Map.of("mode", "API_KEY_REF", "credentialKey", "youtube-prod"))));

        MarketingMonitorProviderHttpRequest httpRequest = transport.singleRequest();
        assertThat(httpRequest.uri().toString()).contains("key=db-key");
    }

    @Test
    void fetchesGoogleBusinessReviewsWithOAuthAndMapsReviews() {
        FakeTransport transport = new FakeTransport("""
                {
                  "reviews": [
                    {
                      "reviewId": "r1",
                      "comment": "bad service",
                      "starRating": "TWO",
                      "reviewer": {"displayName": "Dana"},
                      "updateTime": "2026-06-05T12:30:00Z"
                    }
                  ],
                  "nextPageToken": "gbp-next"
                }
                """);
        MarketingMonitorProviderPollClient client = client(transport, credentials("GBP_TOKEN", "gbp-token"));

        MarketingMonitorPollResponse response = client.fetch(request(
                "GOOGLE_BUSINESS_REVIEWS",
                "review-page",
                null,
                null,
                20,
                Map.of(
                        "accountId", "accounts/123",
                        "locationId", "locations/456",
                        "brandKey", "store-brand",
                        "credentials", Map.of("mode", "BEARER_ENV", "tokenEnv", "GBP_TOKEN"))));

        MarketingMonitorProviderHttpRequest httpRequest = transport.singleRequest();
        assertThat(httpRequest.method()).isEqualTo("GET");
        assertThat(httpRequest.uri().toString())
                .contains("https://mybusiness.googleapis.com/v4/accounts/123/locations/456/reviews");
        assertThat(httpRequest.uri().toString()).contains("pageToken=review-page");
        assertThat(httpRequest.headers()).containsEntry("Authorization", "Bearer gbp-token");
        assertThat(response.nextCursor()).isEqualTo("gbp-next");
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.externalItemId()).isEqualTo("google-review:r1");
            assertThat(item.authorKey()).isEqualTo("Dana");
            assertThat(item.brandKey()).isEqualTo("store-brand");
            assertThat(item.text()).isEqualTo("bad service");
            assertThat(item.rawPayload()).containsEntry("starRating", "TWO");
        });
    }

    @Test
    void fetchesTikTokResearchVideosWithStructuredCursorAndSearchId() {
        FakeTransport transport = new FakeTransport("""
                {
                  "data": {
                    "videos": [
                      {
                        "id": "v1",
                        "username": "creator",
                        "video_description": "Brand test",
                        "create_time": 1780711200,
                        "region_code": "US"
                      }
                    ],
                    "cursor": 20,
                    "has_more": true,
                    "search_id": "sid-2"
                  }
                }
                """);
        MarketingMonitorProviderPollClient client = client(transport, credentials("TT_TOKEN", "tt-token"));

        MarketingMonitorPollResponse response = client.fetch(request(
                "TIKTOK_RESEARCH_VIDEO",
                "{\"cursor\":10,\"searchId\":\"sid-1\"}",
                LocalDateTime.of(2026, 6, 5, 0, 0),
                LocalDateTime.of(2026, 6, 6, 0, 0),
                30,
                Map.of(
                        "query", "brand test",
                        "brandKey", "creator-brand",
                        "credentials", Map.of("mode", "BEARER_ENV", "tokenEnv", "TT_TOKEN"))));

        MarketingMonitorProviderHttpRequest httpRequest = transport.singleRequest();
        assertThat(httpRequest.method()).isEqualTo("POST");
        assertThat(httpRequest.uri().toString()).contains("https://open.tiktokapis.com/v2/research/video/query/");
        assertThat(httpRequest.headers()).containsEntry("Authorization", "Bearer tt-token");
        assertThat(httpRequest.body()).contains("\"max_count\":30", "\"cursor\":10", "\"search_id\":\"sid-1\"");
        assertThat(response.nextCursor()).contains("\"cursor\":20", "\"searchId\":\"sid-2\"");
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.externalItemId()).isEqualTo("tiktok:v1");
            assertThat(item.authorKey()).isEqualTo("creator");
            assertThat(item.brandKey()).isEqualTo("creator-brand");
            assertThat(item.text()).isEqualTo("Brand test");
            assertThat(item.rawPayload()).containsEntry("regionCode", "US");
        });
    }

    private MarketingMonitorProviderPollClient client(FakeTransport transport,
                                                      MarketingMonitorProviderCredentialResolver resolver) {
        return new MarketingMonitorProviderPollClient(transport, resolver, objectMapper);
    }

    private MarketingMonitorPollRequest request(String sourceType,
                                                String cursor,
                                                LocalDateTime from,
                                                LocalDateTime until,
                                                int maxItems,
                                                Map<String, Object> metadata) {
        return new MarketingMonitorPollRequest(
                7L,
                10L,
                "source-1",
                sourceType,
                cursor,
                from,
                until,
                maxItems,
                metadata);
    }

    private MarketingMonitorProviderCredentialResolver credentials(String... entries) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) {
            values.put(entries[i], entries[i + 1]);
        }
        return values::get;
    }

    private MarketingMonitorProviderCredentialResolver tenantCredential(String reference, String value) {
        return new MarketingMonitorProviderCredentialResolver() {
            @Override
            public String resolve(String ref) {
                return null;
            }

            @Override
            public String resolve(Long tenantId, String ref) {
                if (Long.valueOf(7L).equals(tenantId) && reference.equals(ref)) {
                    return value;
                }
                return null;
            }
        };
    }

    private static final class FakeTransport implements MarketingMonitorProviderHttpTransport {

        private final List<MarketingMonitorProviderHttpRequest> requests = new ArrayList<>();
        private final MarketingMonitorProviderHttpResponse response;

        private FakeTransport(String body) {
            this.response = new MarketingMonitorProviderHttpResponse(200, body, Map.of("x-request-id", "req-1"));
        }

        @Override
        public MarketingMonitorProviderHttpResponse execute(MarketingMonitorProviderHttpRequest request) {
            requests.add(request);
            return response;
        }

        private List<MarketingMonitorProviderHttpRequest> requests() {
            return requests;
        }

        private MarketingMonitorProviderHttpRequest singleRequest() {
            assertThat(requests).hasSize(1);
            return requests.get(0);
        }
    }
}
