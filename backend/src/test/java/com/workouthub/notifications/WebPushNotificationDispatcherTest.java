package com.workouthub.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import com.workouthub.push.domain.PushSubscription;
import com.workouthub.push.domain.PushSubscriptionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class WebPushNotificationDispatcherTest {

    @Test
    void deliversToEverySubscriptionAndPrunesGoneOnes() {
        UUID userId = UUID.randomUUID();
        PushSubscription alive = sub(userId, "https://push/alive", "aaaa", "bbbb");
        PushSubscription dead = sub(userId, "https://push/dead",  "cccc", "dddd");

        List<PushSubscription> storage = new ArrayList<>();
        storage.add(alive);
        storage.add(dead);
        PushSubscriptionRepository repo = new InMemoryRepo(storage);

        WebPushSender sender = (s, payload) ->
                s.getEndpoint().endsWith("/dead")
                        ? WebPushSender.Outcome.GONE
                        : WebPushSender.Outcome.OK;

        WebPushNotificationDispatcher dispatcher =
                new WebPushNotificationDispatcher(repo, sender, new ObjectMapper());

        dispatcher.send(userId, "hi", "body", "/dashboard");

        assertThat(storage).containsExactly(alive);
    }

    @Test
    void keepsSubscriptionOnTransientFailure() {
        UUID userId = UUID.randomUUID();
        PushSubscription sub = sub(userId, "https://push/flaky", "aaaa", "bbbb");
        List<PushSubscription> storage = new ArrayList<>(List.of(sub));
        PushSubscriptionRepository repo = new InMemoryRepo(storage);

        WebPushSender sender = (s, payload) -> WebPushSender.Outcome.TRANSIENT_FAILURE;

        WebPushNotificationDispatcher dispatcher =
                new WebPushNotificationDispatcher(repo, sender, new ObjectMapper());

        dispatcher.send(userId, "hi", "body", "/dashboard");

        assertThat(storage).containsExactly(sub);
    }

    @Test
    void serializesExpectedFieldsInPayload() {
        UUID userId = UUID.randomUUID();
        PushSubscription sub = sub(userId, "https://push/ok", "aaaa", "bbbb");
        PushSubscriptionRepository repo = new InMemoryRepo(new ArrayList<>(List.of(sub)));

        StringBuilder captured = new StringBuilder();
        WebPushSender sender = (s, payload) -> {
            captured.append(payload);
            return WebPushSender.Outcome.OK;
        };

        WebPushNotificationDispatcher dispatcher =
                new WebPushNotificationDispatcher(repo, sender, new ObjectMapper());

        dispatcher.send(userId, "Rest done", "Next set ready", "/session/1");

        assertThat(captured.toString()).contains("\"title\":\"Rest done\"");
        assertThat(captured.toString()).contains("\"body\":\"Next set ready\"");
        assertThat(captured.toString()).contains("\"url\":\"/session/1\"");
    }

    private static PushSubscription sub(UUID userId, String endpoint, String p256, String auth) {
        PushSubscription s = new PushSubscription();
        s.setId(UUID.randomUUID());
        s.setUserId(userId);
        s.setEndpoint(endpoint);
        s.setP256dhKey(p256);
        s.setAuthKey(auth);
        return s;
    }

    /**
     * Minimal in-memory implementation that covers the repo methods the
     * dispatcher actually calls. Unsupported methods throw on use so a
     * future refactor that starts relying on one will fail loudly.
     */
    private static final class InMemoryRepo implements PushSubscriptionRepository {

        private final List<PushSubscription> storage;

        InMemoryRepo(List<PushSubscription> storage) {
            this.storage = storage;
        }

        @Override
        public Optional<PushSubscription> findByEndpoint(String endpoint) {
            return storage.stream().filter(s -> s.getEndpoint().equals(endpoint)).findFirst();
        }

        @Override
        public List<PushSubscription> findByUserId(UUID userId) {
            return storage.stream().filter(s -> s.getUserId().equals(userId)).toList();
        }

        @Override
        public void deleteByEndpoint(String endpoint) {
            storage.removeIf(s -> s.getEndpoint().equals(endpoint));
        }

        @Override
        public void delete(PushSubscription entity) {
            storage.remove(entity);
        }

        // --- Unused JpaRepository methods below; throw to surface drift. ---

        @Override
        public <S extends PushSubscription> S save(S entity) { throw notImplemented(); }
        @Override
        public <S extends PushSubscription> List<S> saveAll(Iterable<S> entities) { throw notImplemented(); }
        @Override
        public Optional<PushSubscription> findById(UUID uuid) { throw notImplemented(); }
        @Override
        public boolean existsById(UUID uuid) { throw notImplemented(); }
        @Override
        public List<PushSubscription> findAll() { throw notImplemented(); }
        @Override
        public List<PushSubscription> findAllById(Iterable<UUID> uuids) { throw notImplemented(); }
        @Override
        public long count() { throw notImplemented(); }
        @Override
        public void deleteById(UUID uuid) { throw notImplemented(); }
        @Override
        public void deleteAll(Iterable<? extends PushSubscription> entities) { throw notImplemented(); }
        @Override
        public void deleteAll() { throw notImplemented(); }
        @Override
        public List<PushSubscription> findAll(Sort sort) { throw notImplemented(); }
        @Override
        public Page<PushSubscription> findAll(Pageable pageable) { throw notImplemented(); }
        @Override
        public void deleteAllById(Iterable<? extends UUID> uuids) { throw notImplemented(); }
        @Override
        public <S extends PushSubscription> S saveAndFlush(S entity) { throw notImplemented(); }
        @Override
        public <S extends PushSubscription> List<S> saveAllAndFlush(Iterable<S> entities) { throw notImplemented(); }
        @Override
        public void deleteAllInBatch(Iterable<PushSubscription> entities) { throw notImplemented(); }
        @Override
        public void deleteAllByIdInBatch(Iterable<UUID> uuids) { throw notImplemented(); }
        @Override
        public void deleteAllInBatch() { throw notImplemented(); }
        @Override
        public PushSubscription getOne(UUID uuid) { throw notImplemented(); }
        @Override
        public PushSubscription getById(UUID uuid) { throw notImplemented(); }
        @Override
        public PushSubscription getReferenceById(UUID uuid) { throw notImplemented(); }
        @Override
        public <S extends PushSubscription> Optional<S> findOne(Example<S> example) { throw notImplemented(); }
        @Override
        public <S extends PushSubscription> List<S> findAll(Example<S> example) { throw notImplemented(); }
        @Override
        public <S extends PushSubscription> List<S> findAll(Example<S> example, Sort sort) { throw notImplemented(); }
        @Override
        public <S extends PushSubscription> Page<S> findAll(Example<S> example, Pageable pageable) { throw notImplemented(); }
        @Override
        public <S extends PushSubscription> long count(Example<S> example) { throw notImplemented(); }
        @Override
        public <S extends PushSubscription> boolean exists(Example<S> example) { throw notImplemented(); }
        @Override
        public void flush() { throw notImplemented(); }
        @Override
        public <S extends PushSubscription, R> R findBy(
                Example<S> example, Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
            throw notImplemented();
        }

        private static UnsupportedOperationException notImplemented() {
            return new UnsupportedOperationException("not needed by dispatcher");
        }
    }
}
